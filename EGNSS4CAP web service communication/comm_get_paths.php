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
$output['paths'] = get_paths($uid);

echo json_encode($output);

db_close();

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>