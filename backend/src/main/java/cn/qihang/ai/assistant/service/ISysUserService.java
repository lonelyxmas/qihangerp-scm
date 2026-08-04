package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.SysUser;

import java.util.List;

public interface ISysUserService {
    List<SysUser> selectUserList(SysUser user);
    List<SysUser> selectAllocatedList(SysUser user);
    List<SysUser> selectUnallocatedList(SysUser user);
    SysUser selectUserByUserName(String userName);
    SysUser selectUserById(Long userId);
    boolean checkUserNameUnique(SysUser user);
    boolean checkPhoneUnique(SysUser user);
    boolean checkEmailUnique(SysUser user);
    void checkUserAllowed(SysUser user);
    int insertUser(SysUser user);
    int updateUser(SysUser user);
    int updateUserStatus(SysUser user);
    int updateUserProfile(SysUser user);
    boolean updateUserAvatar(String userName, String avatar);
    int resetPwd(SysUser user);
    int resetUserPwd(String userName, String password);
    int deleteUserById(Long userId);
    void checkUserDataScope(Long userId);
    String selectUserRoleGroup(String userName);
    void insertUserAuth(Long userId, Long[] roleIds);
}