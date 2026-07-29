-- 数据集表追加协作配置字段
ALTER TABLE data_center_datasets
ADD COLUMN collab_config_json JSON DEFAULT NULL COMMENT '协作配置JSON' AFTER import_configs_json;

-- 记录表追加协作字段
ALTER TABLE data_center_records
ADD COLUMN assigned_to BIGINT DEFAULT NULL COMMENT '负责人用户ID' AFTER record_status;

ALTER TABLE data_center_records
ADD COLUMN assigned_at VARCHAR(32) DEFAULT NULL COMMENT '指派时间' AFTER assigned_to;

ALTER TABLE data_center_records
ADD COLUMN approval_status VARCHAR(32) DEFAULT 'none' COMMENT '审批状态: none/pending/approved/rejected' AFTER assigned_at;

ALTER TABLE data_center_records
ADD COLUMN approved_by BIGINT DEFAULT NULL COMMENT '审批人用户ID' AFTER approval_status;

ALTER TABLE data_center_records
ADD COLUMN approved_at VARCHAR(32) DEFAULT NULL COMMENT '审批时间' AFTER approved_by;