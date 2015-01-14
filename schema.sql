-- Database Schema for the Asana-Paymo-Sync Service
--

DROP TABLE IF EXISTS `mappings`;

CREATE TABLE `mappings` (
  `asana` int(20) NOT NULL,
  `paymo` int(20) NOT NULL,
  CONSTRAINT unique_ids UNIQUE (`asana`,`paymo`)
) ENGINE=InnoDB;
