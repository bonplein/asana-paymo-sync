-- Database Schema for the Asana-Paymo-Sync Service
--

DROP TABLE IF EXISTS `mappings`;

CREATE TABLE `mappings` (
  `asana` BIGINT(20) UNSIGNED NOT NULL,
  `paymo` BIGINT(20) UNSIGNED NOT NULL,
  CONSTRAINT unique_ids UNIQUE (`asana`,`paymo`)
) ENGINE=InnoDB;
