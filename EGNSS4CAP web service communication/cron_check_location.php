<?php

include ("/usr/export/wwwroot/egnss4cap/ws/_includes.php");

db_connect();

$sql = "UPDATE photo SET flg_checked_location = 1 
         WHERE flg_checked_location IS NULL 
           AND (network_info IS NULL OR network_location IS NOT NULL)
           AND nmea_distance <= 50 AND distance <= 5000";
$res = mysqli_query($GLOBALS["mysqli_spoj"], $sql);

$sql = "UPDATE photo SET flg_checked_location = 0 
         WHERE flg_checked_location IS NULL 
           AND (network_info IS NULL OR network_location IS NOT NULL)";
$res = mysqli_query($GLOBALS["mysqli_spoj"], $sql);   

db_close();

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>