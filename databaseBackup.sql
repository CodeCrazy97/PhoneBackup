-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               10.1.30-MariaDB - mariadb.org binary distribution
-- Server OS:                    Win32
-- HeidiSQL Version:             9.5.0.5196
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


-- Dumping database structure for sms
CREATE DATABASE IF NOT EXISTS `sms` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `sms`;

-- Dumping structure for table sms.contacts
CREATE TABLE IF NOT EXISTS `contacts` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  `phone_number` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=latin1;

-- Data exporting was unselected.
-- Dumping structure for table sms.messages
CREATE TABLE IF NOT EXISTS `messages` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `message_text` text NOT NULL,
  `incoming` tinyint(1) NOT NULL,
  `contact` int(11) NOT NULL,
  `sent_datetime` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `messages_fk1` (`contact`),
  CONSTRAINT `messages_fk1` FOREIGN KEY (`contact`) REFERENCES `contacts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21518 DEFAULT CHARSET=latin1 MAX_ROWS=1000000;

-- Data exporting was unselected.
-- Dumping structure for table sms.phonecalls
CREATE TABLE IF NOT EXISTS `phonecalls` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `contactname` varchar(30) NOT NULL,
  `calldate` varchar(50) NOT NULL,
  `duration` int(5) NOT NULL,
  `incoming` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=489 DEFAULT CHARSET=latin1;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
