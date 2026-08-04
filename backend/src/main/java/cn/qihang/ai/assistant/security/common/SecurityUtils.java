package cn.qihang.ai.assistant.security.common;

import cn.qihang.ai.assistant.common.ServiceException;
import cn.qihang.ai.assistant.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityUtils {
    public static Long getUserId() {
        try {
            return getLoginUser().getUserId();
        } catch (Exception e) {
            throw new ServiceException("获取用户ID异常", 401);
        }
    }

    public static Long getDeptId() {
        try {
            return getLoginUser().getDeptId();
        } catch (Exception e) {
            throw new ServiceException("获取部门ID异常", 401);
        }
    }

    public static String getUsername() {
        try {
            return getLoginUser().getUsername();
        } catch (Exception e) {
            throw new ServiceException("获取用户账户异常", 401);
        }
    }

    public static LoginUser getLoginUser() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ServiceException("未登录或登录已过期", 401);
        }
        try {
            return (LoginUser) auth.getPrincipal();
        } catch (Exception e) {
            throw new ServiceException("获取用户信息异常", 401);
        }
    }

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public static boolean isAdmin(Long userId) {
        return userId != null && 1L == userId;
    }

    public static boolean isLoggedIn() {
        try {
            Authentication auth = getAuthentication();
            return auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isGuest() {
        return !isLoggedIn();
    }
}