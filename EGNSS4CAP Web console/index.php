<?php

use index\index_action_router;
use index\index_model;
use index\index_view;
use user\user_model;

require_once '_includes.php';

if(isset($_POST['act'])){
  $action_router = new index_action_router();
  $action_router->take_action($_POST['act'], array($_POST));
}elseif (isset($_GET['act'])){
  $action_router = new index_action_router();
  $action_router->take_action($_GET['act'], array($_GET));
}

$model = new index_model(user_model::get_user_role(user_model::get_loged_user_id()));
$template = new index_view();
if($model->getUser_role() == user_model::FARMER_ROLE){
  if (!isset($_SESSION['task_list_sort'])){
    $_SESSION['task_list_sort']['sort.sortorder'] = 'ASC';
  }
  if (!isset($_SESSION['task_list_filter'])){
    $model->setDefaultListFilter(false);
  }
  $template_variables = array(
    "img_gallery" => "index.php?act=gallery_unassigned&id=".user_model::get_loged_user_id(),
    "url_paths" => "index.php?act=user_paths&id=".user_model::get_loged_user_id(),
    "sort" => (isset($_SESSION['task_list_sort'])?$_SESSION['task_list_sort']:array()),
    "filter" => (isset($_SESSION['task_list_filter'])?$_SESSION['task_list_filter']:array("search" => "", "filter" => "")),
    "count" => array("total" => index_model::get_farmer_counts(user_model::get_loged_user_id(), 'tasks'), "filtered" => index_model::get_farmer_counts(user_model::get_loged_user_id(), 'filtered_tasks', (isset($_SESSION['task_list_filter']['search'])?$_SESSION['task_list_filter']['search']:"")))
  );
  echo $template->load_index_farmers_tasks_html_page($template_variables);
}
if($model->getUser_role() == user_model::OFFICER_ROLE){
  if (!isset($_SESSION['user_list_sort'])){
    $_SESSION['user_list_sort']['u.id'] = 'ASC';
  }
  $template_variables = array(
    "test" => 'test',
    "agency" => true,
    "sort" => (isset($_SESSION['user_list_sort'])?$_SESSION['user_list_sort']:array()),
    "filter" => (isset($_SESSION['user_list_filter'])?$_SESSION['user_list_filter']:"")
  );
  echo $template->load_index_agency_farmers_html_page($template_variables);
}
if($model->getUser_role() == user_model::SUPERADMIN_ROLE){
  $template_variables = array(
    "agency" => false,
    "superadmin" => true,
  );
  echo $template->load_index_agency_html_page($template_variables);
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
