# =====================================================================
#  야외활동 지수 - Windows 설치 스크립트
#   -Mode war        : 이미 설치된 Tomcat 11 에 outdoor.war 배포 (1-tomcat-war\INSTALL.bat)
#   -Mode standalone : Tomcat 없이 단독 실행 설정 (2-standalone\SETUP.bat)
#  Windows PowerShell 5.1 기준으로 작성
# =====================================================================
param(
    [ValidateSet('war', 'standalone')]
    [string]$Mode = 'war',
    [string]$TomcatDir = ''
)

$ErrorActionPreference = 'Stop'
$PkgRoot = Split-Path -Parent $PSScriptRoot          # 압축을 푼 최상위 폴더
$LibDir  = Join-Path $PkgRoot '2-standalone\app\target\lib'
$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Say([string]$m)  { Write-Host $m }
function Step([string]$m) { Write-Host ''; Write-Host "== $m ==" -ForegroundColor Cyan }
function Ok([string]$m)   { Write-Host "  [OK] $m" -ForegroundColor Green }
function Warn([string]$m) { Write-Host "  [!] $m" -ForegroundColor Yellow }
function Quit([string]$m) {
    Write-Host ''
    Write-Host "  [X] $m" -ForegroundColor Red
    Write-Host ''
    Read-Host '엔터를 누르면 창이 닫힙니다' | Out-Null
    exit 1
}
function Ask([string]$q, [string]$def) {
    $a = Read-Host "$q (엔터 = $def)"
    if ([string]::IsNullOrWhiteSpace($a)) { return $def }
    return $a.Trim().Trim('"')
}
function Test-Writable([string]$dir) {
    try {
        $t = Join-Path $dir ('.write-test-' + [guid]::NewGuid().ToString('N'))
        [System.IO.File]::WriteAllText($t, 'x')
        Remove-Item -LiteralPath $t -Force
        return $true
    } catch { return $false }
}
function Test-PortOpen([string]$h, [int]$p) {
    $c = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $c.BeginConnect($h, $p, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne(1500, $false)) { return $false }
        $c.EndConnect($iar)
        return $true
    } catch { return $false } finally { $c.Close() }
}
function Run-Java([string]$exe, [string[]]$javaArgs) {
    # java 의 표준오류(-version 출력 등)까지 문자열로 받기. PowerShell 5.1 에서 오류로 멈추지 않게 처리
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $out = & $exe @javaArgs 2>&1 | ForEach-Object { "$_" } }
    finally { $ErrorActionPreference = $old }
    return ($out -join "`n")
}

Write-Host ''
Write-Host '=====================================================' -ForegroundColor Cyan
if ($Mode -eq 'war') {
    Write-Host '  야외활동 지수 - 설치된 Tomcat 11 에 배포' -ForegroundColor Cyan
} else {
    Write-Host '  야외활동 지수 - 단독 실행 설정 (Tomcat 없이)' -ForegroundColor Cyan
}
Write-Host '=====================================================' -ForegroundColor Cyan

# 압축을 풀지 않고 실행한 경우
if ($PkgRoot -like '*\Temp\*' -and $PkgRoot -like '*.zip*') {
    Quit '압축 파일 안에서 바로 실행했어요. 먼저 [압축 풀기] 후 풀린 폴더에서 다시 실행해 주세요.'
}

