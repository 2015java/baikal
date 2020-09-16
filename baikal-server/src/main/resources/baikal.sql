# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 192.168.60.11 (MySQL 5.6.35-log)
# Database: baikal
# Generation Time: 2020-09-11 06:03:42 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table tb_baikal_base
# ------------------------------------------------------------

CREATE TABLE `tb_baikal_base` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '名称',
  `app` int(11) NOT NULL COMMENT 'app',
  `scenes` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '场景(多场景以逗号分隔)',
  `status` tinyint(11) NOT NULL DEFAULT '1' COMMENT '1上架0下架',
  `conf_id` bigint(20) DEFAULT NULL,
  `time_type` tinyint(11) DEFAULT '1' COMMENT '1无限制2大于开始时间3小于结束时间4在开始结束之内',
  `start` datetime(3) DEFAULT NULL COMMENT '开始时间',
  `end` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `debug` tinyint(4) NOT NULL DEFAULT '1',
  `priority` bigint(20) NOT NULL DEFAULT '1' COMMENT '优先级',
  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



# Dump of table tb_baikal_conf
# ------------------------------------------------------------

CREATE TABLE `tb_baikal_conf` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `app` int(11) NOT NULL COMMENT 'app',
  `name` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '名称',
  `son_ids` varchar(200) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '子节点ID列表',
  `type` tinyint(4) NOT NULL DEFAULT '6' COMMENT '1关系and2关系or3关系节点all4关系节点any5Flow叶子6Result叶子7None叶子',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '1上架0下架',
  `inverse` tinyint(4) NOT NULL DEFAULT '0' COMMENT '反转-对TRUE和FALSE进行反转 对None节点无效',
  `conf_name` varchar(1000) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '叶子节点-结果类名',
  `conf_field` varchar(5000) COLLATE utf8mb4_bin NOT NULL DEFAULT '' COMMENT '叶子节点-结果类json',
  `forward_id` bigint(20) DEFAULT NULL COMMENT '高耦合过滤树ID',
  `time_type` tinyint(11) NOT NULL DEFAULT '1' COMMENT '1无限制2大于开始时间3小于结束时间4在开始结束之内',
  `start` datetime(3) DEFAULT NULL COMMENT '开始时间',
  `end` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `complex` int(11) NOT NULL DEFAULT '1',
  `debug` tinyint(4) NOT NULL DEFAULT '1' COMMENT 'debug信息 1 true 0 false',
  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_at` datetime(3) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;



# Dump of table tb_baikal_push_history
# ------------------------------------------------------------

CREATE TABLE `tb_baikal_push_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `app` int(11) NOT NULL,
  `baikal_id` bigint(20) DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `push_data` longtext COLLATE utf8mb4_unicode_ci,
  `operator` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
