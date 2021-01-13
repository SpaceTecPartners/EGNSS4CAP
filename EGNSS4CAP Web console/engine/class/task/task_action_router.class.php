<?php
namespace task;

use user\user_model;

class task_action_router extends \action_router{
  protected $actions = array(
    'task_detail',
    'accept_task',
    'decline_task',
    'return_task',
    'delete_task',
    'move_from_open_task',
    'get_tasks_gps_points',
    'get_tasks_photos',
    'get_polygons_gps_points'
  );

  public function __construct(){
    parent::__construct($this->actions);
  }

  protected function task_detail($params){
    $model = new task_model();
    $template = new task_view();
    $users_id = isset($_SESSION['users_id_task']) ? $_SESSION['users_id_task'] : user_model::get_loged_user_id();
    $users_credencials = user_model::get_users_credencials($users_id);
    //$agent = user_model::get_user_role(user_model::get_loged_user_id()); nahrazeno funkci user_model::is_agency()
    $tasks = $model->load_task_info($params['id']);
    $tasks_verified = array();
    foreach ($tasks as $key => $task) {
      $tasks_verified[$task['id']] = task_model::photos_verified_status($task['id']);
    }
    $template_array = array(
      'agency' => user_model::is_agency(),
      'tasks' => $tasks,
      'tasks_verified' => $tasks_verified,
      'show_trash' => ($tasks[0]['creator'] === user_model::get_loged_user_id())?true:false,
      'images' => $model->load_task_photos($params['id']),
      'farmer_name' => $users_credencials['surname'] . ' ' . $users_credencials['name'],
      "ret_url" => "index.php?act=agency_user_tasks_list&id=".$users_credencials['id']."#".$params['id'],
      "pdf_url" => "pdf_export.php?user_id=".$users_credencials['id']."&task_id=".$params['id']
    );
    if ($tasks[0]['deleted'] === 1){
      header("Location: index.php?act=agency_user_tasks_list&id=".$users_credencials['id']);
    }
    echo $template->load_task_html_page($template_array);
    exit;
  }

  protected function accept_task($params){
    $model = new task_model();
    echo json_encode($model->accept_task_photos($params['id'], $params['reload']));
    exit;
  }

  protected function decline_task($params){
    $model = new task_model();
    $model->decline_task_photos($params['id'], $params['text']);
    exit;
  }

  protected function return_task($params){
    $model = new task_model();
    $model->return_task_photos($params['id'], $params['text']);
    exit;
  }

  protected function delete_task($params){
    $model = new task_model();
    echo json_encode($model->delete_task_photos($params['id']));
    exit;
  }

  protected function move_from_open_task($params){
    $model = new task_model();
    echo json_encode($model->move_from_open_task_photos($params['id'], $params['text']));
    exit;
  }

  protected function get_tasks_gps_points($params){
    $model = new task_model();
    $search = isset($params['search']) ? $params['search'] : '';
    $spec_ids = isset($params['specified_ids']) ? $params['specified_ids'] : '';
    echo json_encode($model->get_tasks_gps($search, $params['filter_type'], $spec_ids));
    exit;
  }

  protected function get_polygons_gps_points($params){
    $model = new task_model();
    $actualBounds = array(
      'lat' => array ('min' => floatval($params['lat_min'])-0.004, 'max' => floatval($params['lat_max'])+0.004),
      'lng' => array ('min' => floatval($params['lng_min'])-0.004, 'max' => floatval($params['lng_max'])+0.004)
    );
    //todo map square
    echo json_encode($model->get_polygons_gps($actualBounds));
    exit;
  }

  protected function get_tasks_photos($params){
    $model = new task_model();
    $template = new task_view();
    if ($params['type']=='users_gallery'){
      $paths_names = $model->load_single_photo($params['id']);
      $users_credencials = user_model::get_users_credencials_from_photo($params['id']);
      $template_array = array(
        'task_id' => null,
        'photo_id' => $params['id'],
        'photos' => $paths_names,
        'farmer_name' => $users_credencials['surname'] . " " . $users_credencials['name'],
        'task_name' => "",
        'azimut' => $model->get_photo_azimuth($params['id'])
      );
    } else {
      $paths_names = $model->load_single_photo($params['photo_id']);
      $users_credencials = user_model::get_users_credencials_from_task($params['id']);
      $template_array = array(
        'task_id' => $params['id'],
        'photo_id' => null,
        'photos' => $paths_names,
        'farmer_name' => $users_credencials['surname'] . " " . $users_credencials['name'],
        'task_name' => $users_credencials['task_name'],
        'photos_count' => $model->get_task_photo_count($params['id']),
        'azimut' => $model->get_photo_azimuth($params['photo_id'])
      );
    }
    echo $template->load_tasks_photos_html($template_array);
    exit;
  }

}

?>
