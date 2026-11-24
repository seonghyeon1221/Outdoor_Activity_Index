# =====================================================================
#  야외활동 지수 - 상태 점검 (CHECK.bat)
#  결과를 화면에 보여주고 check-result.txt 로도 저장합니다.
#  (서비스키와 비밀번호는 가려서 저장)
# =====================================================================
$ErrorActionPreference = 'Continue'
$PkgRoot = Split-Path -Parent $PSScriptRoot
$report = New-Object System.Collections.ArrayList
function Out-Line([string]$m, [string]$color = 'Gray') { Write-Host $m -ForegroundColor $color; [void]$report.Add($m) }
function Test-PortOpen([string]$h, [int]$p) {
    $c = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $c.BeginConnect($h, $p, $null, $null)
        if (-not $iar.AsyncWaitHandle.WaitOne(1500, $false)) { return $false }
        $c.EndConnect($iar); return $true
    } catch { return $false } finally { $c.Close() }
}
function Mask-Line([string]$line) {
    if ($line -match '^(OUTDOOR_SERVICE_KEY|OUTDOOR_DB_PASSWORD)=(.*)$') {
        $v = $matches[2]
        if ($v.Length -gt 4) { return $matches[1] + '=' + $v.Substring(0, 4) + '****' } else { return $matches[1] + '=****' }
    }
    return $line
}
function Http-Status([string]$url) {
    try { return (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 8).StatusCode }
    catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return '응답 없음'
    }
}

