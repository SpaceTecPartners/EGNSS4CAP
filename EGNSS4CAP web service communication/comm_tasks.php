<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

if (isset($_POST['user_id'])) $uid = $_POST['user_id'];
else $uid = $_GET['user_id'];
$uid = trim($uid);

$output = array(); 

$output['status'] = 'ok';
$output['error_msg'] = NULL;
$output['tasks'] = get_tasks($uid);

echo json_encode($output);

db_close();

?>