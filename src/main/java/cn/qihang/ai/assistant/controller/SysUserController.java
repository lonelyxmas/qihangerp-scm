package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.common.utils.StringUtils;
import cn.qihang.ai.assistant.entity.SysRole;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.model.AjaxResult;
import cn.qihang.ai.assistant.model.TableDataInfo;
import cn.qihang.ai.assistant.controller.BaseController;
import cn.qihang.ai.assistant.security.common.SecurityUtils;
import cn.qihang.ai.assistant.service.ISysRoleService;
import cn.qihang.ai.assistant.service.ISysUserService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys-api/system/user")
public class SysUserController extends BaseController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @GetMapping("/list")
    public TableDataInfo list(SysUser user) {
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        userService.checkUserDataScope(userId);
        SysUser sysUser = userService.selectUserById(userId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(AjaxResult.DATA_TAG, sysUser);
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    @PostMapping
    public AjaxResult add(@RequestBody SysUser user) {
        if (!userService.checkUserNameUnique(user))
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    @PutMapping
    public AjaxResult edit(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        if (!userService.checkUserNameUnique(user))
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        if (ArrayUtils.contains(userIds, getUserId())) return error("当前用户不能删除");
        for (var userId : userIds) userService.deleteUserById(userId);
        return toAjax(userIds.length);
    }

    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(getUsername());
        return toAjax(userService.resetPwd(user));
    }

    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user) {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId) {
        SysUser user = userService.selectUserById(userId);
        if (user == null) return error("用户不存在");
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        return ajax;
    }

    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, String roleIds) {
        userService.checkUserDataScope(userId);
        userService.insertUserAuth(userId, StringUtils.splitToLongArray(roleIds));
        return success();
    }
}