# ---------------------------------------------------------------------
# 1. Java 확인
# ---------------------------------------------------------------------
Step '1. Java 확인'
$javaExe = $null
$cmdJava = Get-Command java.exe -ErrorAction SilentlyContinue
if ($cmdJava) { $javaExe = $cmdJava.Source }
elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe' }
if (-not $javaExe) {
    Quit 'Java 를 찾지 못했어요. Java 17 이상을 설치하고 PATH 또는 JAVA_HOME 을 설정한 뒤 다시 실행해 주세요.'
}
$props = Run-Java $javaExe @('-XshowSettings:properties', '-version')
$javaHome = $null
$mHome = [regex]::Match($props, 'java\.home = (.+)')
if ($mHome.Success) { $javaHome = $mHome.Groups[1].Value.Trim() }
$javaMajor = 0
$mVer = [regex]::Match($props, 'java\.specification\.version = (\d+)(\.(\d+))?')
if ($mVer.Success) {
    $javaMajor = [int]$mVer.Groups[1].Value
    if ($javaMajor -eq 1 -and $mVer.Groups[3].Success) { $javaMajor = [int]$mVer.Groups[3].Value }
}
if ($javaMajor -lt 17) {
    Quit "Java $javaMajor 버전이에요. Tomcat 11 과 이 프로그램은 Java 17 이상이 필요해요."
}
Ok "Java $javaMajor  ($javaHome)"
if ($javaHome) { $javaExe = Join-Path $javaHome 'bin\java.exe' }

