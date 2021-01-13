<?php
function ifdefine($name, $value)
{
  if (!defined($name)) define($name, $value);
}

//definice pouze pokud neni jiz definovano
include_once('_servername.php'); //soubor obsahujici definici konstanty SERVER
ifdefine('SERVER', 'localhost'); //jinak defaultni konstanta urcujici server
//nastaveni cest a serveru

define('APP_VERSION', '1.0.0.');

switch (SERVER) {
  case 'UAT':
    define('SYSTEM_HOST', 'https://systemhost.eu');
    define('DB_TYPE', 'mysqli');
    define('DB_HOST', '127.0.0.1');
    define('DB_USER', 'user');
    define('DB_PASSWORD', 'pswd');
    define('DB_SCHEMA', 'schema');
    break;

  case 'LOCALHOST':
    define('SYSTEM_HOST', 'http://farmari.localhost');
    define('DB_TYPE', 'mysqli');
    define('DB_HOST', 'localhost');
    define('DB_USER', 'user');
    define('DB_PASSWORD', 'pswd');
    define('DB_SCHEMA', 'schema');
    ini_set('display_errors', 1);
    break;    
}


define('INCLUDE_PATH', __DIR__ . DIRECTORY_SEPARATOR);

define('SYSTEM_PROTOCOL', 'http:');

define('CHARSET', 'utf8');

define('SYSTEM_URL', SYSTEM_PROTOCOL . '://' . SYSTEM_HOST . '/');
define('HOMEPAGE', SYSTEM_URL);

require_once(INCLUDE_PATH . 'engine/init.php');
?>
