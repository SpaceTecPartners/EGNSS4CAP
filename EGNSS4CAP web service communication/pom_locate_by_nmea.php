<?php

include ("/usr/export/wwwroot/egnss4cap/ws/_includes.php");

db_connect();
                                                                                                                                                  
$sql = "SELECT id, nmea_msg, lat, lng FROM photo WHERE flg_checked_location IS NULL AND device_platform = 'Android' LIMIT 2000";
$res = mysqli_query($GLOBALS["mysqli_spoj"], $sql);
while($rec = $res->fetch_assoc()) {
  $photo_id = $rec['id'];
  $nmea_msg = $rec['nmea_msg'];
  $lat = $rec['lat'];
  $lng = $rec['lng'];
      
  $nmea_location = get_coordinates_from_nmea($nmea_msg); 
  if ($nmea_location) {
    $nmea_location_json = json_encode($nmea_location);
    $distance = get_distance_from_coordinates($lat,$lng,$nmea_location['lat'],$nmea_location['lon']);
    
    $check_location = 0;
    if($distance < 50) $check_location = 1;
    
    $sql2 = "UPDATE photo SET nmea_location = '".addslashes($nmea_location_json)."', nmea_distance = '".addslashes($distance)."', flg_checked_location = '$check_location' WHERE id = '".addslashes($photo_id)."'";
    mysqli_query($GLOBALS["mysqli_spoj"], $sql2);
  } else {
    $sql2 = "UPDATE photo SET flg_checked_location = '0' WHERE id = '".addslashes($photo_id)."'";
    mysqli_query($GLOBALS["mysqli_spoj"], $sql2);
  } 
}    

db_close();

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>