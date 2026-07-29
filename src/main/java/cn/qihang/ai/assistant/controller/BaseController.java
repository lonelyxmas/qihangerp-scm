package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.model.AjaxResult;
import cn.qihang.ai.assistant.model.TableDataInfo;
import cn.qihang.ai.assistant.security.LoginUser;
import cn.qihang.ai.assistant.security.common.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class BaseController {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected Long getUserId() {
        return SecurityUtils.getUserId();
    }

    protected String getUsername() {
        return SecurityUtils.getUsername();
    }

    protected LoginUser getLoginUser() {
        return SecurityUtils.getLoginUser();
    }

    protected AjaxResult success() {
        return AjaxResult.success();
    }

    protected AjaxResult success(Object data) {
        return AjaxResult.success(data);
    }

    protected AjaxResult success(String msg, Object data) {
        return AjaxResult.success(msg, data);
    }

    protected AjaxResult error() {
        return AjaxResult.error();
    }

    protected AjaxResult error(String msg) {
        return AjaxResult.error(msg);
    }

    protected AjaxResult error(int code, String msg) {
        return AjaxResult.error(code, msg);
    }

    protected AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    protected AjaxResult toAjax(boolean result) {
        return result ? AjaxResult.success() : AjaxResult.error();
    }

    protected TableDataInfo getDataTable(List<?> list) {
        return new TableDataInfo(list.size(), list);
    }
}
