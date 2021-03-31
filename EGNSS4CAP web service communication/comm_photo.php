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

if (isset($_POST['photo'])) $photo_json = $_POST['photo'];
else $photo_json = $_GET['photo'];
$photo_json = trim($photo_json);  

$status_ok = true;
if ($task_id) {
  $task_status = get_task_status($task_id);
  if (($task_status != 'new' && $task_status != 'open' && $task_status != 'returned')) {
    $status_ok = false;
  }
}  

$output = array(); 
$output['status'] = 'ok';
$output['error_msg'] = NULL;  

if ($photo_json) {
  if ($user_id) {  
    if ($status_ok) {      
      $photo = json_decode($photo_json,true);
      if (json_last_error() === JSON_ERROR_NONE) {   
          $output = set_photo ($photo,$user_id,$task_id);    
      } else {
        $output['status'] = 'error';
        $output['error_msg'] = 'photo json decode error';
      } 
    } else {
      $output['status'] = 'error';
      $output['error_msg'] = 'task is not in editable status';
    }
  } else {
    $output['status'] = 'error';
    $output['error_msg'] = 'missing user ID';
  }
}

if ($output['status'] == 'error') write_log($output['status'],$output['error_msg']);

echo json_encode($output);

db_close();

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>