<?php

include ("/usr/export/wwwroot/egnss4cap/ws/_includes.php");

db_connect();

$sql = "UPDATE photo SET flg_checked_location = 0 
         WHERE flg_checked_location IS NULL 
           AND (network_info IS NULL OR network_location IS NOT NULL)
           AND nmea_msg IS NULL AND (distance IS NULL OR distance >= 1000)";
$res = mysqli_query($GLOBALS["mysqli_spoj"], $sql);

$sql = "UPDATE photo SET flg_checked_location = 1 
         WHERE flg_checked_location IS NULL 
           AND (network_info IS NULL OR network_location IS NOT NULL)
           AND (nmea_msg IS NOT NULL OR distance < 1000)";
$res = mysqli_query($GLOBALS["mysqli_spoj"], $sql);   

db_close();

?>