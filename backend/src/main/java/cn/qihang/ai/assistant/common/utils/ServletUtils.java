package cn.qihang.ai.assistant.common.utils;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ServletUtils {
    public static String getParameter(String name) { return getRequest().getParameter(name); }
    public static String getParameter(String name, String defaultValue) {
        String v = getRequest().getParameter(name);
        return v == null ? defaultValue : v;
    }
    public static Integer getParameterToInt(String name) {
        String v = getRequest().getParameter(name);
        return v == null ? null : Integer.parseInt(v);
    }
    public static Integer getParameterToInt(String name, Integer defaultValue) {
        String v = getRequest().getParameter(name);
        return v == null ? defaultValue : Integer.parseInt(v);
    }
    public static Boolean getParameterToBool(String name) {
        String v = getRequest().getParameter(name);
        return v == null ? null : Boolean.parseBoolean(v);
    }
    public static Boolean getParameterToBool(String name, Boolean defaultValue) {
        String v = getRequest().getParameter(name);
        return v == null ? defaultValue : Boolean.parseBoolean(v);
    }

    public static Map<String, String[]> getParams(ServletRequest request) {
        return Collections.unmodifiableMap(request.getParameterMap());
    }

    public static Map<String, String> getParamMap(ServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : getParams(request).entrySet()) {
            params.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
        }
        return params;
    }

    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }
    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }
    public static HttpSession getSession() { return getRequest().getSession(); }

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    public static void renderString(HttpServletResponse response, String string) {
        try {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("accept");
        if (accept != null && accept.contains("application/json")) return true;
        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith != null && xRequestedWith.contains("XMLHttpRequest")) return true;
        String uri = request.getRequestURI();
        if (StringUtils.inStringIgnoreCase(uri, ".json", ".xml")) return true;
        String ajax = request.getParameter("__ajax");
        return StringUtils.inStringIgnoreCase(ajax, "json", "xml");
    }

    public static String urlEncode(String str) {
        try { return URLEncoder.encode(str, "UTF-8"); } catch (UnsupportedEncodingException e) { return StringUtils.EMPTY; }
    }

    public static String urlDecode(String str) {
        try { return URLDecoder.decode(str, "UTF-8"); } catch (UnsupportedEncodingException e) { return StringUtils.EMPTY; }
    }
}