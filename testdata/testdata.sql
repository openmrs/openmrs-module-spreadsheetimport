-- MySQL dump 10.13  Distrib 5.1.41, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: openmrs
-- ------------------------------------------------------
-- Server version	5.1.41-3ubuntu12.6

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `spreadsheetimport_template`
--

DROP TABLE IF EXISTS `spreadsheetimport_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spreadsheetimport_template` (
  `id` int(32) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` varchar(1000) NOT NULL,
  `creator` int(11) NOT NULL DEFAULT '0',
  `date_created` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `changed_by` int(11) DEFAULT NULL,
  `date_changed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `User who wrote this template` (`creator`),
  KEY `User who changed this template` (`changed_by`),
  CONSTRAINT `User who wrote this template` FOREIGN KEY (`creator`) REFERENCES `users` (`user_id`),
  CONSTRAINT `User who changed this template` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spreadsheetimport_template`
--

LOCK TABLES `spreadsheetimport_template` WRITE;
/*!40000 ALTER TABLE `spreadsheetimport_template` DISABLE KEYS */;
INSERT INTO `spreadsheetimport_template` VALUES (3,'testdata','testdata',1,'2010-09-08 14:08:18',1,'2010-09-08 14:08:18');
/*!40000 ALTER TABLE `spreadsheetimport_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spreadsheetimport_template_column`
--

DROP TABLE IF EXISTS `spreadsheetimport_template_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spreadsheetimport_template_column` (
  `id` int(32) NOT NULL AUTO_INCREMENT,
  `template_id` int(32) NOT NULL,
  `name` varchar(100) NOT NULL,
  `database_table_dot_column` varchar(1000) NOT NULL,
  `database_table_dataset_index` int(11) DEFAULT NULL,
  `column_import_index` int(32) NOT NULL,
  `disallow_duplicate_value` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `Template to which this column belongs` (`template_id`),
  CONSTRAINT `Template to which this column belongs` FOREIGN KEY (`template_id`) REFERENCES `spreadsheetimport_template` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spreadsheetimport_template_column`
--

LOCK TABLES `spreadsheetimport_template_column` WRITE;
/*!40000 ALTER TABLE `spreadsheetimport_template_column` DISABLE KEYS */;
INSERT INTO `spreadsheetimport_template_column` VALUES (3,3,'Given Name','person_name.given_name',NULL,7,0),(4,3,'Middle Name','person_name.middle_name',NULL,8,0),(5,3,'Family Name','person_name.family_name',NULL,9,0),(6,3,'Gender','person.gender',NULL,0,0),(7,3,'Birthdate','person.birthdate',NULL,1,0),(8,3,'Identifier','patient_identifier.identifier',NULL,6,1),(9,3,'Weight 1','obs.value_numeric',0,2,0),(10,3,'Date 1','obs.obs_datetime',0,3,0),(11,3,'Weight 2','obs.value_numeric',1,4,0),(12,3,'Date 2','obs.obs_datetime',1,5,0);
/*!40000 ALTER TABLE `spreadsheetimport_template_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spreadsheetimport_template_column_column`
--

DROP TABLE IF EXISTS `spreadsheetimport_template_column_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spreadsheetimport_template_column_column` (
  `id` int(32) NOT NULL AUTO_INCREMENT,
  `template_column_id_import_first` int(32) NOT NULL,
  `template_column_id_import_next` int(32) NOT NULL,
  `foreign_key_column_name` varchar(1000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Template column which must be imported first` (`template_column_id_import_first`),
  KEY `Template column which must be imported next` (`template_column_id_import_next`),
  CONSTRAINT `Template column which must be imported first` FOREIGN KEY (`template_column_id_import_first`) REFERENCES `spreadsheetimport_template_column` (`id`),
  CONSTRAINT `Template column which must be imported next` FOREIGN KEY (`template_column_id_import_next`) REFERENCES `spreadsheetimport_template_column` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spreadsheetimport_template_column_column`
--

LOCK TABLES `spreadsheetimport_template_column_column` WRITE;
/*!40000 ALTER TABLE `spreadsheetimport_template_column_column` DISABLE KEYS */;
INSERT INTO `spreadsheetimport_template_column_column` VALUES (42,6,3,'person_id'),(43,6,4,'person_id'),(44,6,5,'person_id'),(45,6,8,'patient_id'),(46,6,9,'person_id'),(47,6,10,'person_id'),(48,6,11,'person_id'),(49,6,12,'person_id');
/*!40000 ALTER TABLE `spreadsheetimport_template_column_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spreadsheetimport_template_column_prespecified_value`
--

DROP TABLE IF EXISTS `spreadsheetimport_template_column_prespecified_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spreadsheetimport_template_column_prespecified_value` (
  `id` int(32) NOT NULL AUTO_INCREMENT,
  `template_column_id` int(32) NOT NULL,
  `template_prespecified_value_id` int(32) NOT NULL,
  `foreign_key_column_name` varchar(1000) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Template column which is being mapped to a pre-specified value` (`template_column_id`),
  KEY `Pre-specified value which is being mapped to a template column` (`template_prespecified_value_id`),
  CONSTRAINT `Template column which is being mapped to a pre-specified value` FOREIGN KEY (`template_column_id`) REFERENCES `spreadsheetimport_template_column` (`id`),
  CONSTRAINT `Pre-specifived value which is being mapped to a template column` FOREIGN KEY (`template_prespecified_value_id`) REFERENCES `spreadsheetimport_template_prespecified_value` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spreadsheetimport_template_column_prespecified_value`
--

LOCK TABLES `spreadsheetimport_template_column_prespecified_value` WRITE;
/*!40000 ALTER TABLE `spreadsheetimport_template_column_prespecified_value` DISABLE KEYS */;
INSERT INTO `spreadsheetimport_template_column_prespecified_value` VALUES (44,9,37,'concept_id'),(45,10,37,'concept_id'),(46,11,38,'concept_id'),(47,12,38,'concept_id'),(48,8,39,'identifier_type'),(49,8,40,'location_id');
/*!40000 ALTER TABLE `spreadsheetimport_template_column_prespecified_value` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `spreadsheetimport_template_prespecified_value`
--

DROP TABLE IF EXISTS `spreadsheetimport_template_prespecified_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
# noinspection SqlNoDataSourceInspection

CREATE TABLE `spreadsheetimport_template_prespecified_value` (
  `id` int(32) NOT NULL AUTO_INCREMENT,
  `template_id` int(32) NOT NULL,
  `database_table_dot_column` varchar(1000) NOT NULL,
  `value` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Template to which this pre-specified value belongs` (`template_id`),
  CONSTRAINT `Template to which this pre-specified value belongs` FOREIGN KEY (`template_id`) REFERENCES `spreadsheetimport_template` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `spreadsheetimport_template_prespecified_value`
--

LOCK TABLES `spreadsheetimport_template_prespecified_value` WRITE;
/*!40000 ALTER TABLE `spreadsheetimport_template_prespecified_value` DISABLE KEYS */;
INSERT INTO `spreadsheetimport_template_prespecified_value` VALUES (37,3,'concept.concept_id','5089'),(38,3,'concept.concept_id','5089'),(39,3,'patient_identifier_type.patient_identifier_type_id','2'),(40,3,'location.location_id','1');
/*!40000 ALTER TABLE `spreadsheetimport_template_prespecified_value` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2010-09-08 15:55:16
