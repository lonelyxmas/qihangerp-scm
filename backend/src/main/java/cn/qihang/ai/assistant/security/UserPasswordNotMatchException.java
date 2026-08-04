package cn.qihang.ai.assistant.security;

import cn.qihang.ai.assistant.common.exception.UserException;

public class UserPasswordNotMatchException extends UserException {
    private static final long serialVersionUID = 1L;

    public UserPasswordNotMatchException() {
        super("user.password.not.match", null, "用户密码错误");
    }
}