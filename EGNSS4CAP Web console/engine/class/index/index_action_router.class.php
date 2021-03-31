<?php
namespace index;

use task\task_model;
use user\user_model;

class index_action_router extends \action_router{
  protected $actions = array(
    'agency_user_tasks_list',
    'new_farmer',
    'edit_farmer',
    'add_task',
    'load_users_table',
    'load_task_table',
    'load_agency_table',
    'pa_officers_list',
    'load_pa_officer_table',
    'deactivate_officer',
    'new_agency',
    'gallery_unassigned',
    'assign_photos_to_task',
    'list_sort',
    'list_filter',
    'get_translate',
    'get_data_from_db',
    'save_photo_rotation',
    'delete_photo',
    'user_paths',
    'get_user_paths_gps_points',
    'delete_path',
    'new_agency',
    'load_release_notes'
  );

  public function __construct(){
    parent::__construct($this->actions);
  }

  protected function agency_user_tasks_list($params){
    if(isset($params['id'])){
      if (!isset($_SESSION['task_list_sort'])){
        $_SESSION['task_list_sort']['sort.sortorder'] = 'ASC';
      }
      if (!isset($_SESSION['task_list_filter'])){
        index_model::setDefaultListFilter(false);
      }
      $template = new index_view();
      $users_credencials = user_model::get_users_credencials($params['id']);
      $template_variables = array(
        "agency"=>user_model::is_agency(),
        "statuses" => task_model::STATUS,
        "farmer_name" => $users_credencials['surname'] . ' ' . $users_credencials['name'],
        "ret_url" => "index.php#".$params['id'],
        "task_types" => task_model::get_task_types(),
        "img_gallery" => "index.php?act=gallery_unassigned&id=".$params['id'],
        "url_paths" => "index.php?act=user_paths&id=".$params['id'],
        "sort" => (isset($_SESSION['task_list_sort'])?$_SESSION['task_list_sort']:array()),
        "filter" => (isset($_SESSION['task_list_filter'])?$_SESSION['task_list_filter']:array("search" => "", "filter" => "")),
        "count" => array("total" => index_model::get_farmer_counts($params['id'], 'tasks'), "filtered" => index_model::get_farmer_counts($params['id'], 'filtered_tasks'))
      );
      echo $template->load_index_farmers_tasks_html_page($template_variables);
    }
    exit;
  }

  protected function gallery_unassigned($params){
    if(isset($params['id'])){
      $template = new index_view();
      if(isset($params['task_select']) && $params['task_select']=='true'){
        $tasks = user_model::get_users_tasks($params['id']);
        $gallery = user_model::get_users_gallery_unassigned($params['id'],$params['photos']);
        $template_variables = array(
          "agency"=>user_model::is_agency(),
          "task_select"=>true,
          "photos"=>$gallery,
          "photo_ids"=>implode($params['photos'],','),
          "tasks"=>$tasks,
          "user_id"=>$params['id'],
          "ret_url" => "index.php?act=agency_user_tasks_list&id=".$params['id']."#".$params['id']
        );
      } else {
        $gallery = user_model::get_users_gallery_unassigned($params['id']);
        $template_variables = array(
          "agency"=>user_model::is_agency(),
          "task_select"=>false,
          "photos"=>$gallery,
          "user_id"=>$params['id'],
          "ret_url" => "index.php?act=agency_user_tasks_list&id=".$params['id'],
          "pdf_url" => "pdf_export.php?act=prepare&user_id=".$params['id']."&task_id=0"
        );
      }
      echo $template->load_index_gallery_unassigned_html_page($template_variables);
    }
    exit;
  }

  protected function user_paths($params){
    if(isset($params['id'])){
      $template = new index_view();
      $paths = user_model::get_users_paths($params['id']);
      $template_variables = array(
        "agency"=>user_model::is_agency(),
        "paths"=>$paths,
        "show_visible_checkbox"=>(count($paths)>1?true:false),
        "user_id"=>$params['id'],
        "ret_url" => "index.php?act=agency_user_tasks_list&id=".$params['id']
      );

      echo $template->load_index_user_paths_html_page($template_variables);
    }
    exit;
  }

  protected function load_release_notes(){
    $model = new index_model(user_model::OFFICER_ROLE);
    $template = new index_view();
    $template_variables = array(
      "web" => $model->get_release_notes('web'),
      "android" => $model->get_release_notes('android'),
      "ios" => $model->get_release_notes('ios')
    );
    echo $template->load_index_release_notes_html_page($template_variables);
    exit;
  }

  protected function get_user_paths_gps_points($params){
    echo json_encode(user_model::get_users_paths($params['user_id']));
    exit;
  }

