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
}
