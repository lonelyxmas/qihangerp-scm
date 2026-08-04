package cn.qihang.ai.assistant.mapper;

import cn.qihang.ai.assistant.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {
    int deleteUserRoleByUserId(Long userId);
    int deleteUserRole(Long[] ids);
    int countUserRoleByRoleId(Long roleId);
    int batchUserRole(List<SysUserRole> userRoleList);
    int deleteUserRoleInfo(SysUserRole userRole);
    int deleteUserRoleInfos(@Param("roleId") Long roleId, @Param("userIds") Long[] userIds);
}