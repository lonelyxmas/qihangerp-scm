package cn.qihang.ai.assistant.security.common;

import io.jsonwebtoken.Claims;

public class Constants {
    public static final String LOGIN_SUCCESS = "Success";
    public static final String LOGOUT = "Logout";
    public static final String REGISTER = "Register";
    public static final String LOGIN_FAIL = "Error";
    public static final Integer CAPTCHA_EXPIRATION = 2;
    public static final String TOKEN = "token";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String LOGIN_USER_KEY = "login_user_key";
    public static final String JWT_USERID = "userid";
    public static final String JWT_USERNAME = Claims.SUBJECT;
    public static final String JWT_AVATAR = "avatar";
    public static final String JWT_CREATED = "created";
    public static final String JWT_AUTHORITIES = "authorities";
    public static final String RESOURCE_PREFIX = "/profile";
    public static final String LOOKUP_RMI = "rmi:";
    public static final String LOOKUP_LDAP = "ldap:";
    public static final String LOOKUP_LDAPS = "ldaps:";
}