Out-Line '===== 야외활동 지수 상태 점검 =====' 'Cyan'
Out-Line ('시각: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Out-Line ('Windows: ' + [Environment]::OSVersion.VersionString)

# Java
Out-Line ''
Out-Line '[Java]' 'Cyan'
$java = Get-Command java.exe -ErrorAction SilentlyContinue
if ($java) {
    Out-Line ('java 위치: ' + $java.Source)
    $v = & $java.Source -version 2>&1 | ForEach-Object { "$_" }
    $v | ForEach-Object { Out-Line ('  ' + $_) }
} else { Out-Line 'java: PATH 에서 찾지 못함' 'Yellow' }
Out-Line ('JAVA_HOME: ' + $env:JAVA_HOME)

# Tomcat
Out-Line ''
Out-Line '[Tomcat]' 'Cyan'
$tomcats = @()
if ($env:CATALINA_HOME) { $tomcats += $env:CATALINA_HOME }
try {
    Get-CimInstance Win32_Service | Where-Object { $_.Name -like 'Tomcat*' } | ForEach-Object {
        Out-Line ("서비스: {0}  상태={1}  시작유형={2}" -f $_.Name, $_.State, $_.StartMode)
        $m = [regex]::Match([string]$_.PathName, '^"?(.+?)\\bin\\', 'IgnoreCase')
        if ($m.Success) { $tomcats += $m.Groups[1].Value }
    }
} catch { }
foreach ($r in @('C:\Program Files\Apache Software Foundation', 'C:\', (Join-Path $env:USERPROFILE 'Downloads'), (Join-Path $env:USERPROFILE 'Desktop'))) {
    if (Test-Path $r) { Get-ChildItem -LiteralPath $r -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -like '*tomcat*' } | ForEach-Object { $tomcats += $_.FullName } }
}
$tomcats = @($tomcats | Where-Object { $_ -and (Test-Path (Join-Path $_ 'conf\server.xml')) } | Select-Object -Unique)
if ($tomcats.Count -eq 0) { Out-Line 'Tomcat 폴더를 찾지 못함' 'Yellow' }
foreach ($t in $tomcats) {
    Out-Line ''
    Out-Line ("Tomcat 폴더: $t")
    $rn = Join-Path $t 'RELEASE-NOTES'
    if (Test-Path $rn) {
        $m = [regex]::Match((Get-Content -LiteralPath $rn -Raw), 'Apache Tomcat Version ([\d\.]+)')
        if ($m.Success) { Out-Line ('  버전: ' + $m.Groups[1].Value) }
    }
    $xml = [regex]::Replace((Get-Content -LiteralPath (Join-Path $t 'conf\server.xml') -Raw), '<!--[\s\S]*?-->', '')
    $port = 8080
    $mp = [regex]::Match($xml, '<Connector[^>]*?port="(\d+)"[^>]*?protocol="HTTP/1\.1"')
    if (-not $mp.Success) { $mp = [regex]::Match($xml, '<Connector[^>]*?port="(\d+)"') }
    if ($mp.Success) { $port = [int]$mp.Groups[1].Value }
    Out-Line ("  HTTP 포트: $port  (열림: " + (Test-PortOpen '127.0.0.1' $port) + ')')
    Out-Line ('  outdoor.war: ' + (Test-Path (Join-Path $t 'webapps\outdoor.war')) + ',  풀린 폴더: ' + (Test-Path (Join-Path $t 'webapps\outdoor')))
    $cf = Join-Path $t 'conf\outdoor-index.properties'
    if (Test-Path $cf) {
        Out-Line "  설정 파일: $cf"
        Get-Content -LiteralPath $cf -Encoding UTF8 | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object { Out-Line ('    ' + (Mask-Line $_)) }
    } else { Out-Line "  설정 파일 없음: $cf" 'Yellow' }
    Out-Line ("  http://localhost:$port/outdoor/search → " + (Http-Status "http://localhost:$port/outdoor/search"))
    $logs = Join-Path $t 'logs'
    if (Test-Path $logs) {
        foreach ($pat in @('catalina*.log', 'localhost.*.log', 'tomcat11-stderr*.log', 'commons-daemon*.log')) {
            $f = Get-ChildItem -LiteralPath $logs -Filter $pat -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($f) {
                Out-Line ("  --- 로그 마지막 25줄: " + $f.Name)
                Get-Content -LiteralPath $f.FullName -Tail 25 -ErrorAction SilentlyContinue | ForEach-Object { Out-Line ('    ' + $_) }
            }
        }
    }
}

# 단독 실행
Out-Line ''
Out-Line '[단독 실행판]' 'Cyan'
$envFile = Join-Path $PkgRoot '2-standalone\app\.env'
if (Test-Path $envFile) {
    Get-Content -LiteralPath $envFile -Encoding UTF8 | Where-Object { $_ -and -not $_.StartsWith('#') } | ForEach-Object { Out-Line ('  ' + (Mask-Line $_)) }
    $sp = 8090
    $mm = Select-String -LiteralPath $envFile -Pattern '^OUTDOOR_PORT=(\d+)' | Select-Object -First 1
    if ($mm) { $sp = [int]$mm.Matches[0].Groups[1].Value }
    Out-Line ("  http://localhost:$sp/search → " + (Http-Status "http://localhost:$sp/search"))
} else { Out-Line '  설정 안 됨 (SETUP.bat 을 아직 실행하지 않음)' }

# MySQL
Out-Line ''
Out-Line '[MySQL]' 'Cyan'
try {
    Get-CimInstance Win32_Service | Where-Object { $_.Name -like 'MySQL*' } | ForEach-Object { Out-Line ("서비스: {0}  상태={1}" -f $_.Name, $_.State) }
} catch { }
Out-Line ('localhost:3306 열림: ' + (Test-PortOpen '127.0.0.1' 3306))

# 인터넷 (공공데이터포털)
Out-Line ''
Out-Line '[인터넷]' 'Cyan'
Out-Line ('apis.data.go.kr:443 연결: ' + (Test-PortOpen 'apis.data.go.kr' 443))

$outFile = Join-Path $PkgRoot 'check-result.txt'
[System.IO.File]::WriteAllLines($outFile, [string[]]$report, (New-Object System.Text.UTF8Encoding($true)))
Write-Host ''
Write-Host "결과를 저장했어요: $outFile" -ForegroundColor Green
Read-Host '엔터를 누르면 창이 닫힙니다' | Out-Null
