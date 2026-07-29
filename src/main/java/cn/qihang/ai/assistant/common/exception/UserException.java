package cn.qihang.ai.assistant.common.exception;

public class UserException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String module;
    private String code;
    private Object[] args;
    private String defaultMessage;

    public UserException(String module, String code, Object[] args, String defaultMessage) {
        this.module = module;
        this.code = code;
        this.args = args;
        this.defaultMessage = defaultMessage;
    }

    public UserException(String code, Object[] args, String defaultMessage) {
        this("user", code, args, defaultMessage);
    }

    @Override
    public String getMessage() {
        return defaultMessage != null ? defaultMessage : code;
    }

    public String getModule() { return module; }
    public String getCode() { return code; }
    public Object[] getArgs() { return args; }
    public String getDefaultMessage() { return defaultMessage; }
}