<?php

include ("/usr/export/wwwroot/egnss4cap/ws/_includes.php");

db_connect();

$sql = "SELECT id, network_info, lat, lng, nmea_msg, extra_sat_count FROM photo WHERE network_location IS NULL AND network_info IS NOT NULL";
//$sql = "SELECT id, network_info, lat, lng, nmea_msg, extra_sat_count FROM photo WHERE network_info IS NOT NULL";
$res = mysqli_query($GLOBALS["mysqli_spoj"], $sql);
while($rec = $res->fetch_assoc()) {
  $photo_id = $rec['id'];
  $network_info = json_decode($rec['network_info'],true);
  $lat = $rec['lat'];
  $lng = $rec['lng'];
  $nmea_msg = $rec['nmea_msg'];
  $extra_sat_count = $rec['extra_sat_count'];
  
  $info = array();
  $info['token'] = '03f5591c4d4aeb';
  //$info['radio'] = $network_info['radio'];
  //$info['mcc'] = $network_info['mcc'];
  //$info['mnc'] = $network_info['mnc'];         
    
  if ($network_info['cells']) {
    foreach ($network_info['cells'] as $nc) {
      $cell = array();
      $cell['radio'] = $nc['radio'];
      $cell['mcc'] = $nc['mcc'];
      $cell['mnc'] = $nc['mnc'];  
      if ($nc['lac']) {
        $cell['lac'] = $nc['lac'];
      } else {
        $cell['lac'] = $nc['tac'];
      }
      if ($nc['cid']) {
        $cell['cid'] = $nc['cid'];
      } else {
        $cell['cid'] = $nc['ci'];
      }
      $info['cells'][] = $cell;
    }
  }
    
  $info['address'] = 1;
  
  if ($network_info['wifi']) {
    $wifi = array();
    $wifi['bssid'] = $network_info['wifi']['bssid'];
    $wifi['channel'] = $network_info['wifi']['channel'];
    $wifi['frequency'] = $network_info['wifi']['frequency'];
    $wifi['signal'] = $network_info['wifi']['signal'];
    
    $info['wifi'][] = $wifi;
  }
  
  $info_json = json_encode($info);               
  $location_json = get_location($info_json);  
  $location = json_decode($location_json,true);
  
  $distance = NULL;
  if ($location['lat'] && $location['lon'] && $lat && $lng) {
    $distance = get_distance_from_coordinates($lat,$lng,$location['lat'],$location['lon']);      
  }  
  
  set_network_location_and_distance ($photo_id, $location_json, $distance); 
}    

db_close();

?>