  protected function assign_photos_to_task($params){
    if (isset($params['photos']) && !empty($params['photos'])){
      task_model::assign_photos($params['user_id'], $params['photos'], $params['task_id']);
    }
    header("Location: index.php?act=agency_user_tasks_list&id=".$params['user_id']);
    exit;
  }

  protected function new_farmer($params){
    $model = new index_model(user_model::OFFICER_ROLE);
    if(!isset($params['role'])){ $params['role'] = 1; }
    if(!isset($params['agency_id'])){ $params['agency_id'] = false; }
    $model->new_farmer($params['login'], $params['password'], $params['name'], $params['surname'], $params['identification_number'],
    $params['email'], $params['vat'], $params['role'], $params['agency_id']);
    header("Location: ".$_SERVER['HTTP_REFERER']);
    exit;
  }

  protected function new_agency($params){
    $model = new index_model(user_model::SUPERADMIN_ROLE);
    $model->new_agency($params['agency_name']);
    header("Location: ".$_SERVER['HTTP_REFERER']);
    exit;
  }

  protected function edit_farmer($params){
    $model = new index_model(user_model::OFFICER_ROLE);
    $model->edit_farmer($params['user_id'], $params['password'], $params['name'], $params['surname'], $params['identification_number'],
    $params['email'], $params['vat']);
    header("Location: ".$_SERVER['HTTP_REFERER']);
    exit;
  }

  protected function add_task($params){
    $params['note']="";//not in formdata
    $result = task_model::add_new_task($_SESSION['users_id_task'], user_model::get_loged_user_id(), $params['status'], $params['name'], $params['description'], $params['due_date'], $params['note'], $params['type']);
    echo json_encode($result);
    //header("Location: ".$_SERVER['HTTP_REFERER']);
    exit;
  }

  protected function load_users_table($params){
    $model = new index_model(user_model::OFFICER_ROLE);
    $template = new index_view();
    $search = (isset($_SESSION['user_list_filter'])?$_SESSION['user_list_filter']:'');
    if (isset($params['search'])){
      $search = $params['search'];
      $_SESSION['user_list_filter'] = $search;
    }
    echo $template->load_table_of_users(array('users'=>$model->load_farmers_list(user_model::get_agency_id(user_model::get_loged_user_id()), $search), 'agency'=>user_model::is_agency()));
    exit;
  }

  protected function load_task_table($params){
    if(isset($params['id'])){ //for oficera
      $model = new index_model(user_model::OFFICER_ROLE);
      $search = (isset($_SESSION['task_list_filter']['search'])?$_SESSION['task_list_filter']['search']:'');
      if (isset($params['search'])){
        $search = $params['search'];
        $_SESSION['task_list_filter']['search'] = $search;
      }
      $farmers_tasks = $model->load_farmers_task_list($params['id'], user_model::get_agency_id(user_model::get_loged_user_id()), $search);
      $farmers_tasks_verified = array();
      foreach ($farmers_tasks as $key => $task) {
        $farmers_tasks_verified[$task['id']] = task_model::photos_verified_status($task['id']);
      }
      $status_texts = task_model::get_statuses_texts();
      $template = new index_view();
      $template_variables = array(
        "tasks"=>$farmers_tasks,
        "tasks_verified"=>$farmers_tasks_verified,
        "agency"=>user_model::is_agency(),
      );
      $template_variables = array_merge($template_variables, $status_texts);
      echo $template->load_table_of_tasks($template_variables);
    }else{
      $model = new index_model(user_model::FARMER_ROLE);
      $search = (isset($_SESSION['task_list_filter']['search'])?$_SESSION['task_list_filter']['search']:'');
      if (isset($params['search'])){
        $search = $params['search'];
        $_SESSION['task_list_filter']['search'] = $search;
      }
      $farmers_tasks = $model->load_farmers_task_list(0, 0, $search); //0 is default
      $status_texts = task_model::get_statuses_texts();
      $template = new index_view();
      $template_variables = array(
        "tasks"=>$farmers_tasks,
        "agency"=>user_model::is_agency(),
      );
      $template_variables = array_merge($template_variables, $status_texts);
      echo $template->load_table_of_tasks($template_variables);
    }
    exit;
  }

  protected function pa_officers_list($params){
    if(isset($params['id'])){
      $model = new index_model(user_model::SUPERADMIN_ROLE);
      $template = new index_view();
      $template_variables = array(
        "agency" => false,
        "superadmin" => true,
        "pa_id" => $params['id'],
        "pa_name" => $model->get_pa_name($params['id']),
        "ret_url" => "index.php#".$params['id']
      );
      echo $template->load_index_pa_officers_html_page($template_variables);
    }
    exit;
  }

  protected function load_agency_table(){
    $model = new index_model(user_model::SUPERADMIN_ROLE);
    $list_of_agency = $model->load_agency_list();

    $template = new index_view();
    $template_variables = array(
      "list_of_agency"=>$list_of_agency,
      "agency"=> false,
      "superadmin" => true
    );

    echo $template->load_table_of_agency($template_variables);
    exit;
  }

