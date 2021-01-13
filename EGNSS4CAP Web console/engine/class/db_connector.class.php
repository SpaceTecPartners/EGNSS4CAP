<?php

use Dibi\Connection;

class db_connector
{
  public static function connect($static = true) : Connection{
    $connection = null;
    $con_arr = array(
      'driver' => DB_TYPE,
      'host' => DB_HOST,
      'username' => DB_USER,
      'password' => DB_PASSWORD,
      'database' => DB_SCHEMA,
      'charset' => CHARSET,
    );
    try{
      if($static) return dibi::connect($con_arr);
      else $connection = new Dibi\Connection($con_arr);
    }catch (Exception $ex) {
      echo ('Připojení do db se nezdařilo');
      exit;
    //todo: logovat
    }
    return $connection;
  }
}
?>