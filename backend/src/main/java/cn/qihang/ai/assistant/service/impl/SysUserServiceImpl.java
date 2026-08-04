package cn.qihang.ai.assistant.service.impl;

import cn.qihang.ai.assistant.common.ServiceException;
import cn.qihang.ai.assistant.common.constant.UserConstants;
import cn.qihang.ai.assistant.common.utils.StringUtils;
import cn.qihang.ai.assistant.entity.SysRole;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.entity.SysUserRole;
import cn.qihang.ai.assistant.mapper.SysRoleMapper;
import cn.qihang.ai.assistant.mapper.SysUserMapper;
import cn.qihang.ai.assistant.mapper.SysUserRoleMapper;
import cn.qihang.ai.assistant.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl implements ISysUserService {
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Override
    public List<SysUser> selectUserList(SysUser user) { return userMapper.selectUserList(user); }

    @Override
    public List<SysUser> selectAllocatedList(SysUser user) { return userMapper.selectAllocatedList(user); }

    @Override
    public List<SysUser> selectUnallocatedList(SysUser user) { return userMapper.selectUnallocatedList(user); }

    @Override
    public SysUser selectUserByUserName(String userName) { return userMapper.selectUserByUserName(userName); }

    @Override
    public SysUser selectUserById(Long userId) { return userMapper.selectUserById(userId); }

    @Override
    public boolean checkUserNameUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkUserNameUnique(user.getUserName());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) return UserConstants.NOT_UNIQUE;
        return UserConstants.UNIQUE;
    }

    @Override
    public boolean checkPhoneUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) return UserConstants.NOT_UNIQUE;
        return UserConstants.UNIQUE;
    }

    @Override
    public boolean checkEmailUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) return UserConstants.NOT_UNIQUE;
        return UserConstants.UNIQUE;
    }

    @Override
    public void checkUserAllowed(SysUser user) {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin())
            throw new ServiceException("不允许操作超级管理员用户");
    }

    @Override
    @Transactional
    public int insertUser(SysUser user) {
        if (user.getUserType() == null || user.getUserType().isEmpty()) user.setUserType("00");
        if (user.getDeptId() == null) user.setDeptId(0L);
        int rows = userMapper.insertUser(user);
        insertUserRole(user);
        return rows;
    }

    public boolean registerUser(SysUser user) {
        if (user.getUserType() == null || user.getUserType().isEmpty()) user.setUserType("00");
        if (user.getDeptId() == null) user.setDeptId(0L);
        return userMapper.insertUser(user) > 0;
    }

    @Override
    @Transactional
    public int updateUser(SysUser user) {
        Long userId = user.getUserId();
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(user);
        return userMapper.updateUser(user);
    }

    @Override
    public int updateUserStatus(SysUser user) { return userMapper.updateUser(user); }

    @Override
    public int updateUserProfile(SysUser user) { return userMapper.updateUser(user); }

    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return userMapper.updateUserAvatar(userName, avatar) > 0;
    }

    @Override
    public int resetPwd(SysUser user) { return userMapper.updateUser(user); }

    @Override
    public int resetUserPwd(String userName, String password) {
        return userMapper.resetUserPwd(userName, password);
    }

    @Override
    @Transactional
    public int deleteUserById(Long userId) {
        userRoleMapper.deleteUserRoleByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    @Override
    public void checkUserDataScope(Long userId) {
        if (!SysUser.isAdmin(userId)) {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = selectUserList(user);
            if (StringUtils.isEmpty(users)) throw new ServiceException("没有权限访问用户数据!");
        }
    }

    @Override
    public String selectUserRoleGroup(String userName) {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list)) return StringUtils.EMPTY;
        return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
    }

    @Override
    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds) {
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    public void insertUserRole(SysUser user) { this.insertUserRole(user.getUserId(), user.getRoleIds()); }

    public void insertUserRole(Long userId, Long[] roleIds) {
        if (StringUtils.isNotEmpty(roleIds)) {
            List<SysUserRole> list = new ArrayList<SysUserRole>(roleIds.length);
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.batchUserRole(list);
        }
    }
}