  protected function load_pa_officer_table($params){
    $model = new index_model(user_model::SUPERADMIN_ROLE);
    $list_of_officers = $model->load_pa_officer_list($params['id']);

    $template = new index_view();
    $template_variables = array(
      "list_of_officers"=>$list_of_officers,
      "agency"=> false,
      "superadmin" => true
    );

    echo $template->load_table_of_pa_officers($template_variables);
    exit;
  }

  protected function list_sort($params){
    if(isset($_POST['sort'])) {
      $sort = $_POST['sort'];
      if ($sort == 'reset'){
          unset($_SESSION[$params['page'].'_sort']);
          index_model::setDefaultListFilter(false, $params['page']);//unset($_SESSION[$params['page'].'_filter']);
          switch ($params['page']) {
            case 'task_list':
              $_SESSION[$params['page'].'_sort']['sort.sortorder']='ASC';
              break;
            case 'user_list':
              $_SESSION[$params['page'].'_sort']['u.id']='ASC';
              break;
          }
      } else if ($sort == 'afterdeadline') {
        if (isset($_SESSION[$params['page'].'_sort'][$sort]) && $_SESSION[$params['page'].'_sort'][$sort] == 'DESC'){
          unset($_SESSION[$params['page'].'_sort'][$sort]);
        } else {
          $_SESSION[$params['page'].'_sort'][$sort] = 'DESC';
        }
      } else {
          if (isset($_SESSION[$params['page'].'_sort'][$sort]) && $_SESSION[$params['page'].'_sort'][$sort] == 'ASC'){
              unset($_SESSION[$params['page'].'_sort']);
              $_SESSION[$params['page'].'_sort'][$sort] = 'DESC';
          } else {
              unset($_SESSION[$params['page'].'_sort']);
              $_SESSION[$params['page'].'_sort'][$sort] = 'ASC';
          }
      }
    }
    echo "1";
    exit;
  }

  protected function list_filter($params){
    if(isset($_POST['filter'])) {
        $filter = $_POST['filter'];
        if ($filter == 'reset'){
            unset($_SESSION[$params['page'].'_filter']);
        } else if ($filter == 'status') {
          $type = $_POST['type'];
          $value = $_POST['value'];
          if (($value == '' || empty($value)) && $value != '0'){
              //unset($_SESSION[$params['page'].'_filter']['filter']['status'][$type]);
              $_SESSION[$params['page'].'_filter']['filter']['status'][$type] = '0';
          } else {
              $_SESSION[$params['page'].'_filter']['filter']['status'][$type] = $value;
          }
        } else if ($filter == 'flag') {
          $type = $_POST['type'];
          $value = $_POST['value'];
          if (($value == '' || empty($value)) && $value != '0'){
              //unset($_SESSION[$params['page'].'_filter']['filter']['status'][$type]);
              $_SESSION[$params['page'].'_filter']['filter']['flag'][$type] = '0';
          } else {
              $_SESSION[$params['page'].'_filter']['filter']['flag'][$type] = $value;
          }
        } else {
          $value = $_POST['value'];
          if (($value == '' || empty($value)) && $value != '0'){
              unset($_SESSION[$params['page'].'_filter']['filter'][$filter]);
          } else {
              $_SESSION[$params['page'].'_filter']['filter'][$filter] = $value;
          }
        }
    }
    echo "1";
    exit;
  }

  protected function get_translate($params){
    echo json_encode(user_model::get_translate_from_db($params['tmp_param']));
    exit;
  }

  protected function save_photo_rotation($params){
    if (user_model::save_photo_rotation_to_db($params['photo_id'],$params['rotation'])){
      echo json_encode(array('err' => "1"));
    } else {
      echo json_encode(array('err' => "0"));
    }
    exit;
  }

  protected function deactivate_officer($params){
    $model = new index_model(user_model::SUPERADMIN_ROLE);
    echo json_encode($model->deactivate_officer($params['id']));
    exit;
  }

  protected function get_data_from_db($params){
    $db_fields = array();
    switch ($params['type']) {
      case 'task_note':
        $db_fields = array('db_table' => "task", 'db_col' => "note", 'db_id' => $params['id']);
        break;
    }
    if (!empty($db_fields)){
      echo json_encode(index_model::get_data_from_db($db_fields));
    } else {
      echo json_encode('null');
    }
    exit;
  }

  protected function delete_photo($params){
    echo json_encode(user_model::delete_unassigned_photo($params['id'], $params['reload']));
    exit;
  }

  protected function delete_path($params){
    echo json_encode(user_model::delete_user_path($params['id'], $params['reload']));
    exit;
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
