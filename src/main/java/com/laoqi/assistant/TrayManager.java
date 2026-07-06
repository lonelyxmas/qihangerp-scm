package com.laoqi.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 系统托盘管理 — 打包成 EXE 后右下角显示图标。
 * 全部通过反射实现，不依赖 java.desktop 模块。
 * 如果运行时没有 java.desktop 模块，托盘图标静默跳过，不影响应用运行。
 */
@Component
public class TrayManager {

    private static final Logger log = LoggerFactory.getLogger(TrayManager.class);

    private final ApplicationContext appContext;

    @Value("${server.port:6790}")
    private int port;

    @Value("${app.tray-enabled:true}")
    private boolean trayEnabled;

    /** 反射持有的 trayIcon 对象，用于移除 */
    private Object trayIconRef;

    public TrayManager(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onServerReady() {
        if (!trayEnabled) return;
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            // 在 AWT EventQueue（EDT）上创建托盘，避免线程问题
            installTrayOnEdt();
        }, "tray-init").start();
    }

    /** 通过 EventQueue.invokeLater 在 EDT 线程安装托盘 */
    private void installTrayOnEdt() {
        try {
            // 0. 强制启用非 headless 模式（部分 jlink 运行时默认 headless）
            System.setProperty("java.awt.headless", "false");

            // 1. 初始化 AWT Toolkit
            Class<?> tkClass = Class.forName("java.awt.Toolkit");
            tkClass.getMethod("getDefaultToolkit").invoke(null);

            // 2. 在 EDT 上创建托盘（不检查 isSupported，直接 try）
            Class<?> eqClass = Class.forName("java.awt.EventQueue");
            Method invokeLater = eqClass.getMethod("invokeLater", Runnable.class);
            invokeLater.invoke(null, (Runnable) () -> {
                try {
                    installTrayReflect();
                } catch (Exception e) {
                    log.warn("[Tray] 托盘安装失败（非桌面环境或 AWT 不支持）: {}", e.toString());
                }
            });
        } catch (ClassNotFoundException e) {
            log.info("[Tray] java.desktop 模块未加载，跳过托盘");
        } catch (Exception e) {
            log.warn("[Tray] 托盘初始化失败: {}", e.toString());
        }
    }

    /** 全反射创建托盘图标（已在 EDT 线程上执行） */
    private void installTrayReflect() throws Exception {
        String url = "http://localhost:" + port;

        Class<?> systemTrayClass = Class.forName("java.awt.SystemTray");
        Object tray = systemTrayClass.getMethod("getSystemTray").invoke(null);

        // 创建图标
        Object image = createIconReflect();

        // 创建 TrayIcon
        Class<?> trayIconClass = Class.forName("java.awt.TrayIcon");
        Object trayIcon = trayIconClass.getConstructor(
                Class.forName("java.awt.Image"), String.class)
                .newInstance(image, "BiLing AI 笔记助理\n" + url);
        trayIconClass.getMethod("setImageAutoSize", boolean.class).invoke(trayIcon, true);
        this.trayIconRef = trayIcon;

        // 右键菜单
        Object popup = createPopupReflect(url, tray, trayIcon);
        trayIconClass.getMethod("setPopupMenu", Class.forName("java.awt.PopupMenu"))
                .invoke(trayIcon, popup);

        // 双击打开
        Object actionListener = createActionListenerReflect(() -> openBrowser(url));
        trayIconClass.getMethod("addActionListener", Class.forName("java.awt.event.ActionListener"))
                .invoke(trayIcon, actionListener);

        // 添加到系统托盘
        systemTrayClass.getMethod("add", trayIconClass).invoke(tray, trayIcon);
        log.info("[Tray] ✅ 托盘图标已添加 (右键→退出可关闭应用)");
    }

    /** 反射绘制 16x16 蓝色图标 */
    private Object createIconReflect() throws Exception {
        Class<?> biClass = Class.forName("java.awt.image.BufferedImage");
        int typeIntArgb = biClass.getField("TYPE_INT_ARGB").getInt(null);
        Object img = biClass.getConstructor(int.class, int.class, int.class)
                .newInstance(16, 16, typeIntArgb);

        Class<?> g2dClass = Class.forName("java.awt.Graphics2D");
        Object g = biClass.getMethod("createGraphics").invoke(img);
        try {
            // 抗锯齿
            Class<?> rhClass = Class.forName("java.awt.RenderingHints");
            g2dClass.getMethod("setRenderingHint", Class.forName("java.awt.RenderingHints$Key"), Object.class)
                    .invoke(g,
                            rhClass.getField("KEY_ANTIALIASING").get(null),
                            rhClass.getField("VALUE_ANTIALIAS_ON").get(null));

            // 蓝色圆形
            Class<?> colorClass = Class.forName("java.awt.Color");
            Object blue = colorClass.getConstructor(int.class, int.class, int.class)
                    .newInstance(0x4F, 0x6E, 0xF5);
            g2dClass.getMethod("setColor", Class.forName("java.awt.Paint"))
                    .invoke(g, blue);
            g2dClass.getMethod("fillOval", int.class, int.class, int.class, int.class)
                    .invoke(g, 0, 0, 16, 16);

            // 白色字母 B
            g2dClass.getMethod("setColor", Class.forName("java.awt.Paint"))
                    .invoke(g, colorClass.getField("WHITE").get(null));
            g2dClass.getMethod("setFont", Class.forName("java.awt.Font"))
                    .invoke(g, Class.forName("java.awt.Font")
                            .getConstructor(String.class, int.class, int.class)
                            .newInstance("Arial", 1, 11)); // 1 = Font.BOLD
            g2dClass.getMethod("drawString", String.class, int.class, int.class)
                    .invoke(g, "B", 3, 13);
        } finally {
            g2dClass.getMethod("dispose").invoke(g);
        }
        return img;
    }

    /** 反射构建 PopupMenu（打开界面 / 运行状态 / 退出） */
    private Object createPopupReflect(String url, Object tray, Object trayIcon) throws Exception {
        Class<?> popupClass = Class.forName("java.awt.PopupMenu");
        Class<?> itemClass = Class.forName("java.awt.MenuItem");
        var alClass = Class.forName("java.awt.event.ActionListener");

        // 分隔线
        Class<?> sepClass = Class.forName("java.awt.MenuItem");

        Object popup = popupClass.getConstructor().newInstance();

        // "打开界面"
        Object openItem = itemClass.getConstructor(String.class).newInstance("打开界面");
        openItem.getClass().getMethod("addActionListener", alClass)
                .invoke(openItem, createActionListenerReflect(() -> openBrowser(url)));
        popupClass.getMethod("add", Class.forName("java.awt.MenuItem")).invoke(popup, openItem);

        // 分隔线
        popupClass.getMethod("insertSeparator", int.class).invoke(popup, 1);

        // 状态显示
        Object statusItem = itemClass.getConstructor(String.class).newInstance("端口: " + port);
        statusItem.getClass().getMethod("setEnabled", boolean.class).invoke(statusItem, false);
        popupClass.getMethod("add", Class.forName("java.awt.MenuItem")).invoke(popup, statusItem);

        // 分隔线
        popupClass.getMethod("insertSeparator", int.class).invoke(popup, 3);

        // "退出"
        Object exitItem = itemClass.getConstructor(String.class).newInstance("退出");
        exitItem.getClass().getMethod("addActionListener", alClass)
                .invoke(exitItem, createActionListenerReflect(() -> {
                    try {
                        systemTrayRemove(tray, trayIcon);
                    } catch (Exception ignored) {}
                    shutdown();
                }));
        popupClass.getMethod("add", Class.forName("java.awt.MenuItem")).invoke(popup, exitItem);

        return popup;
    }

    private void systemTrayRemove(Object tray, Object trayIcon) throws Exception {
        Class<?> st = Class.forName("java.awt.SystemTray");
        st.getMethod("remove", Class.forName("java.awt.TrayIcon")).invoke(tray, trayIcon);
    }

    /** 创建 ActionListener 代理 */
    private Object createActionListenerReflect(Runnable action) throws Exception {
        Class<?> alClass = Class.forName("java.awt.event.ActionListener");
        return Proxy.newProxyInstance(
                alClass.getClassLoader(),
                new Class[]{alClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("actionPerformed")) {
                        action.run();
                    }
                    return null;
                });
    }

    /** 打开默认浏览器 */
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
        } catch (Exception ex) {
            log.warn("[Tray] 打开浏览器失败: {}", ex.getMessage());
        }
    }

    /** 优雅关闭 Spring Boot */
    private void shutdown() {
        log.info("[Tray] 用户选择退出，正在关闭应用...");
        int exitCode = SpringApplication.exit(appContext, () -> 0);
        System.exit(exitCode);
    }
}
