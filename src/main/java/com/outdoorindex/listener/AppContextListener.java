package com.outdoorindex.listener;

import com.outdoorindex.dao.Db;
import com.outdoorindex.util.ApiConfig;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

/**
 * 서버 시작 시 한 번 실행되는 준비 작업.
 * - DB 테이블 준비 (Db.isAvailable)
 * - 모든 화면에서 쓰는 값 등록: demoMode (시연 재생 모드면 머리글에 표시)
 * 서버 종료 · 재배포 시 정리 작업 (contextDestroyed)
 * - 이 웹앱이 등록한 JDBC 드라이버 해제, MySQL 드라이버의 정리 스레드 종료
 * - 이클립스에서 저장할 때마다 일어나는 재배포에서 메모리 누수 경고(SEVERE)가 쌓이지 않게
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext app = sce.getServletContext();
        String mode = ApiConfig.getDemoMode();
        app.setAttribute("demoMode", mode);
        app.setAttribute("dbAvailable", Db.isAvailable());
        LOG.info("DB=" + Db.getDescription() + (Db.isAvailable() ? " (연결됨)" : " (연결 실패: " + Db.getLastError() + ")"));
        LOG.info("외부 설정 파일=" + (ApiConfig.getExternalConfigPath() == null ? "없음" : ApiConfig.getExternalConfigPath())
                + ", 서비스키=" + (ApiConfig.hasServiceKey() ? "있음" : "없음") + ", DB=" + (ApiConfig.getDbUrl().isBlank() ? "H2(내장)" : ApiConfig.getDbUrl()));
        LOG.info("시연 모드=" + mode + ", 캐시=" + ApiConfig.getCacheTtlSeconds() + "초, 데이터 폴더=" + ApiConfig.getDataDir());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ClassLoader webappLoader = Thread.currentThread().getContextClassLoader();

        // 1) 이 웹앱(WEB-INF/lib)이 등록한 JDBC 드라이버만 해제 (MySQL, H2)
        java.util.Enumeration<java.sql.Driver> drivers = java.sql.DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            java.sql.Driver d = drivers.nextElement();
            if (d.getClass().getClassLoader() == webappLoader) {
                try {
                    java.sql.DriverManager.deregisterDriver(d);
                } catch (java.sql.SQLException e) {
                    LOG.warning("JDBC 드라이버 해제 실패: " + d + " (" + e.getMessage() + ")");
                }
            }
        }

        // 2) MySQL 드라이버가 만든 정리 스레드 종료 (드라이버가 없으면 건너뜀)
        try {
            Class<?> cleanup = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread", false, webappLoader);
            cleanup.getMethod("checkedShutdown").invoke(null);
        } catch (ClassNotFoundException ignored) {
            // MySQL 드라이버를 쓰지 않는 환경
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.warning("MySQL 정리 스레드 종료 실패: " + e);
        }
    }
}
