package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.SysRole;
import cn.qihang.ai.assistant.entity.SysUserRole;

import java.util.List;
import java.util.Set;

public interface ISysRoleService {
    List<SysRole> selectRoleList(SysRole role);
    List<SysRole> selectRolesByUserId(Long userId);
    Set<String> selectRolePermissionByUserId(Long userId);
    List<SysRole> selectRoleAll();
    List<Long> selectRoleListByUserId(Long userId);
    SysRole selectRoleById(Long roleId);
    boolean checkRoleNameUnique(SysRole role);
    boolean checkRoleKeyUnique(SysRole role);
    void checkRoleAllowed(SysRole role);
    void checkRoleDataScope(Long roleId, Long userId);
    int countUserRoleByRoleId(Long roleId);
    int insertRole(SysRole role);
    int updateRole(SysRole role);
    int updateRoleStatus(SysRole role);
    int authDataScope(SysRole role);
    int deleteRoleById(Long roleId);
    int deleteRoleByIds(Long[] roleIds, Long userId);
    int deleteAuthUser(SysUserRole userRole);
    int deleteAuthUsers(Long roleId, Long[] userIds);
    int insertAuthUsers(Long roleId, Long[] userIds);
}