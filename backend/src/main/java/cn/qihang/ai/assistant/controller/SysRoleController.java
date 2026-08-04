package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.common.utils.StringUtils;
import cn.qihang.ai.assistant.entity.SysRole;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.entity.SysUserRole;
import cn.qihang.ai.assistant.model.AjaxResult;
import cn.qihang.ai.assistant.model.TableDataInfo;
import cn.qihang.ai.assistant.security.LoginUser;
import cn.qihang.ai.assistant.security.TokenService;
import cn.qihang.ai.assistant.controller.BaseController;
import cn.qihang.ai.assistant.service.ISysRoleService;
import cn.qihang.ai.assistant.service.ISysUserService;
import cn.qihang.ai.assistant.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys-api/system/role")
public class SysRoleController extends BaseController {

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserService userService;

    @GetMapping("/list")
    public TableDataInfo list(SysRole role) {
        List<SysRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId) {
        roleService.checkRoleDataScope(roleId, getUserId());
        return success(roleService.selectRoleById(roleId));
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysRole role) {
        if (!roleService.checkRoleNameUnique(role))
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        else if (!roleService.checkRoleKeyUnique(role))
            return error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        role.setCreateBy(getUsername());
        return toAjax(roleService.insertRole(role));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysRole role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId(), getUserId());
        if (!roleService.checkRoleNameUnique(role))
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        else if (!roleService.checkRoleKeyUnique(role))
            return error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        role.setUpdateBy(getUsername());
        if (roleService.updateRole(role) > 0) {
            LoginUser loginUser = getLoginUser();
            if (StringUtils.isNotNull(loginUser.getUser()) && !loginUser.getUser().isAdmin()) {
                loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
                loginUser.setUser(userService.selectUserByUserName(loginUser.getUser().getUserName()));
                tokenService.setLoginUser(loginUser);
            }
            return success();
        }
        return error("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
    }

    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysRole role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId(), getUserId());
        role.setUpdateBy(getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds) {
        return toAjax(roleService.deleteRoleByIds(roleIds, getUserId()));
    }

    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        return success(roleService.selectRoleAll());
    }

    @GetMapping("/authUser/allocatedList")
    public TableDataInfo allocatedList(SysUser user) {
        List<SysUser> list = userService.selectAllocatedList(user);
        return getDataTable(list);
    }

    @GetMapping("/authUser/unallocatedList")
    public TableDataInfo unallocatedList(SysUser user) {
        List<SysUser> list = userService.selectUnallocatedList(user);
        return getDataTable(list);
    }

    @PutMapping("/authUser/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole) {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    @PutMapping("/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(Long roleId, Long[] userIds) {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    @PutMapping("/authUser/selectAll")
    public AjaxResult selectAuthUserAll(Long roleId, Long[] userIds) {
        roleService.checkRoleDataScope(roleId, getUserId());
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }
}