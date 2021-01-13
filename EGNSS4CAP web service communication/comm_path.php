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
      $output = set_path ($user_id,$name,$start,$end,$area,$points);    
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

?>