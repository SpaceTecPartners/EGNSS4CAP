<?php

use task\task_action_router;
use task\task_model;
use task\task_view;

require_once '_includes.php';

if(isset($_POST['act'])){
  $action_router = new task_action_router();
  $action_router->take_action($_POST['act'], array($_POST));
}elseif (isset($_GET['act'])){
  $action_router = new task_action_router();
  $action_router->take_action($_GET['act'], array($_GET));
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
