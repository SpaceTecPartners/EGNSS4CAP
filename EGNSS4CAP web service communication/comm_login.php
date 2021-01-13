<?php

include ("_includes.php");

header('Content-Type: application/json; charset=utf-8');

db_connect();

$client_ip = get_client_ip();

if (isset($_POST['login'])) $login = $_POST['login'];
else $login = $_GET['login'];
$login = trim($login);

if (isset($_POST['pswd'])) $pswd = $_POST['pswd'];
else $pswd = $_GET['pswd'];
$pswd = trim($pswd);

$output = array(); 

if ($user = get_user($login,$pswd)) {
  $output['status'] = 'ok';
  $output['error_msg'] = NULL;
  $output['user'] = $user;
} else {
  $output['status'] = 'error';
  $output['error_msg'] = 'bad login or password';
}

echo json_encode($output);

db_close();

?>