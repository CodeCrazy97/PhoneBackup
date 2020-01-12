-- --------------------------------------------------------
-- Host:                         localhost
-- Server version:               10.3.16-MariaDB - mariadb.org binary distribution
-- Server OS:                    Win64
-- HeidiSQL Version:             10.2.0.5599
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- Dumping database structure for phone_backup
CREATE DATABASE IF NOT EXISTS `phone_backup` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `phone_backup`;

-- Dumping structure for table phone_backup.contacts
CREATE TABLE IF NOT EXISTS `contacts` (
  `person_name` varchar(30) NOT NULL,
  `phone_number` bigint(20) NOT NULL,
  PRIMARY KEY (`phone_number`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.

-- Dumping structure for table phone_backup.last_backup_timestamps
CREATE TABLE IF NOT EXISTS `last_backup_timestamps` (
  `backup_name` varchar(30) NOT NULL,
  `backup_timestamp` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE current_timestamp(),
  PRIMARY KEY (`backup_name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.

-- Dumping structure for table phone_backup.phone_calls
CREATE TABLE IF NOT EXISTS `phone_calls` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contact_phone_number` bigint(20) NOT NULL,
  `duration` int(5) NOT NULL,
  `call_timestamp` timestamp NULL DEFAULT NULL,
  `call_type` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `phone_calls_fk2` (`contact_phone_number`),
  CONSTRAINT `phone_calls_fk2` FOREIGN KEY (`contact_phone_number`) REFERENCES `contacts` (`phone_number`)
) ENGINE=InnoDB AUTO_INCREMENT=3017 DEFAULT CHARSET=latin1;

-- Data exporting was unselected.

-- Dumping structure for table phone_backup.text_messages
CREATE TABLE IF NOT EXISTS `text_messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `msg_text` text NOT NULL,
  `sender_phone_number` bigint(20) NOT NULL,
  `msg_timestamp` timestamp NULL DEFAULT NULL,
  `text_only` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `text_messages_fk1` (`sender_phone_number`),
  CONSTRAINT `text_messages_fk1` FOREIGN KEY (`sender_phone_number`) REFERENCES `contacts` (`phone_number`)
) ENGINE=InnoDB AUTO_INCREMENT=58301 DEFAULT CHARSET=latin1 MAX_ROWS=1000000;

-- Data exporting was unselected.

-- Dumping structure for table phone_backup.text_message_recipients
CREATE TABLE IF NOT EXISTS `text_message_recipients` (
  `contact_phone_number` bigint(20) NOT NULL,
  `text_message_id` int(11) NOT NULL,
  KEY `contact_fk` (`contact_phone_number`),
  KEY `text_message_fk` (`text_message_id`),
  CONSTRAINT `contact_fk` FOREIGN KEY (`contact_phone_number`) REFERENCES `contacts` (`phone_number`),
  CONSTRAINT `text_message_fk` FOREIGN KEY (`text_message_id`) REFERENCES `text_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- Data exporting was unselected.

/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
