<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

$client_ip = get_client_ip();

if (isset($_POST['task_id'])) $task_id = $_POST['task_id'];
else $task_id = $_GET['task_id'];
$task_id = trim($task_id);

if (isset($_POST['user_id'])) $user_id = $_POST['user_id'];
else $user_id = $_GET['user_id'];
$user_id = trim($user_id);

if (isset($_POST['status'])) $status = $_POST['status'];
else $status = $_GET['status'];
$status = trim($status);

if (isset($_POST['note'])) $note = $_POST['note'];
else $note = $_GET['note'];
$note = trim($note);

if (isset($_POST['photos'])) $photos_json = $_POST['photos'];
else $photos_json = $_GET['photos'];
$photos_json = trim($photos_json);   

$output = array(); 
$output['status'] = 'ok';
$output['error_msg'] = NULL;  

if ($photos_json) {
  if ($user_id) {  
    $photos = json_decode($photos_json,true);
    if (json_last_error() === JSON_ERROR_NONE) {   
        $output = set_photos ($photos,$user_id,$task_id);    
    } else {
      $output['status'] = 'error';
      $output['error_msg'] = 'photos json decode error';
    } 
  } else {
    $output['status'] = 'error';
    $output['error_msg'] = 'missing user ID';
  }
}

if ($output['status'] == 'ok') {
  if ($task_id) {
  $task_status = get_task_status($task_id);
    if ($task_status == 'new' && $status == 'open') {
      $output = set_task_status($task_id, $status, $note);
    } elseif (($task_status == 'new' || $task_status == 'open') && $status == 'data provided') {
      if (check_task_photos($task_id)) {
        $output = set_task_status($task_id, $status, $note);
      } else {
        $output['status'] = 'error';
        $output['error_msg'] = 'task has no photos';
      }       
    }
  } 
}  

if ($output['status'] == 'error') write_log($output['status'],$output['error_msg']);

echo json_encode($output);

db_close();

?>