package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.model.AjaxResult;
import cn.qihang.ai.assistant.model.LoginBody;
import cn.qihang.ai.assistant.controller.BaseController;
import cn.qihang.ai.assistant.security.common.Constants;
import cn.qihang.ai.assistant.security.common.SecurityUtils;
import cn.qihang.ai.assistant.security.service.SysLoginService;
import cn.qihang.ai.assistant.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
public class SysLoginController extends BaseController {

    @Autowired
    private SysLoginService loginService;

    @Autowired
    private SysPermissionService permissionService;

    @PostMapping("/api/sys-api/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        try {
            AjaxResult ajax = AjaxResult.success();
            String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(), loginBody.getUuid());
            ajax.put(Constants.TOKEN, token);
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error(500, e.getMessage());
        }
    }

    @GetMapping("/api/sys-api/getInfo")
    public AjaxResult getInfo() {
        SysUser user = SecurityUtils.getLoginUser().getUser();
        Set<String> roles = permissionService.getRolePermission(user);
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }
}