# ---------------------------------------------------------------------
# 2. (war) Tomcat 폴더 찾기 + 관리자 권한
# ---------------------------------------------------------------------
$tomcat = $null
$serviceName = $null
if ($Mode -eq 'war') {
    Step '2. Tomcat 11 폴더 찾기'

    function Test-Tomcat([string]$p) {
        if ([string]::IsNullOrWhiteSpace($p)) { return $false }
        return (Test-Path (Join-Path $p 'conf\server.xml')) -and (Test-Path (Join-Path $p 'webapps'))
    }
    function Get-TomcatMajor([string]$p) {
        $rn = Join-Path $p 'RELEASE-NOTES'
        if (Test-Path $rn) {
            $m = [regex]::Match((Get-Content -LiteralPath $rn -Raw), 'Apache Tomcat Version (\d+)')
            if ($m.Success) { return [int]$m.Groups[1].Value }
        }
        $m2 = [regex]::Match($p, 'Tomcat[ \-_]?(\d+)', 'IgnoreCase')
        if ($m2.Success) { return [int]$m2.Groups[1].Value }
        return 0
    }

    $cands = New-Object System.Collections.ArrayList
    if ($TomcatDir) { [void]$cands.Add($TomcatDir) }
    if ($env:CATALINA_BASE) { [void]$cands.Add($env:CATALINA_BASE) }
    if ($env:CATALINA_HOME) { [void]$cands.Add($env:CATALINA_HOME) }
    try {
        Get-CimInstance Win32_Service -ErrorAction Stop | Where-Object { $_.Name -like 'Tomcat*' } | ForEach-Object {
            $m = [regex]::Match([string]$_.PathName, '^"?(.+?)\\bin\\', 'IgnoreCase')
            if ($m.Success) { [void]$cands.Add($m.Groups[1].Value) }
        }
    } catch { }
    $searchRoots = @(
        'C:\Program Files\Apache Software Foundation',
        'C:\Program Files (x86)\Apache Software Foundation',
        'C:\', 'D:\',
        (Join-Path $env:USERPROFILE 'Downloads'),
        (Join-Path $env:USERPROFILE 'Desktop'),
        $env:USERPROFILE
    )
    foreach ($r in $searchRoots) {
        if ($r -and (Test-Path $r)) {
            Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -like '*tomcat*' } |
                ForEach-Object {
                    [void]$cands.Add($_.FullName)
                    # apache-tomcat-11.0.x 안에 한 번 더 폴더가 있는 경우
                    Get-ChildItem -LiteralPath $_.FullName -Directory -ErrorAction SilentlyContinue |
                        Where-Object { $_.Name -like '*tomcat*' } | ForEach-Object { [void]$cands.Add($_.FullName) }
                }
        }
    }
    $found = @()
    foreach ($c in $cands) {
        $full = $null
        try { $full = (Resolve-Path -LiteralPath $c -ErrorAction Stop).Path.TrimEnd('\') } catch { continue }
        if ((Test-Tomcat $full) -and ($found -notcontains $full)) { $found += $full }
    }
    $found11 = @($found | Where-Object { (Get-TomcatMajor $_) -eq 11 })
    if ($found11.Count -gt 0) { $found = $found11 }

    if ($found.Count -eq 1) {
        $tomcat = $found[0]
    } elseif ($found.Count -gt 1) {
        Say '  Tomcat 폴더가 여러 개 있어요. 사용할 번호를 고르세요.'
        for ($i = 0; $i -lt $found.Count; $i++) { Say ("   {0}) {1}" -f ($i + 1), $found[$i]) }
        $n = Ask '  번호' '1'
        $idx = 0
        if ([int]::TryParse($n, [ref]$idx) -and $idx -ge 1 -and $idx -le $found.Count) { $tomcat = $found[$idx - 1] } else { $tomcat = $found[0] }
    }
    while (-not $tomcat) {
        Warn 'Tomcat 폴더를 자동으로 찾지 못했어요.'
        Say  '  Tomcat 폴더 경로를 붙여 넣어 주세요. (bin, conf, webapps 폴더가 들어 있는 폴더)'
        Say  '  예) C:\Program Files\Apache Software Foundation\Tomcat 11.0'
        $p = (Read-Host '  경로').Trim().Trim('"').TrimEnd('\')
        if (Test-Tomcat $p) { $tomcat = $p } else { Warn "그 폴더에는 conf\server.xml 또는 webapps 가 없어요: $p" }
    }
    $major = Get-TomcatMajor $tomcat
    Ok "Tomcat 폴더: $tomcat"
    if ($major -ne 0 -and $major -ne 11) { Warn "Tomcat $major 버전으로 보여요. 이 프로그램은 Tomcat 11 (jakarta) 기준이에요." }

    # 관리자 권한이 필요한 위치(Program Files 등)면 권한을 올려서 다시 실행
    if (-not (Test-Writable (Join-Path $tomcat 'conf')) -or -not (Test-Writable (Join-Path $tomcat 'webapps'))) {
        $isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
        if ($isAdmin) { Quit "관리자 권한으로도 $tomcat 폴더에 쓸 수 없어요. 폴더 권한을 확인해 주세요." }
        Warn 'Tomcat 폴더에 파일을 쓰려면 관리자 권한이 필요해요.'
        Say  '  잠시 후 뜨는 [사용자 계정 컨트롤] 창에서 [예]를 눌러 주세요. 새 창에서 설치가 이어집니다.'
        Start-Sleep -Seconds 2
        $argList = "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Mode war -TomcatDir `"$tomcat`""
        try { Start-Process -FilePath 'powershell.exe' -ArgumentList $argList -Verb RunAs | Out-Null }
        catch { Quit '관리자 권한 요청이 취소됐어요. INSTALL.bat 을 마우스 오른쪽 버튼 → [관리자 권한으로 실행] 해 주세요.' }
        exit 0
    }

    # 서비스로 설치된 Tomcat 인지 확인
    try {
        $svc = Get-CimInstance Win32_Service -ErrorAction Stop | Where-Object {
            $_.Name -like 'Tomcat*' -and ([string]$_.PathName) -like ('*' + $tomcat + '\bin\*')
        } | Select-Object -First 1
        if ($svc) { $serviceName = $svc.Name; Ok "Windows 서비스로 설치된 Tomcat: $serviceName" }
    } catch { }
}

# ---------------------------------------------------------------------
# 3. 공공데이터 서비스키
# ---------------------------------------------------------------------
Step '3. 공공데이터포털 서비스키'
$defaults = @{}
$defFile = Join-Path $PSScriptRoot 'defaults.properties'
if (Test-Path $defFile) {
    foreach ($line in [System.IO.File]::ReadAllLines($defFile, [System.Text.Encoding]::UTF8)) {
        $m = [regex]::Match($line, '^\s*([A-Z_]+)\s*=(.*)$')
        if ($m.Success) { $defaults[$m.Groups[1].Value] = $m.Groups[2].Value.Trim() }
    }
}
$serviceKey = $defaults['OUTDOOR_SERVICE_KEY']
if ([string]::IsNullOrWhiteSpace($serviceKey)) {
    $serviceKey = (Read-Host '  서비스키(Decoding 키)를 붙여 넣어 주세요').Trim()
} else {
    $useDefault = Ask '  팀 공용 서비스키를 그대로 쓸까요? (Y/n)' 'Y'
    if ($useDefault -match '^[nN]') { $serviceKey = (Read-Host '  새 서비스키(Decoding 키)').Trim() }
}
if ([string]::IsNullOrWhiteSpace($serviceKey)) { Quit '서비스키가 비어 있어요.' }
Ok ('서비스키 준비됨 (' + $serviceKey.Substring(0, [Math]::Min(6, $serviceKey.Length)) + '...)')

# ---------------------------------------------------------------------
# 4. MySQL
# ---------------------------------------------------------------------
Step '4. MySQL (즐겨찾기 · 검색 기록 저장)'
$dbLines = @()
$useMysql = Ask '  MySQL 을 사용할까요? n 을 누르면 내장 DB(H2) 사용 (Y/n)' 'Y'
if ($useMysql -notmatch '^[nN]') {
    $dbHost = Ask '  MySQL 주소' 'localhost'
    $dbPort = Ask '  MySQL 포트' '3306'
    $dbUser = Ask '  MySQL 사용자' 'root'
    $connected = $false
    for ($try = 1; $try -le 3 -and -not $connected; $try++) {
        $sec = Read-Host '  MySQL 비밀번호' -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
        $dbPass = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

        if (-not (Test-PortOpen $dbHost ([int]$dbPort))) {
            Warn "$dbHost`:$dbPort 에 MySQL 이 응답하지 않아요."
            Say  '     → [서비스] 앱(services.msc)에서 MySQL80 이 [실행 중]인지 확인해 주세요.'
            $again = Ask '  MySQL 을 켠 뒤 다시 확인할까요? n 이면 내장 DB(H2) 사용 (Y/n)' 'Y'
            if ($again -match '^[nN]') { break } else { $try--; continue }
        }

        # 비밀번호는 명령줄이 아니라 환경변수로 넘긴다 (특수문자 안전)
        $cp = (Join-Path $PSScriptRoot 'dbcheck') + ';' + (Join-Path $LibDir 'mysql-connector-j-9.3.0.jar') + ';' + (Join-Path $LibDir 'protobuf-java-4.29.0.jar')
        $env:OUTDOOR_DB_PASSWORD = $dbPass
        $res = (Run-Java $javaExe @('-cp', $cp, 'DbCheck', $dbHost, $dbPort, $dbUser)).Trim()
        Remove-Item Env:\OUTDOOR_DB_PASSWORD -ErrorAction SilentlyContinue
        $last = ($res -split "`r?`n" | Where-Object { $_ -match '^(OK|AUTH|CONN|OTHER)' } | Select-Object -Last 1)
        if ($last -eq 'OK') {
            $connected = $true
            Ok "MySQL 연결 성공 (outdoor_index 데이터베이스 준비됨)"
        } elseif ($last -eq 'AUTH') {
            Warn "사용자 또는 비밀번호가 틀렸어요. ($try/3)"
        } elseif ($last -eq 'CONN') {
            Warn 'MySQL 에 연결할 수 없어요. MySQL 서비스가 켜져 있는지 확인해 주세요.'
        } else {
            Warn "MySQL 확인 중 문제: $res"
        }
    }
    if ($connected) {
        $dbLines = @(
            "OUTDOOR_DB_URL=jdbc:mysql://$dbHost`:$dbPort/outdoor_index",
            "OUTDOOR_DB_USER=$dbUser",
            "OUTDOOR_DB_PASSWORD=$dbPass"
        )
    } else {
        Warn 'MySQL 대신 내장 DB(H2)를 사용합니다. 나중에 설정 파일만 고치면 MySQL 로 바꿀 수 있어요.'
    }
} else {
    Ok '내장 DB(H2) 사용'
}

$demoMode = $defaults['OUTDOOR_DEMO_MODE']; if (-not $demoMode) { $demoMode = 'auto' }
$cacheTtl = $defaults['OUTDOOR_CACHE_TTL_SECONDS']; if (-not $cacheTtl) { $cacheTtl = '600' }

# ---------------------------------------------------------------------
# 5. 설정 파일 쓰기 + 시연용 저장 데이터 복사
# ---------------------------------------------------------------------
Step '5. 설정 저장'
$header = @(
    '# 야외활동 지수 설정 파일 (설치 스크립트가 만듦)',
    '# 값을 바꾼 뒤에는 서버(Tomcat)를 다시 시작해야 반영됩니다.',
    ("# 만든 시각: " + (Get-Date -Format 'yyyy-MM-dd HH:mm'))
)
$body = @(
    "OUTDOOR_SERVICE_KEY=$serviceKey",
    "OUTDOOR_DEMO_MODE=$demoMode",
    "OUTDOOR_CACHE_TTL_SECONDS=$cacheTtl"
) + $dbLines

if ($Mode -eq 'war') {
    $confFile = Join-Path $tomcat 'conf\outdoor-index.properties'
    $dataDir  = Join-Path $tomcat 'outdoor-data'
    [System.IO.File]::WriteAllLines($confFile, [string[]]($header + $body), $Utf8NoBom)
    Ok "설정 파일: $confFile"
} else {
    $appDir = Join-Path $PkgRoot '2-standalone\app'
    $port = Ask '  단독 실행 서버 포트' '8090'
    if (Test-PortOpen '127.0.0.1' ([int]$port)) {
        Warn "$port 번 포트를 다른 프로그램이 쓰고 있어요. 다른 번호(예: 8091)를 쓰는 게 좋아요."
        $port = Ask '  포트' ([string]([int]$port + 1))
    }
    $confFile = Join-Path $appDir '.env'
    $dataDir  = Join-Path $appDir 'data'
    [System.IO.File]::WriteAllLines($confFile, [string[]]($header + @("OUTDOOR_PORT=$port") + $body), $Utf8NoBom)
    Ok "설정 파일: $confFile"
}

# 인터넷이 안 될 때 보여줄 저장 데이터 (이미 있는 파일은 덮어쓰지 않음)
$savedSrc = Join-Path $PkgRoot 'data\api-saved'
$savedDst = Join-Path $dataDir 'api-saved'
if (Test-Path $savedSrc) {
    New-Item -ItemType Directory -Force -Path $savedDst | Out-Null
    $copied = 0
    Get-ChildItem -LiteralPath $savedSrc -File | ForEach-Object {
        $t = Join-Path $savedDst $_.Name
        if (-not (Test-Path $t)) { Copy-Item -LiteralPath $_.FullName -Destination $t; $copied++ }
    }
    Ok "시연용 저장 데이터 $copied 개 복사 → $savedDst"
}

# ---------------------------------------------------------------------
# 6. 배포 / 실행
# ---------------------------------------------------------------------
if ($Mode -eq 'standalone') {
    Step '완료'
    Ok '단독 실행 설정이 끝났어요.'
    Say "  → 2-standalone\START.bat 을 더블클릭하면 서버가 켜지고 http://localhost:$port/ 이 열립니다."
    $start = Ask '  지금 바로 서버를 켤까요? (Y/n)' 'Y'
    if ($start -notmatch '^[nN]') {
        Start-Process -FilePath (Join-Path $PkgRoot '2-standalone\START.bat') -WorkingDirectory (Join-Path $PkgRoot '2-standalone')
    }
    Read-Host '엔터를 누르면 이 창이 닫힙니다' | Out-Null
    exit 0
}

Step '6. Tomcat 에 배포'
# HTTP 포트 (server.xml)
$httpPort = 8080
$serverXml = Get-Content -LiteralPath (Join-Path $tomcat 'conf\server.xml') -Raw
$serverXml = [regex]::Replace($serverXml, '<!--[\s\S]*?-->', '')
$mPort = [regex]::Match($serverXml, '<Connector[^>]*?port="(\d+)"[^>]*?protocol="HTTP/1\.1"')
if (-not $mPort.Success) { $mPort = [regex]::Match($serverXml, '<Connector[^>]*?protocol="HTTP/1\.1"[^>]*?port="(\d+)"') }
if (-not $mPort.Success) { $mPort = [regex]::Match($serverXml, '<Connector[^>]*?port="(\d+)"') }
if ($mPort.Success) { $httpPort = [int]$mPort.Groups[1].Value }
Ok "Tomcat HTTP 포트: $httpPort"

$wasRunning = Test-PortOpen '127.0.0.1' $httpPort
if ($serviceName) {
    $s = Get-Service -Name $serviceName
    if ($s.Status -eq 'Running') {
        Say "  서비스 $serviceName 를 잠시 멈춥니다..."
        try { Stop-Service -Name $serviceName -Force -ErrorAction Stop; Start-Sleep -Seconds 3 }
        catch { Warn "서비스를 멈추지 못했어요 (관리자 권한 필요). 켜진 상태로 배포를 계속합니다."; $serviceName = $null; $wasRunning = $true }
    }
}

$webapps = Join-Path $tomcat 'webapps'
$oldDir = Join-Path $webapps 'outdoor'
$oldWar = Join-Path $webapps 'outdoor.war'
if (Test-Path $oldDir) {
    try { Remove-Item -LiteralPath $oldDir -Recurse -Force } catch { Warn "이전 배포 폴더를 지우지 못했어요: $oldDir (Tomcat 을 끄고 다시 실행해 주세요)" }
}
Copy-Item -LiteralPath (Join-Path $PkgRoot '1-tomcat-war\outdoor.war') -Destination $oldWar -Force
Ok "outdoor.war 복사 → $oldWar"

# 이전에 컴파일된 JSP 캐시 정리
$workDir = Join-Path $tomcat 'work\Catalina\localhost\outdoor'
if (Test-Path $workDir) { Remove-Item -LiteralPath $workDir -Recurse -Force -ErrorAction SilentlyContinue }

if ($serviceName) {
    Say "  서비스 $serviceName 시작..."
    try { Start-Service -Name $serviceName -ErrorAction Stop }
    catch { Warn "서비스를 시작하지 못했어요. [서비스] 앱에서 $serviceName 을 직접 [시작] 해 주세요." }
} elseif ($wasRunning) {
    Say '  Tomcat 이 이미 켜져 있어서 자동으로 배포됩니다. (10~30초)'
    Warn '설정을 바꾼 경우에는 Tomcat 을 껐다 켜야 반영돼요. (bin\shutdown.bat → bin\startup.bat)'
} else {
    Say '  Tomcat 을 시작합니다. (새 검은 창이 뜨면 닫지 마세요)'
    $env:JAVA_HOME = $javaHome
    $env:CATALINA_HOME = $tomcat
    Start-Process -FilePath (Join-Path $tomcat 'bin\startup.bat') -WorkingDirectory (Join-Path $tomcat 'bin')
}

# 뜰 때까지 기다리기
$url = "http://localhost:$httpPort/outdoor/search"
Say "  $url 응답을 기다리는 중... (최대 90초)"
$okUp = $false
for ($i = 0; $i -lt 45; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -eq 200) { $okUp = $true; break }
    } catch { }
}

Step '완료'
if ($okUp) {
    Ok "서버가 정상 응답해요 → $url"
    Start-Process "http://localhost:$httpPort/outdoor/"
} else {
    Warn '아직 응답이 없어요. Tomcat 이 켜졌는지 확인하고, 1~2분 뒤 아래 주소를 열어 보세요.'
    Say  "     http://localhost:$httpPort/outdoor/"
    Say  '     계속 안 되면 CHECK.bat 을 실행해 결과(check-result.txt)를 확인해 주세요.'
}
Read-Host '엔터를 누르면 이 창이 닫힙니다' | Out-Null
