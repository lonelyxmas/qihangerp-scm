package cn.qihang.ai.assistant.mapper;

import cn.qihang.ai.assistant.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysRoleMapper {
    List<SysRole> selectRoleList(SysRole role);
    List<SysRole> selectRolePermissionByUserId(Long userId);
    List<SysRole> selectRoleAll();
    List<Long> selectRoleListByUserId(Long userId);
    SysRole selectRoleById(Long roleId);
    List<SysRole> selectRolesByUserName(String userName);
    SysRole checkRoleNameUnique(String roleName);
    SysRole checkRoleKeyUnique(String roleKey);
    int updateRole(SysRole role);
    int insertRole(SysRole role);
    int deleteRoleById(Long roleId);
    int deleteRoleByIds(Long[] roleIds);
}