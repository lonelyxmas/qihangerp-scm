-- Move Feishu config from config.json to app_config table
-- Run this if you have existing Feishu config in config.json
-- After running, the system will read Feishu config from app_config instead of config.json

INSERT IGNORE INTO `app_config` (`config_key`, `config_value`) VALUES
('feishu.webhookUrl', ''),
('feishu.appId', ''),
('feishu.appSecret', ''),
('feishu.chatId', '');
