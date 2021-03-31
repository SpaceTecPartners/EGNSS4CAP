<?php

use login\login_model;

date_default_timezone_set('Europe/Prague');

if (!defined(__DIR__)) define(__DIR__, dirname(__FILE__));

require_once(INCLUDE_PATH . 'engine/autoloader.class.php'); //autolaoder
require_once(INCLUDE_PATH . '/vendor/autoload.php');
autoloader::register();
db_connector::connect();

session_start();
if(isset($GLOBALS['NO_LOGIN_CHECK']))/*do not check*/;
else if(!login_model::is_user_logged()) header('Location: ' . SYSTEM_HOST . '/login.php');
$_SESSION['lang'] = isset($_SESSION['lang']) ? $_SESSION['lang'] : 'en';
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
