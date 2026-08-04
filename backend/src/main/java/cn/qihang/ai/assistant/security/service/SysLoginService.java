package cn.qihang.ai.assistant.security.service;

import cn.qihang.ai.assistant.common.ServiceException;
import cn.qihang.ai.assistant.common.constant.UserConstants;
import cn.qihang.ai.assistant.common.exception.UserException;
import cn.qihang.ai.assistant.common.utils.DateUtils;
import cn.qihang.ai.assistant.common.utils.IpUtils;
import cn.qihang.ai.assistant.common.utils.StringUtils;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.security.AuthenticationContextHolder;
import cn.qihang.ai.assistant.security.LoginUser;
import cn.qihang.ai.assistant.security.TokenService;
import cn.qihang.ai.assistant.security.UserPasswordNotMatchException;
import cn.qihang.ai.assistant.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class SysLoginService {
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private ISysUserService userService;

    public String login(String username, String password, String code, String uuid) {
        loginPreCheck(username, password);
        Authentication authentication = null;
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                throw new UserPasswordNotMatchException();
            } else {
                throw new ServiceException(e.getMessage());
            }
        } finally {
            AuthenticationContextHolder.clearContext();
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        return tokenService.createToken(loginUser);
    }

    public void loginPreCheck(String username, String password) {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new UserException("user.not.exists", null, "用户或密码错误");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            throw new UserPasswordNotMatchException();
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            throw new UserPasswordNotMatchException();
        }
    }

    public void recordLoginInfo(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(IpUtils.getIpAddr());
        sysUser.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(sysUser);
    }
}