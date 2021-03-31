CREATE TABLE `egnss4cap`.`page_lang` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `page_id` BIGINT(20) NOT NULL,
  `description` VARCHAR(255) NULL,
  `template_param` VARCHAR(45) NOT NULL,
  `cz` VARCHAR(4000) NULL,
  `en` VARCHAR(4000) NULL,
  `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
  PRIMARY KEY (`id`),
  INDEX `page_id_idx` (`page_id` ASC) VISIBLE,
  CONSTRAINT `page_id`
    FOREIGN KEY (`page_id`)
    REFERENCES `page` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE);

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (1, "login titulek", "title", "Přihlášení", "Login");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (1, "egnss login", "heading", "EGNSS4CAP PŘIHLÁŠENÍ", "EGNSS4CAP LOGIN");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (1, "user name", "user_name", "Uživatelské jméno:", "User name:");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (1, "heslo", "password", "Heslo:", "Password:");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (1, "enter tlačítko", "enter", "Přihlásit", "Enter");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (1, "špatné heslo", "wrong_login", "Špatné přihlašovací údaje.", "Login credentials don't match.");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "farmáři titulek", "title", "List farmářů", "Farmers list");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "nadpis", "heading", "Správa uživatelů", "User management");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "tlačítko nový farmář", "new_farmer", "Přidat farmáře", "Add new farmer");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "login", "login", "login", "login");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "heslo", "password", "Heslo", "Password");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "jméno", "name", "Jméno", "Name");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "příjmení", "surname", "Příjmení", "Surname");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "identifikační číslo", "ji", "JI", "Identification number");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "e-mail", "email", "E-mail", "E-mail");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "vat", "vat", "Vat", "Vat");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "tlačítko uložit", "save", "Uložit", "Save");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "id", "id", "ID", "ID");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "úkoly titulek", "title", "Seznam úkolů", "Task list");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "úkoly", "task_s", "úkoly", "tasks");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "úkoly", "task_l", "Úkoly", "Tasks");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "nový úkol button", "new_task_btn", "Přidat nový", "Add new task");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "status", "status", "Status", "Status");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "jmeno", "name", "Název", "Name");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "popis", "description", "Popis", "Description");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "poznámka", "note", "Poznámka", "Note");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "do data", "due_date", "Datum do splnění", "Due date");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "ulož button", "save", "Uložit", "Save");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "počet fotek", "photos", "Počet fotek", "Photos taken");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "datum vytvoření", "date_created", "Datum vytvoření", "Date created");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "přijetí", "accept", "Stav přijetí", "Acceptation");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "schváleno", "ack", "Schváleno", "Accepted");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "odmítnoto", "decline", "Zamítnuto", "Declined");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "čeká", "wait", "Čeká", "Waiting");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (4, "počet fotek", "count", "Počet fotek:", "Photos count:");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "detail úkolu", "title", "Detail úkolu", "Task detail");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "nadpis", "header", "detail úkolu", "task detail");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "status", "status", "Status", "Status");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "jméno", "name", "Název", "Name");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "popis", "description", "Popis", "Description");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "vytvořeno", "created", "Vytvořeno", "Date created");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "do data", "due_date", "Datum do splnění", "Due date");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "akce", "action", "Akce", "Actions");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "schváleno", "ack", "Schváleno", "Accepted");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "odmítnoto", "decline", "Zamítnuto", "Declined");


INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (6, "domů", "home", "Domů", "Home");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (6, "odhlásit", "logout", "Odhlásit", "Logout");

INSERT INTO `egnss4cap`.`page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '3', 'zpět', 'back', 'zpět', 'back', CURRENT_TIMESTAMP), (NULL, '5', 'zpět', 'back', 'zpět', 'back', CURRENT_TIMESTAMP);
INSERT INTO `egnss4cap`.`page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '2', NULL, 'new_farmer_header', 'Nový farmář', 'New farmer', CURRENT_TIMESTAMP), (NULL, '3', NULL, 'new_task_header', 'Nový úkol', 'New task', CURRENT_TIMESTAMP)
INSERT INTO `egnss4cap`.`page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '3', NULL, 'unassigned_photos', 'Zobrazit nepřiřazené fotky', 'Show unassigned photos', CURRENT_TIMESTAMP)
INSERT INTO `egnss4cap`.`page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '7', NULL, 'heading', 'Galerie nepřiřazených fotek', 'Gallery of unassigned photos', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '7', 'zpět', 'back', 'zpět', 'back', '2020-06-16 13:23:11');
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '7', 'vybrat task', 'choose_task_button', 'Vybrat úkol', 'Choose task', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '7', 'přiřadit k tasku', 'assign_photos_button', 'Přiřadit', 'Assign', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, 'detail fotky meta', 'photo_meta_head', 'Metadata fotky', 'Photos metadata', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_lat', 'Zeměpisná šířka', 'Latitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_lng', 'Zeměpisná délka', 'Longitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_altitude', 'Nadmořská výška', 'Altitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_bearing', 'Bearing', 'Bearing', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_azimuth', 'Azimuth', 'Azimut', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_roll', 'Roll', 'Roll', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_pitch', 'Pitch', 'Pitch', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_orientation', 'Orientace', 'Orientation', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_hvangle', 'Horizontální úhel pohledu', 'Horizontal view angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_vvangle', 'Vertikální úhel pohledu', 'Vertical view angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_accuracy', 'Přesnost', 'Accuracy', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_device', 'Zařízení', 'Device', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_satsinfo', 'Satelitní info', 'Satellite info', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_nmea', 'NMEA', 'NMEA', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_network', 'Síť', 'Network', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_distance', 'Vzdálenost', 'Distance', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_timestamp', 'Časové razítko', 'Timestamp', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_yes', 'ANO', 'YES', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_no', 'NE', 'NO', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_note', 'Poznámka', 'Note', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, 'detail fotky meta', 'photo_meta_head', 'Metadata fotky', 'Photos metadata', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_lat', 'Zeměpisná šířka', 'Latitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_lng', 'Zeměpisná délka', 'Longitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_altitude', 'Nadmořská výška', 'Altitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_bearing', 'Bearing', 'Bearing', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_azimuth', 'Azimuth', 'Azimut', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_roll', 'Roll', 'Roll', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_pitch', 'Pitch', 'Pitch', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_orientation', 'Orientace', 'Orientation', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_hvangle', 'Horizontální úhel pohledu', 'Horizontal view angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_vvangle', 'Vertikální úhel pohledu', 'Vertical view angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_accuracy', 'Přesnost', 'Accuracy', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_device', 'Zařízení', 'Device', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_satsinfo', 'Satelitní info', 'Satellite info', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_nmea', 'NMEA', 'NMEA', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_network', 'Síť', 'Network', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_distance', 'Vzdálenost', 'Distance', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_timestamp', 'Časové razítko', 'Timestamp', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_yes', 'ANO', 'YES', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_no', 'NE', 'NO', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_note', 'Poznámka', 'Note', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 2, 'tlacitko editovat farmare', 'edit_user', 'Editovat farmáře', 'Edit farmer', CURRENT_TIMESTAMP);
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "akce", "action", "Akce", "Actions");
INSERT INTO `egnss4cap`.`page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '2', NULL, 'edit_farmer_header', 'Editace farmáře', 'Editation of farmer', CURRENT_TIMESTAMP);
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "tlačítko zavřít", "close", "Zavřít", "Close");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "tlačítko reset řazení", "reset_sort", "Zrušit řazení", "Cancel sorting");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (3, "tlačítko reset řazení", "reset_sort", "Zrušit řazení", "Cancel sorting");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "počet úkolů", "tasks_count_text", "Počet úkolů", "Tasks count");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "počet fotek", "photos_count_text", "Počet fotek", "Photos count");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "počet nepřiřazených fotek", "unassigned_photos_count_text", "Nepřiřazené fotky", "Unassigned photos");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_created_date", "Vytvořeno (UTC)", "Created (UTC)");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_created_date", "Vytvořeno (UTC)", "Created (UTC)");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 3, '', 'task_due_date_error', 'Datum do splnění nemůže být v minulosti!', 'Due date has to be in the future!', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_photo_accept_error', 'Úkol nemůže být schválen bez fotografií!', 'Task cannot be accepted without photos!', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_accept_confirm', 'Schválit úkol?', 'Accept task?', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_decline_confirm', 'Zamítnout úkol? Prosím, doplňte důvod zamítnutí.', 'Decline task? Enter reason of decline, please.', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_return_confirm', 'Vrátit úkol farmáři? Prosím, doplňte důvod vrácení.', 'Return task to farmer? Enter reason of reopening, please.', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "poznámka", "note", "Poznámka", "Note");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "vráceno", "returned", "Znovuotevřeno", "Reopened");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "vráceno", "returned", "Znovuotevřeno", "Reopened");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "důvod znovuotevření", "note_returned", "Důvod znovuotevření", "Reopen reason");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_rotate_left", "Otočit vlevo", "Rotate left");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_rotate_right", "Otočit vpravo", "Rotate right");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_rotate_left", "Otočit vlevo", "Rotate left");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_rotate_right", "Otočit vpravo", "Rotate right");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'note_declined', 'Důvod zamítnutí', 'Decline reason', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_delete_confirm', 'Smazat úkol?', 'Delete task?', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_photo_delete_error', 'Není možné úkol smazat.', 'Task cannot be deleted.', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "ucel", "type", "Účel", "Purpose");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "ucel", "type", "Účel", "Purpose");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "group_task_accept", "Hromadné akce", "Bulk actions");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "group_task_accept_button", "Schválit hromadně", "Bulk accept");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "group_task_accept_confirm", "Hromadně schválit vybrané úkoly?", "Bulk accept selected tasks?");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_select", "Vybrat", "Select");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_delete", "Smazat fotku", "Delete photo");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "select_all", "Vybrat vše", "Select all");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "deselect_all", "Zrušit výběr", "Cancel selection");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "photo_delete_confirm", "Smazat fotku?", "Delete photo?");
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_move_from_open_confirm', 'Změnit stav na "Data provided"?', 'Change status to "Data provided"?', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'task_move_from_open_error', 'K úkolu nejsou přiřazeny žádné fotografie!', 'The task has no photos!', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'assign_photos_select_error', 'Není vybrána žádna fotografie!', 'No photo selected!', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'photo_assign_confirm', 'Přiřadit fotografie k vybranému úkolu?', 'Assign photos to selected task?', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'assign_photos_select_error_1', 'Není vybrán žádný úkol!', 'No task selected!', CURRENT_TIMESTAMP);


INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "photo_multi_delete_confirm", "Smazat všechny označené fotky?", "Delete all selected photos?");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_multi_delete", "Smazat označené", "Delete selected");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_new", "Nové", "New");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_open", "Otevřené", "Open");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_provided", "Poskytnuté", "Data provided");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_checked", "Schválené", "Data checked");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_closed", "Uzavřené", "Closed");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_returned", "Vrácené", "Returned");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_flag_accept", "Schválené", "Accepted");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_flag_decline", "Zamítnuté", "Declined");


INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "after_deadline_to_end", "Prošlé na konec", "After deadline last");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "showing", "Zobrazeno", "Showing");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "out_of", "z", "out of");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "status_filter", "Filtr statusu", "Status filter");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "task_adv_sorting", "Řazení", "Sort");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (2, "počet tasku v data provided", "tasks_provided_count_text", "Poskytnuté úkoly", "Tasks in Data provided");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_angle', 'Úhel pohledu', 'Vertical angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_angle', 'Úhel pohledu', 'Vertical angle', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (3, "", "photos_verified", "Prověřeno", "Verified");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "photos_verified", "Prověřeno", "Verified");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_checked_location_ok", "Poloha fotky je korektní", "Photo location is correct");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_checked_location_fail", "Poloha fotky není korektní", "Photo location is not correct");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_checked_location_notvf", "Poloha fotky ještě nebyla ověřena.", "Photo location has not been verified yet");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_original_ok", "Fotka je původní", "Photo is original");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_original_fail", "Fotka není původní", "Photo is not original");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_original_notvf", "Fotka ještě nebyla ověřena", "Photo has not been verified yet");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_checked_location_ok", "Poloha fotky je korektní", "Photo location is correct");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_checked_location_fail", "Poloha fotky není korektní", "Photo location is not correct");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_checked_location_notvf", "Poloha fotky ještě nebyla ověřena.", "Photo location has not been verified yet");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_original_ok", "Fotka je původní", "Photo is original");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_original_fail", "Fotka není původní", "Photo is not original");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_original_notvf", "Fotka ještě nebyla ověřena", "Photo has not been verified yet");

