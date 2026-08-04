package cn.qihang.ai.assistant.service.impl;

import cn.qihang.ai.assistant.common.ServiceException;
import cn.qihang.ai.assistant.common.constant.UserConstants;
import cn.qihang.ai.assistant.common.utils.StringUtils;
import cn.qihang.ai.assistant.entity.SysRole;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.entity.SysUserRole;
import cn.qihang.ai.assistant.mapper.SysRoleMapper;
import cn.qihang.ai.assistant.mapper.SysUserRoleMapper;
import cn.qihang.ai.assistant.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SysRoleServiceImpl implements ISysRoleService {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Override
    public List<SysRole> selectRoleList(SysRole role) { return roleMapper.selectRoleList(role); }

    @Override
    public List<SysRole> selectRolesByUserId(Long userId) {
        List<SysRole> userRoles = roleMapper.selectRolePermissionByUserId(userId);
        List<SysRole> roles = selectRoleAll();
        for (SysRole role : roles) {
            for (SysRole userRole : userRoles) {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue()) {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRole> perms = roleMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms) {
            if (StringUtils.isNotNull(perm)) permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
        }
        return permsSet;
    }

    @Override
    public List<SysRole> selectRoleAll() { return this.selectRoleList(new SysRole()); }

    @Override
    public List<Long> selectRoleListByUserId(Long userId) { return roleMapper.selectRoleListByUserId(userId); }

    @Override
    public SysRole selectRoleById(Long roleId) { return roleMapper.selectRoleById(roleId); }

    @Override
    public boolean checkRoleNameUnique(SysRole role) {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        SysRole info = roleMapper.checkRoleNameUnique(role.getRoleName());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue()) return UserConstants.NOT_UNIQUE;
        return UserConstants.UNIQUE;
    }

    @Override
    public boolean checkRoleKeyUnique(SysRole role) {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        SysRole info = roleMapper.checkRoleKeyUnique(role.getRoleKey());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue()) return UserConstants.NOT_UNIQUE;
        return UserConstants.UNIQUE;
    }

    @Override
    public void checkRoleAllowed(SysRole role) {
        if (StringUtils.isNotNull(role.getRoleId()) && role.isAdmin())
            throw new ServiceException("不允许操作超级管理员角色");
    }

    @Override
    public void checkRoleDataScope(Long roleId, Long userId) {
        if (!SysUser.isAdmin(userId)) {
            SysRole role = new SysRole();
            role.setRoleId(roleId);
            List<SysRole> roles = selectRoleList(role);
            if (StringUtils.isEmpty(roles)) throw new ServiceException("没有权限访问角色数据!");
        }
    }

    @Override
    public int countUserRoleByRoleId(Long roleId) { return userRoleMapper.countUserRoleByRoleId(roleId); }

    @Override
    @Transactional
    public int insertRole(SysRole role) {
        return roleMapper.insertRole(role);
    }

    @Override
    @Transactional
    public int updateRole(SysRole role) {
        return roleMapper.updateRole(role);
    }

    @Override
    public int updateRoleStatus(SysRole role) { return roleMapper.updateRole(role); }

    @Override
    @Transactional
    public int authDataScope(SysRole role) {
        roleMapper.updateRole(role);
        return 1;
    }

    @Override
    @Transactional
    public int deleteRoleById(Long roleId) {
        return roleMapper.deleteRoleById(roleId);
    }

    @Override
    @Transactional
    public int deleteRoleByIds(Long[] roleIds, Long userId) {
        for (Long roleId : roleIds) {
            checkRoleAllowed(new SysRole(roleId));
            checkRoleDataScope(roleId, userId);
            SysRole role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0)
                throw new ServiceException(String.format("%1$s已分�?不能删除", role.getRoleName()));
        }
        return roleMapper.deleteRoleByIds(roleIds);
    }

    @Override
    public int deleteAuthUser(SysUserRole userRole) { return userRoleMapper.deleteUserRoleInfo(userRole); }

    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        return userRoleMapper.deleteUserRoleInfos(roleId, userIds);
    }

    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        List<SysUserRole> list = new ArrayList<SysUserRole>();
        for (Long userId : userIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        return userRoleMapper.batchUserRole(list);
    }
}