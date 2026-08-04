package cn.qihang.ai.assistant.common.exception;

public class CaptchaExpireException extends UserException {
    private static final long serialVersionUID = 1L;

    public CaptchaExpireException() {
        super("user.jcaptcha.expire", null, "验证码已过期");
    }
}