INSERT INTO `egnss4cap`.`page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '3', NULL, 'paths', 'Zobrazit cesty', 'Show paths', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_id", "ID cesty", "Path ID");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_name", "Název", "Name");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_start", "Začátek cesty", "Path start time");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_end", "Konec cesty", "Path end time");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_actions", "Akce", "Actions");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "back", "Zpět", "Back");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_delete", "Odstranit cestu", "Delete path");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_delete_confirm", "Opravdu odstranit cestu?", "Are you sure with deleting path?");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_area", "Plocha", "Area");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '9', NULL, 'heading', 'Správa agentur', 'Agency management', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '10', NULL, 'heading', 'Editace spravců', 'Officers management', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '9', NULL, 'agency_name_text', 'Název agentury', 'Agency name', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '10', NULL, 'new_farmer', 'Nový správce', 'New officer', CURRENT_TIMESTAMP), (NULL, '10', NULL, 'back', 'Zpět', 'Back', CURRENT_TIMESTAMP), (NULL, '10', NULL, 'new_farmer_header', 'Přidat nového správce', 'Add new officer', CURRENT_TIMESTAMP), (NULL, '10', NULL, 'officer_login_text', 'Login', 'Login', CURRENT_TIMESTAMP), (NULL, '10', NULL, 'officer_name_text', 'Jméno', 'Name', CURRENT_TIMESTAMP), (NULL, '10', NULL, 'officer_surname_text', 'Příjmení', 'Surname', CURRENT_TIMESTAMP), (NULL, '10', NULL, 'officer_actions_text', 'Akce', 'Actions', CURRENT_TIMESTAMP);
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "login", "login", "login", "login");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "heslo", "password", "Heslo", "Password");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "jméno", "name", "Jméno", "Name");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "příjmení", "surname", "Příjmení", "Surname");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "identifikační číslo", "ji", "JI", "Identification number");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "e-mail", "email", "E-mail", "E-mail");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "vat", "vat", "Vat", "Vat");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "tlačítko uložit", "save", "Uložit", "Save");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "tlačítko zavřít", "close", "Zavřít", "Close");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "", "edit_user", "Upravit", "Edit");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "", "deactivate_user", "Deaktivovat", "Deactivate");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "", "officer_deactivate_confirm", "Opravdu si přejete správce deaktivovat?", "Are you sure with deactivating officer?");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (10, "", "officer_deactivate_error", "Správce se nepodařilo deaktivovat.", "Officer cannot be deactivated.");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '9', NULL, 'new_agency', 'Přidat novou agenturu', 'Add new agency', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '9', NULL, 'back', 'Zpět', 'Back', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '9', NULL, 'new_agency_header', 'Přidat novou agenturu', 'Add new agency', CURRENT_TIMESTAMP);
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (9, "název", "name", "Název agentury", "Agency name");

INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (9, "tlačítko uložit", "save", "Uložit", "Save");
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (9, "tlačítko zavřít", "close", "Zavřít", "Close");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '5', NULL, 'pdf_export', 'Exportovat do PDF', 'Export to PDF', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '7', NULL, 'pdf_export', 'Exportovat do PDF', 'Export to PDF', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_checked_location_ok", "Poloha fotky je korektní", "Photo location is correct");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_checked_location_fail", "Poloha fotky není korektní", "Photo location is not correct");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_checked_location_notvf", "Poloha fotky ještě nebyla ověřena.", "Photo location has not been verified yet");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_original_ok", "Fotka je původní", "Photo is original");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_original_fail", "Fotka není původní", "Photo is not original");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_original_notvf", "Fotka ještě nebyla ověřena", "Photo has not been verified yet");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, 'detail fotky meta', 'photo_meta_head', 'Metadata fotky', 'Photos metadata', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_lat', 'Zeměpisná šířka', 'Latitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_lng', 'Zeměpisná délka', 'Longitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_altitude', 'Nadmořská výška', 'Altitude', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_bearing', 'Bearing', 'Bearing', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_azimuth', 'Azimuth', 'Azimut', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_roll', 'Roll', 'Roll', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_pitch', 'Pitch', 'Pitch', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_orientation', 'Orientace', 'Orientation', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_hvangle', 'Horizontální úhel pohledu', 'Horizontal view angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_vvangle', 'Vertikální úhel pohledu', 'Vertical view angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_accuracy', 'Přesnost', 'Accuracy', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_device', 'Zařízení', 'Device', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_satsinfo', 'Satelitní info', 'Satellite info', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_nmea', 'NMEA', 'NMEA', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_network', 'Síť', 'Network', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_distance', 'Vzdálenost', 'Distance', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_timestamp', 'Časové razítko (UTC)', 'Timestamp (UTC)', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_yes', 'ANO', 'YES', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_no', 'NE', 'NO', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_note', 'Poznámka', 'Note', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_angle', 'Úhel pohledu', 'Vertical angle', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "pht_created_date", "Vytvořeno (UTC)", "Created (UTC)");


INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "status", "status", "Status", "Status");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "jméno", "name", "Název", "Name");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "popis", "description", "Pokyny", "Guidelines");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "vytvořeno", "created", "Vytvořeno", "Date created");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "do data", "due_date", "Datum do splnění", "Due date");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "akce", "action", "Akce", "Actions");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "schváleno", "ack", "Schváleno", "Accepted");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "odmítnuto", "decline", "Zamítnuto", "Declined");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "photos_verified", "Prověřeno", "Verified");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "poznámka", "note", "Poznámka", "Note");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "důvod znovuotevření", "note_returned", "Důvod znovuotevření", "Reopen reason");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "ucel", "type", "Účel", "Purpose");
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'note_declined', 'Důvod zamítnutí', 'Decline reason', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "yes", "Ano", "Yes");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "no", "Ne", "No");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "nadpis", "header", "detail úkolu", "task detail");
INSERT INTO `page_lang` (`page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (91, NULL, 'heading', 'Galerie nepřiřazených fotek', 'Gallery of unassigned photos', CURRENT_TIMESTAMP);


INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '11', NULL, 'heading', 'Poznámky k verzím', 'Release notes', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '11', NULL, 'web_heading', 'Webová konzole', 'Web console', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '11', NULL, 'ios_heading', 'iOS', 'iOS', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '11', NULL, 'android_heading', 'Android', 'Android', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '11', NULL, 'version', 'Verze', 'Version', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, '11', NULL, 'text', 'Poznámka', 'Note', CURRENT_TIMESTAMP);
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (6, "release notes", "release_notes", "Poznámky k verzím", "Release notes");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_show", "Zobrazit na mapě", "Show on map");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "tooltip_task_ack", "Schválit", "Accept");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "tooltip_task_decline", "Zamítnout", "Decline");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "tooltip_task_return", "Vrátit", "Return");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "tooltip_task_delete", "Smazat", "Delete");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "tooltip_task_move_from_open", "Posunout do Data provided", "Move to Data provided");

INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 91, '', 'pht_distance_nmea', 'Vzdálenost (GNSS)', 'Distance (GNSS)', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_distance_nmea', 'Vzdálenost (GNSS)', 'Distance (GNSS)', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_distance_nmea', 'Vzdálenost (GNSS)', 'Distance (GNSS)', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'pht_distance_nmea_title', 'Vzdálenost mezi polohou dle souřadnic a polohou dle GNSS', 'Distance between location by coordinates and location by GNSS', CURRENT_TIMESTAMP);
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 7, '', 'pht_distance_nmea_title', 'Vzdálenost mezi polohou dle souřadnic a polohou dle GNSS', 'Distance between location by coordinates and location by GNSS', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "export_date", "Datum exportu", "Date of export");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "photo_exported", "Exportováno", "Exported");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "photo_out_of", "z", "ouf of");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "photo_photo", "fotek", "photos");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pdf_export_selected", "Exportovat vybrané do PDF", "Export selected to PDF");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pdf_export_selected", "Exportovat vybrané do PDF", "Export selected to PDF");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_select", "Vybrat", "Select");
INSERT INTO `page_lang` (`id`, `page_id`, `description`, `template_param`, `cz`, `en`, `timestamp`) VALUES (NULL, 5, '', 'assign_photos_select_error', 'Není vybrána žádna fotografie!', 'No photo selected!', CURRENT_TIMESTAMP);

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "show_ekf_metadata", "Zobrazit EKF metadata", "Show EKF metadata");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "show_ekf_metadata", "Zobrazit EKF metadata", "Show EKF metadata");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_ref_time", "Referenční čas", "Reference time");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_ref_time", "Referenční čas", "Reference time");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_altitude", "Nadmořská výška", "Altitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_altitude", "Nadmořská výška", "Altitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_longitude", "Zeměpisná délka", "Longitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_longitude", "Zeměpisná délka", "Longitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (7, "", "pht_latitude", "Zeměpisná šířka", "Latitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (5, "", "pht_latitude", "Zeměpisná šířka", "Latitude");

INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_device", "Zařízení", "Device");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_point", "Bod", "Point");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_path", "Cesta", "Path");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_lat", "Zeměpisná šířka", "Latitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_lng", "Zeměpisná délka", "Longitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_accu", "Přesnost", "Accuracy");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_alt", "Nadmořská výška", "Altitude");
INSERT INTO `page_lang` (page_id, description, template_param, cz, en) VALUES (8, "", "path_popup_created", "Čas vzniku", "Created time");

INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (91,'prep_heading','Generování dokumentu PDF','Generating of PDF document');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (91,'prep_confirm','Vygenerovat','Generate');

INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (2,'show_map','Zobrazit mapu','Show map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (2,'hide_map','Schovat mapu','Hide map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (3,'show_map','Zobrazit mapu','Show map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (3,'hide_map','Schovat mapu','Hide map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (5,'show_map','Zobrazit mapu','Show map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (5,'hide_map','Schovat mapu','Hide map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (7,'show_map','Zobrazit mapu','Show map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (7,'hide_map','Schovat mapu','Hide map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (8,'show_map','Zobrazit mapu','Show map');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (8,'hide_map','Schovat mapu','Hide map');

INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (91,'counter_out_of','z','out of');
INSERT INTO page_lang (page_id,template_param,cz,en) VALUES (91,'wait','Prosím vyčkejte','Please wait');
INSERT INTO `egnss4cap`.`page_lang` (page_id, description, template_param, cz, en) VALUES (91, "", "title", "PDF Export", "PDF Export");
