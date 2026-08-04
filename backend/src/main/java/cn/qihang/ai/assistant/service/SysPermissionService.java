package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SysPermissionService {
    @Autowired
    private ISysRoleService roleService;

    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new HashSet<String>();
        if (user.isAdmin()) roles.add("admin");
        else roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        return roles;
    }

    public Set<String> getMenuPermission(SysUser user) {
        Set<String> perms = new HashSet<String>();
        if (user.isAdmin()) perms.add("*:*:*");
        else perms.add("*:*:*");
        return perms;
    }
}