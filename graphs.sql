-- phpMyAdmin SQL Dump
-- version 3.5.1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Apr 19, 2013 at 02:11 PM
-- Server version: 5.5.24-log
-- PHP Version: 5.3.13

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `lodmilla`
--

-- --------------------------------------------------------

--
-- Table structure for table `graphs`
--

DROP TABLE IF EXISTS `graphs`;
CREATE TABLE IF NOT EXISTS `graphs` (
  `graph_id` varchar(255) NOT NULL,
  `graph_name` varchar(255) NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `graph` longblob NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
