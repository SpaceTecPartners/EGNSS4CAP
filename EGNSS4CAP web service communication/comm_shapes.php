<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

if (isset($_POST['max_lat'])) $max_lat = $_POST['max_lat'];
else $max_lat = $_GET['max_lat'];
$max_lat = trim($max_lat);  

if (isset($_POST['min_lat'])) $min_lat = $_POST['min_lat'];
else $min_lat = $_GET['min_lat'];
$min_lat = trim($min_lat);

if (isset($_POST['max_lng'])) $max_lng = $_POST['max_lng'];
else $max_lng = $_GET['max_lng'];
$max_lng = trim($max_lng);

if (isset($_POST['min_lng'])) $min_lng = $_POST['min_lng'];
else $min_lng = $_GET['min_lng'];
$min_lng = trim($min_lng);

$output = array();

$output['status'] = 'ok';
$output['error_msg'] = null;
$output['shapes'] = get_shapes ($max_lat,$min_lat,$max_lng,$min_lng);

echo json_encode($output);

db_close();

?>