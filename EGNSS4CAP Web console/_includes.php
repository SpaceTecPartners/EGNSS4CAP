<?php
function ifdefine($name, $value)
{
  if (!defined($name)) define($name, $value);
}

include_once('_servername.php'); //file used for set SERVER
ifdefine('SERVER', 'localhost'); //default server
//set of paths and server

define('APP_VERSION', '1.0.0.');

switch (SERVER) {
  case 'UAT':
    define('SYSTEM_HOST', 'https://egnss4cap-uat.foxcom.eu');
    define('DB_TYPE', 'mysqli');
    define('DB_HOST', '127.0.0.1');
    define('DB_USER', 'login');
    define('DB_PASSWORD', 'pswd');
    define('DB_SCHEMA', 'egnss4cap');
    break;

  case 'LOCALHOST':
    define('SYSTEM_HOST', 'http://farmari.localhost');
    define('DB_TYPE', 'mysqli');
    define('DB_HOST', 'localhost');
    define('DB_USER', 'login');
    define('DB_PASSWORD', 'pswd');
    define('DB_SCHEMA', 'egnss4cap');
    ini_set('display_errors', 1);
    break;
}


define('INCLUDE_PATH', __DIR__ . DIRECTORY_SEPARATOR);

define('SYSTEM_PROTOCOL', 'http:');

define('CHARSET', 'utf8');

define('SYSTEM_URL', SYSTEM_PROTOCOL . '://' . SYSTEM_HOST . '/');
define('HOMEPAGE', SYSTEM_URL);

require_once(INCLUDE_PATH . 'engine/init.php');

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
