package cn.qihang.ai.assistant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private boolean menuCheckStrictly;
    private boolean deptCheckStrictly;
    private String status;
    private String delFlag;
    private boolean flag = false;
    private Long[] menuIds;
    private Long[] deptIds;
    private Set<String> permissions;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String remark;
    private Map<String, Object> params;

    @JsonIgnore
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public SysRole() {}
    public SysRole(Long roleId) { this.roleId = roleId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    @JsonIgnore
    public boolean isAdmin() { return isAdmin(this.roleId); }
    public static boolean isAdmin(Long roleId) { return roleId != null && 1L == roleId; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleKey() { return roleKey; }
    public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
    public Integer getRoleSort() { return roleSort; }
    public void setRoleSort(Integer roleSort) { this.roleSort = roleSort; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public boolean isMenuCheckStrictly() { return menuCheckStrictly; }
    public void setMenuCheckStrictly(boolean menuCheckStrictly) { this.menuCheckStrictly = menuCheckStrictly; }
    public boolean isDeptCheckStrictly() { return deptCheckStrictly; }
    public void setDeptCheckStrictly(boolean deptCheckStrictly) { this.deptCheckStrictly = deptCheckStrictly; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
    public boolean isFlag() { return flag; }
    public void setFlag(boolean flag) { this.flag = flag; }
    public Long[] getMenuIds() { return menuIds; }
    public void setMenuIds(Long[] menuIds) { this.menuIds = menuIds; }
    public Long[] getDeptIds() { return deptIds; }
    public void setDeptIds(Long[] deptIds) { this.deptIds = deptIds; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}