<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

$client_ip = get_client_ip();

if (isset($_POST['user_id'])) $user_id = $_POST['user_id'];
else $user_id = $_GET['user_id'];
$user_id = trim($user_id);

if (isset($_POST['name'])) $name = $_POST['name'];
else $name = $_GET['name'];
$name = trim($name);

if (isset($_POST['deviceManufacture'])) $device_manufacture = $_POST['deviceManufacture'];
else $device_manufacture = $_GET['deviceManufacture'];
$device_manufacture = trim($device_manufacture);

if (isset($_POST['deviceModel'])) $device_model = $_POST['deviceModel'];
else $device_model = $_GET['deviceModel'];
$device_model = trim($device_model);

if (isset($_POST['devicePlatform'])) $device_platform = $_POST['devicePlatform'];
else $device_platform = $_GET['devicePlatform'];
$device_platform = trim($device_platform);

if (isset($_POST['deviceVersion'])) $device_version = $_POST['deviceVersion'];
else $device_version = $_GET['deviceVersion'];
$device_version = trim($device_version);

if (isset($_POST['start'])) $start = gmdate('Y-m-d H:i:s', strtotime($_POST['start']));
else $start = gmdate('Y-m-d H:i:s', strtotime($_GET['start']));

if (isset($_POST['end'])) $end = gmdate('Y-m-d H:i:s', strtotime($_POST['end']));
else $end = gmdate('Y-m-d H:i:s', strtotime($_GET['end']));

if (isset($_POST['area'])) $area = $_POST['area'];
else $area = $_GET['area'];
$area = trim($area);

if (isset($_POST['points'])) $points_json = $_POST['points'];
else $points_json = $_GET['points'];
$points_json = trim($points_json);   

$output = array(); 
$output['status'] = 'ok';
$output['error_msg'] = NULL;  

if ($user_id && $start && $end && $points_json) {
  $points = json_decode($points_json,true);
  if (json_last_error() === JSON_ERROR_NONE) {   
      $output = set_path ($user_id,$name,$start,$end,$area,$device_manufacture,$device_model,$device_platform,$device_version,$points);    
  } else {
    $output['status'] = 'error';
    $output['error_msg'] = 'points json decode error';
  } 
} else {
  $output['status'] = 'error';
  $output['error_msg'] = 'missing mandatory data';
} 

if ($output['status'] == 'error') write_log($output['status'],$output['error_msg']);

echo json_encode($output);

db_close();

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>