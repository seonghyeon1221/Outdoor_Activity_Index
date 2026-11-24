package com.outdoorindex;

import com.outdoorindex.util.ApiConfig;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.JarResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 내장 Tomcat 11로 서버를 실행하는 시작 클래스. (Tomcat 따로 설치할 필요 없음)
 * - 실행: ./서버시작.command 더블클릭, 또는 터미널에서 ./run.sh
 * - 이클립스/인텔리제이: 이 클래스를 "Java Application"으로 실행 (작업 폴더 = 프로젝트 폴더)
 *
 * 하는 일
 *   1) 프로젝트 폴더의 .env 파일을 읽어 설정값을 등록 (이미 있는 환경변수가 우선)
 *   2) src/main/webapp 을 웹 루트로, 컴파일된 클래스를 /WEB-INF/classes 로 연결
 *   3) JSP는 JDK 컴파일러(javax.tools)로 컴파일
 *   4) http://localhost:{OUTDOOR_PORT, 기본 8080}/ 에서 서버 시작
 */
public final class Launcher {

    public static final String ENV_PORT = "OUTDOOR_PORT";
    private static final int DEFAULT_PORT = 8080;

    private Launcher() {}

    public static void main(String[] args) throws Exception {
        Path root = findProjectRoot();
        int loaded = loadDotEnv(root.resolve(".env"));

        int port = parsePort(setting(ENV_PORT));
        Path webapp = root.resolve("src/main/webapp").toAbsolutePath().normalize();
        Path classes = Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(root.resolve("target/tomcat").toAbsolutePath().toString());
        tomcat.setPort(port);
        tomcat.getConnector();                          // 기본 HTTP 커넥터 생성

        Context ctx = tomcat.addWebapp("", webapp.toString());

        // 컴파일된 클래스(@WebServlet, @WebFilter 포함)를 /WEB-INF/classes 로 연결
        WebResourceRoot resources = new StandardRoot(ctx);
        if (Files.isDirectory(classes)) {
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", classes.toString(), "/"));
        } else {
            resources.addPreResources(new JarResourceSet(resources, "/WEB-INF/classes", classes.toString(), "/"));
        }
        ctx.setResources(resources);

        // JSP를 JDK 컴파일러로 컴파일 + 수정하면 바로 반영 (개발 모드)
        ctx.addLifecycleListener(event -> {
            if (Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
                Container jsp = ctx.findChild("jsp");
                if (jsp instanceof Wrapper) {
                    Wrapper w = (Wrapper) jsp;
                    // JDK가 있으면 JDK 컴파일러, JRE만 있으면 Tomcat 기본(JDT) 컴파일러 사용
                    if (javax.tools.ToolProvider.getSystemJavaCompiler() != null) {
                        w.addInitParameter("compilerClassName", "org.apache.jasper.compiler.JavaCompiler");
                    }
                    w.addInitParameter("development", "true");
                    w.addInitParameter("compilerSourceVM", "17");
                    w.addInitParameter("compilerTargetVM", "17");
                }
            }
        });

        try {
            tomcat.start();
        } catch (LifecycleException e) {
            System.err.println("서버를 시작하지 못했어요: " + e.getMessage());
            System.exit(1);
        }
        if (tomcat.getConnector().getState() != LifecycleState.STARTED || tomcat.getConnector().getLocalPort() < 0) {
            System.err.println();
            System.err.println("  ✖ " + port + "번 포트를 이미 다른 프로그램이 쓰고 있어요.");
            System.err.println("    ./stop.sh 로 기존 서버를 끄거나, .env 의 OUTDOOR_PORT 를 바꿔 주세요.");
            tomcat.stop();
            tomcat.destroy();
            System.exit(2);
        }

        String url = "http://localhost:" + port + "/";
        System.out.println();
        System.out.println("  ================================================");
        System.out.println("   ☀ 야외활동 지수 서버가 시작됐어요");
        System.out.println("     주소: " + url);
        System.out.println("     종료: 이 창에서 Ctrl + C  (또는 ./stop.sh)");
        System.out.println("  ================================================");
        System.out.println("  .env 설정 " + loaded + "개 읽음");
        boolean dbOk = com.outdoorindex.dao.Db.isAvailable();
        System.out.println("  DB: " + com.outdoorindex.dao.Db.getDescription()
                + (dbOk ? " ✔" : " ✖ → " + com.outdoorindex.dao.Db.getLastError()));
        System.out.println("  서비스키: " + (ApiConfig.hasServiceKey()
                ? "설정됨 ✔ (" + keySource() + ")"
                : "없음 ✖  → .env 의 OUTDOOR_SERVICE_KEY 를 확인하세요"));
        System.out.println();

        tomcat.getServer().await();
    }

    /** src/main/webapp 이 있는 폴더를 현재 폴더부터 위로 올라가며 찾기 */
    static Path findProjectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (Path p = dir; p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("src/main/webapp"))) return p;
        }
        throw new IllegalStateException("프로젝트 폴더(src/main/webapp 이 있는 곳)에서 실행해 주세요. 현재: " + dir);
    }

    /**
     * .env 파일(KEY=VALUE 형식)을 읽어 JVM 시스템 속성으로 등록한다.
     * 같은 이름의 환경변수가 이미 있으면 환경변수가 우선이므로 등록하지 않는다.
     * @return 등록한 개수
     */
    static int loadDotEnv(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return 0;
        Map<String, String> env = System.getenv();
        int count = 0;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("﻿", "").trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("export ")) line = line.substring(7).trim();
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (env.containsKey(key) && !env.get(key).isBlank()) continue;
                System.setProperty(key, value);
                count++;
            }
        }
        return count;
    }

    /** 서비스키를 어디서 읽었는지 (안내용) */
    private static String keySource() {
        String env = System.getenv(ApiConfig.ENV_SERVICE_KEY);
        if (env != null && !env.isBlank()) return "환경변수";
        String prop = System.getProperty(ApiConfig.ENV_SERVICE_KEY);
        if (prop != null && !prop.isBlank()) return ".env 파일";
        return "api.properties";
    }

    private static String setting(String name) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? System.getProperty(name) : v;
    }

    private static int parsePort(String value) {
        if (value == null || value.isBlank()) return DEFAULT_PORT;
        try {
            int p = Integer.parseInt(value.trim());
            return p > 0 && p < 65536 ? p : DEFAULT_PORT;
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

}
