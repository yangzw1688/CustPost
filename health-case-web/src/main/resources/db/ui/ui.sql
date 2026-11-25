CREATE TABLE IF NOT EXISTS `ui` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `asset_key` VARCHAR(64) NOT NULL,
  `slot` VARCHAR(32) NOT NULL DEFAULT 'home_banner',
  `title` VARCHAR(80) NOT NULL,
  `description` VARCHAR(200) NOT NULL DEFAULT '',
  `link_url` VARCHAR(512) NOT NULL DEFAULT '',
  `tone` VARCHAR(16) NOT NULL DEFAULT '',
  `object_key` VARCHAR(255) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ui_asset_key` (`asset_key`),
  KEY `idx_ui_slot_enabled_sort` (`slot`, `enabled`, `sort_order`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `ui` (`asset_key`, `slot`, `title`, `description`, `link_url`, `tone`, `sort_order`)
VALUES ('home-health', 'home_banner', '健康服务', '便捷、专业的健康服务', '/submit', '', 1);
INSERT IGNORE INTO `ui` (`asset_key`, `slot`, `title`, `description`, `link_url`, `tone`, `sort_order`)
VALUES ('home-claim', 'home_banner', '理赔服务', '快速提交资料，查询服务进度', '/service-record', 'blue', 2);
INSERT IGNORE INTO `ui` (`asset_key`, `slot`, `title`, `description`, `link_url`, `tone`, `sort_order`)
VALUES ('home-support', 'home_banner', '联系客服', '在线咨询，获取专业服务帮助', 'https://work.weixin.qq.com/kfid/kfcbca36d8d108da2db', 'green', 3);
