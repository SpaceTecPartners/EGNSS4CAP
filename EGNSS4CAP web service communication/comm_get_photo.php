<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

if (isset($_POST['photo_id'])) $photo_id = $_POST['photo_id'];
else $photo_id = $_GET['photo_id'];
$photo_id = trim($photo_id);  

$output = array();

$output['status'] = 'ok';
$output['error_msg'] = null;
$output['photo'] = get_photo($photo_id);

if (empty($output['photo'])) {
  $output['status'] = 'error';
  $output['error_msg'] = 'wrong photo ID';
  unset ($output['photo']);
}

echo json_encode($output);

db_close();

//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>