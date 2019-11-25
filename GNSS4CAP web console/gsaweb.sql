-- phpMyAdmin SQL Dump
-- version 4.7.6
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Versione del server: 5.7.20
-- Versione PHP: 5.6.32

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `gsaweb`
--
CREATE DATABASE IF NOT EXISTS `gsaweb` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;
USE `gsaweb`;

-- --------------------------------------------------------

--
-- Struttura della tabella `foto_cell_info`
--

CREATE TABLE `foto_cell_info` (
  `id` int(11) NOT NULL,
  `id_foto` int(11) NOT NULL,
  `resp` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `foto_gsa`
--

CREATE TABLE `foto_gsa` (
  `id` int(11) NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `delete_date` timestamp NULL DEFAULT NULL,
  `json` longtext CHARACTER SET latin1 NOT NULL,
  `status` text CHARACTER SET latin1
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `logins_activity`
--

CREATE TABLE `logins_activity` (
  `id` int(11) NOT NULL,
  `user` varchar(32) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `token` varchar(64) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `valid` tinyint(1) NOT NULL,
  `loggedout` tinyint(1) NOT NULL DEFAULT '0',
  `from_app` tinyint(4) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `past_logins_activity`
--

CREATE TABLE `past_logins_activity` (
  `id` int(11) NOT NULL,
  `user` varchar(32) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `token` varchar(64) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `valid` tinyint(1) NOT NULL,
  `loggedout` tinyint(1) NOT NULL DEFAULT '0',
  `from_app` tinyint(4) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Struttura della tabella `roles`
--

CREATE TABLE `roles` (
  `id_role` int(11) NOT NULL,
  `ruolo` varchar(2000) CHARACTER SET latin1 NOT NULL,
  `role_desc` varchar(50) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `data_inizio` datetime NOT NULL,
  `data_fine` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dump dei dati per la tabella `roles`
--

INSERT INTO `roles` (`id_role`, `ruolo`, `role_desc`, `data_inizio`, `data_fine`) VALUES
(1, 'SUPERUSER', 'Super user', '2010-01-01 00:00:00', NULL),
(2, 'PAYING_AGENCY', 'Paying Agency', '2000-01-01 00:00:00', NULL),
(3, 'USER', 'User', '2000-01-01 00:00:00', NULL);

-- --------------------------------------------------------

--
-- Struttura della tabella `users`
--

CREATE TABLE `users` (
  `id_user` int(11) NOT NULL,
  `user` varchar(2000) CHARACTER SET latin1 COLLATE latin1_general_cs NOT NULL,
  `pwd` varchar(2000) CHARACTER SET latin1 NOT NULL,
  `data_inizio` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `data_fine` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Dump dei dati per la tabella `users`
--

INSERT INTO `users` (`id_user`, `user`, `pwd`, `data_inizio`, `data_fine`) VALUES
(1, 'admin', '*4ACFE3202A5FF5CF467898FC58AAB1D615029441', '2019-11-18 20:29:08', NULL);

-- --------------------------------------------------------

--
-- Struttura della tabella `user_affiliation`
--

CREATE TABLE `user_affiliation` (
  `id_user` int(11) NOT NULL,
  `id_superuser` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Struttura della tabella `user_roles`
--

CREATE TABLE `user_roles` (
  `id_user_role` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `id_role` int(11) NOT NULL,
  `data_inizio` datetime NOT NULL,
  `data_fine` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dump dei dati per la tabella `user_roles`
--

INSERT INTO `user_roles` (`id_user_role`, `id_user`, `id_role`, `data_inizio`, `data_fine`) VALUES
(1, 1, 1, '2019-01-01 00:00:00', NULL);

--
-- Indici per le tabelle scaricate
--

--
-- Indici per le tabelle `foto_cell_info`
--
ALTER TABLE `foto_cell_info`
  ADD PRIMARY KEY (`id`);

--
-- Indici per le tabelle `foto_gsa`
--
ALTER TABLE `foto_gsa`
  ADD PRIMARY KEY (`id`),
  ADD KEY `closed_idx` (`delete_date`);

--
-- Indici per le tabelle `logins_activity`
--
ALTER TABLE `logins_activity`
  ADD PRIMARY KEY (`id`),
  ADD KEY `tokenidx` (`token`),
  ADD KEY `lastloginidx` (`user`,`token`,`valid`,`loggedout`);

--
-- Indici per le tabelle `past_logins_activity`
--
ALTER TABLE `past_logins_activity`
  ADD PRIMARY KEY (`id`),
  ADD KEY `tokenidx` (`token`),
  ADD KEY `lastloginidx` (`user`,`token`,`valid`,`loggedout`);

--
-- Indici per le tabelle `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`id_role`);

--
-- Indici per le tabelle `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id_user`),
  ADD UNIQUE KEY `uniqueusername` (`user`);

--
-- Indici per le tabelle `user_roles`
--
ALTER TABLE `user_roles`
  ADD PRIMARY KEY (`id_user_role`);

--
-- AUTO_INCREMENT per le tabelle scaricate
--

--
-- AUTO_INCREMENT per la tabella `foto_cell_info`
--
ALTER TABLE `foto_cell_info`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=106;

--
-- AUTO_INCREMENT per la tabella `foto_gsa`
--
ALTER TABLE `foto_gsa`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=563;

--
-- AUTO_INCREMENT per la tabella `logins_activity`
--
ALTER TABLE `logins_activity`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1305;

--
-- AUTO_INCREMENT per la tabella `past_logins_activity`
--
ALTER TABLE `past_logins_activity`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1286;

--
-- AUTO_INCREMENT per la tabella `roles`
--
ALTER TABLE `roles`
  MODIFY `id_role` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT per la tabella `users`
--
ALTER TABLE `users`
  MODIFY `id_user` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT per la tabella `user_roles`
--
ALTER TABLE `user_roles`
  MODIFY `id_user_role` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
