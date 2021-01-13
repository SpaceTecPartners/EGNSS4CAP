<?php

use login\login_action_router;
use login\login_model;
use login\login_view;
use user\user_model;

$GLOBALS['NO_LOGIN_CHECK'] = true;

require_once('_includes.php');
/*routing*/

if(isset($_POST['act'])){
  $action_router = new login_action_router();
  $action_router->take_action($_POST['act'], array($_POST));
}elseif(isset($_GET['act'])){
  $action_router = new login_action_router();
  $action_router->take_action($_GET['act'], array($_GET));
}

if(isset($_SESSION['user_id'])) header('Location: ' . SYSTEM_HOST . '/index.php');

$model = new login_model();
$template = new login_view();
echo $template->load_login_html_page(array());
?>