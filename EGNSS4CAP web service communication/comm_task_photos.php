<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

if (isset($_POST['task_id'])) $task_id = $_POST['task_id'];
else $task_id = $_GET['task_id'];
$task_id = trim($task_id);  

if (isset($_POST['user_id'])) $user_id = $_POST['user_id'];
else $user_id = $_GET['user_id'];
$user_id = trim($user_id);

$output = array();

$output['status'] = 'ok';
$output['error_msg'] = null;
$output['photos'] = get_task_photos ($task_id,$user_id);

echo json_encode($output);

db_close();

?>