package com.laoqi.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 桌面 UI 集成 — 打包成 EXE 后自动弹出浏览器。
 * 通过 cmd /c start 实现，不依赖 java.desktop 模块。
 */
@Component
public class DesktopUI {

    private static final Logger log = LoggerFactory.getLogger(DesktopUI.class);

    @Value("${server.port:6790}")
    private int port;

    @EventListener(ApplicationReadyEvent.class)
    public void onServerReady() {
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            String url = "http://localhost:" + port;
            log.info("[DesktopUI] 应用已启动: {}", url);
            openBrowser(url);
        }, "desktop-ui").start();
    }

    private void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();
            if (os.contains("win")) {
                rt.exec(new String[]{"cmd", "/c", "start", url});
            } else if (os.contains("mac")) {
                rt.exec(new String[]{"open", url});
            } else {
                rt.exec(new String[]{"xdg-open", url});
            }
            log.info("[DesktopUI] 浏览器已打开: {}", url);
        } catch (Exception e) {
            log.warn("[DesktopUI] 打开浏览器失败，请手动访问: {}", url);
        }
    }
}