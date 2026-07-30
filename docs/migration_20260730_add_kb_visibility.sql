ALTER TABLE kb_bases ADD COLUMN visibility VARCHAR(20) DEFAULT 'private' COMMENT '可见性: public=公开, private=私有';
