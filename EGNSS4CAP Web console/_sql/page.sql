CREATE TABLE `egnss4cap`.`page` (
  `id` BIGINT(20) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  PRIMARY KEY (`id`));

INSERT INTO page (id, name) VALUES ("1","login");
INSERT INTO page (id, name) VALUES ("2","list farmářů");
INSERT INTO page (id, name) VALUES ("3","seznam úkolů");
INSERT INTO page (id, name) VALUES ("4","popup mapy");
INSERT INTO page (id, name) VALUES ("5","detail úkolu");
INSERT INTO page (id, name) VALUES ("6","base šablona");
INSERT INTO `page` (`id`, `name`, `timestamp`) VALUES ('7', 'galerie neprirazenych fotek');
INSERT INTO `page` (`id`, `name`, `timestamp`) VALUES ('8', 'cesty');
INSERT INTO `page` (`id`, `name`, `timestamp`) VALUES ('9', 'správa agentur', CURRENT_TIMESTAMP);
INSERT INTO `page` (`id`, `name`, `timestamp`) VALUES ('10', 'správa PA officer', CURRENT_TIMESTAMP);
INSERT INTO `page` (`id`, `name`, `timestamp`) VALUES ('11', 'release notes', CURRENT_TIMESTAMP);

INSERT INTO `page` (`id`, `name`, `timestamp`) VALUES ('91', 'PHOTO_PDF_EXPORT', CURRENT_TIMESTAMP);

INSERT INTO `pa_flag` (`id`, `flag`, `timestamp`) VALUES ('3', 'reopened photo', CURRENT_DATE());
