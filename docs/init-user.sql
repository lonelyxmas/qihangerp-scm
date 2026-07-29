-- 用户信息表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `user_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id`       bigint                DEFAULT NULL COMMENT '部门ID',
  `user_name`     varchar(30)  NOT NULL COMMENT '用户账号',
  `nick_name`     varchar(30)  NOT NULL COMMENT '用户昵称',
  `user_type`     varchar(2)   NOT NULL DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email`         varchar(50)           DEFAULT '' COMMENT '用户邮箱',
  `phonenumber`   varchar(11)           DEFAULT '' COMMENT '手机号码',
  `sex`           char(1)               DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar`        varchar(100)          DEFAULT '' COMMENT '头像地址',
  `password`      varchar(100)          DEFAULT '' COMMENT '密码',
  `status`        char(1)               DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag`      char(1)               DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `login_ip`      varchar(128)          DEFAULT '' COMMENT '最后登录IP',
  `login_date`    datetime              DEFAULT NULL COMMENT '最后登录时间',
  `create_by`     varchar(64)           DEFAULT '' COMMENT '创建者',
  `create_time`   datetime              DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)           DEFAULT '' COMMENT '更新者',
  `update_time`   datetime              DEFAULT NULL COMMENT '更新时间',
  `remark`        varchar(500)          DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 角色信息表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `role_id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`           varchar(30)  NOT NULL COMMENT '角色名称',
  `role_key`            varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort`           int          NOT NULL COMMENT '显示顺序',
  `data_scope`          char(1)      DEFAULT '1' COMMENT '数据范围（1全部 2自定义 3本部门 4本部门及以下）',
  `menu_check_strictly` tinyint(1)   DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1)   DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status`              char(1)      NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag`            char(1)      DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`         datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`         datetime     DEFAULT NULL COMMENT '更新时间',
  `remark`              varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- 用户和角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 应用配置表（key-value 存储，替代 config.json）
CREATE TABLE IF NOT EXISTS `app_config` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   varchar(128) NOT NULL COMMENT '配置键',
  `config_value` text                  DEFAULT NULL COMMENT '配置值（JSON格式）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用配置表';

-- 初始化数据：默认管理员用户 (密码: admin123)
INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(1, 103, 'admin', '管理员', '00', 'admin@qihang.com', '13888888888', '0', '', '$2a$10$a.pN.GYB/iFPxBSwsAwQIuRt8Wpk5hhX010x31zAostyzsjx7ZANS', '0', '0', '127.0.0.1', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', '管理员');

-- 初始化数据：默认角色
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', '超级管理员'),
(2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', '普通角色');

-- 初始化数据：默认用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);