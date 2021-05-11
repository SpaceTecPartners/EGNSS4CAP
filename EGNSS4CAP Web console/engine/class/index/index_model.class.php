<?php

namespace index;

use dibi;
use user\user_model;

class index_model {
  private $user_role;

  public function __construct(int $user_role){
    $this->user_role = $user_role;
  }

  public function load_farmers_list($agency_id, $search){
    if (isset($_SESSION['user_list_sort'])){
        $sort = $this->setListSort($_SESSION['user_list_sort'],'user_list');
    } else {
        $_SESSION['user_list_sort']['u.id'] = 'ASC';
        $sort = "u.id ASC";
    }
    $users_sql = dibi::select('u.id, u.name, u.surname, u.identification_number, u.vat, u.email, u.login')->from('user u')
    ->innerJoin('user_role ur')->on('u.id = ur.user_id')
    ->where('u.pa_id = %i', $agency_id)
    ->where('ur.role_id = %i', user_model::FARMER_ROLE);
    if(!empty($search)){
      $users_sql->where('(name LIKE %~like~ OR surname LIKE %~like~ OR identification_number LIKE %like~)', $search, $search, $search);
    }
      $users_sql->orderBy($sort);
      $users = $users_sql->fetchAll();

      foreach ($users as $key => $user) {
        $users[$key]['tasks_count']=$this->get_farmer_counts($user['id'],'tasks');
        $users[$key]['photos_count']=$this->get_farmer_counts($user['id'],'photos');
        $users[$key]['unassigned_photos_count']=$this->get_farmer_counts($user['id'],'unassigned_photos');
        $users[$key]['tasks_provided_count']=$this->get_farmer_counts($user['id'],'tasks_provided');
      }

    return $users;
  }

  public static function get_farmer_counts(int $farmers_id = 0, $count_type,  $search = ''){
    $count_sql="";
    $ret=0;
    if ($farmers_id !== 0){
      switch ($count_type) {
        case 'tasks':
          $count_sql = dibi::select('count(t.id) as count')->from('task t')->where('t.user_id = %i', $farmers_id)->where('t.flg_deleted = 0');
          break;
        case 'tasks_provided':
          $count_sql = dibi::select('count(t.id) as count')->from('task t')->where('t.user_id = %i', $farmers_id)->where('t.flg_deleted = 0')->and('t.status = "data provided"');
          break;
        case 'photos':
          $count_sql = dibi::select('count(id) as count')->from('photo')->where('user_id = %i', $farmers_id)->where('flg_deleted = 0');
          break;
        case 'unassigned_photos':
          $count_sql = dibi::select('count(id) as count')->from('photo')->where('user_id = %i', $farmers_id)->where('task_id is null')->where('flg_deleted = 0');
          break;
        case 'filtered_tasks':
          if (isset($_SESSION['task_list_filter']['filter'])){
              $filter = index_model::setListFilter($_SESSION['task_list_filter']['filter']);
          } else {
              $filter = index_model::setDefaultListFilter();
          }
          $count_sql = dibi::select('count(t.id) as count')->from('task t')->leftJoin('task_flag tf')->on('t.id = tf.task_id')->where('t.user_id = %i', $farmers_id)->where('t.flg_deleted = 0');
          if(!empty($search))
            $count_sql->where('t.name LIKE %~like~', $search);
          if(!empty($filter))
            $count_sql->where($filter);
          $count_sql->innerJoin('user u')->on('t.user_id = u.id');
          $count_sql->where('u.pa_id = %i', user_model::get_agency_id($farmers_id));
          break;
      }
    }
    return $ret = $count_sql->fetchSingle();
  }

  public function load_farmers_task_list(int $farmers_id = 0, int $pa_id = 0, $search = ''){
    if (isset($_SESSION['task_list_sort'])){
        $sort = $this->setListSort($_SESSION['task_list_sort'], 'task_list');
    } else {
        $_SESSION['task_list_sort']['sort.sortorder'] = 'ASC';
        $sort = "sort.sortorder ASC, created DESC";
    }
    if (isset($_SESSION['task_list_filter']['filter'])){
        $filter = $this->setListFilter($_SESSION['task_list_filter']['filter']);
    } else {
        $filter = $this->setDefaultListFilter();
    }
    if($farmers_id <= 0) $farmers_id = user_model::get_loged_user_id();
    $tasks_sql = dibi::select('t.id, t.status, t.name, t.text, t.date_created as created, t.task_due_date as due, DATE_FORMAT(t.date_created, "%d-%m-%Y") as date_created, DATE_FORMAT(t.task_due_date, "%d-%m-%Y") as task_due_date')
    ->select('COUNT(p.id)')->as('photos_taken')
    ->select('tf.flag_id')
    ->select('sort.sortorder')
    ->from('task t')
    ->leftJoin('photo p')->on('t.id = p.task_id')
    ->leftJoin('task_flag tf')->on('t.id = tf.task_id')
    ->leftJoin('pa_flag pf')->on('pf.id = tf.flag_id')
    ->leftJoin('status_sortorder sort')->on('t.status = sort.status')
    ->where('t.user_id = %i', $farmers_id)
    ->where('t.flg_deleted = 0');
    if(!empty($search))
      $tasks_sql->where('t.name LIKE %~like~', $search);
    if(!empty($filter))
      $tasks_sql->where($filter);
    if($pa_id){
      $tasks_sql->innerJoin('user u')->on('t.user_id = u.id');
      $tasks_sql->where('u.pa_id = %i', $pa_id);
      $_SESSION['users_id_task'] = $farmers_id;
    }
    $tasks_sql->orderBy($sort);//'sort.sortorder ASC, t.id DESC'
    $tasks_sql->groupBy('t.id, t.status, t.name, t.text, date_created, task_due_date, tf.flag_id');
    return $tasks_sql->fetchAll();
  }

  public function load_agency_list(){
    $agency_sql = dibi::select('name,id')
    ->from('pa')
    ->where('name <> "admin"');
    $agency_sql->orderBy('id');
    return $agency_sql->fetchAll();
  }

  public function get_release_notes($type){
    $lang = $_SESSION['lang'];
    $agency_sql = dibi::select('version, note_'.$lang.' as note')
    ->from('release_notes')
    ->where('type = %s', $type);
    $agency_sql->orderBy('id DESC');
    return $agency_sql->fetchAll();
  }

  public function get_pa_name($id){
    $agency_sql = dibi::select('name')
    ->from('pa')
    ->where('id = %i', $id);
    $agency_sql->orderBy('id');
    return $agency_sql->fetchSingle();
  }

  public function load_pa_officer_list($id){
    $agency_sql = dibi::select('u.id, u.login, u.name, u.surname, u.identification_number, u.vat, u.email')
    ->from('user u')
    ->from('user_role ur')
    ->where('ur.role_id = 2')->where('u.id = ur.user_id')
    ->where('u.active = 1')
    ->where('u.pa_id = %i', $id);
    $agency_sql->orderBy('u.id');
    return $agency_sql->fetchAll();
  }


  public function new_farmer($login, $paswd, $name, $surname, $ident_num, $email, $vat, $role=user_model::FARMER_ROLE, $agency_id=false){
    if($agency_id===false) { $agency_id = user_model::get_agency_id(user_model::get_loged_user_id()); }
    user_model::add_new_user($agency_id, $login, $paswd, $name, $surname, $ident_num, $email, $vat, $role);
  }

  public function new_agency($agency_name){
    if(user_model::SUPERADMIN_ROLE === $this->getUser_role()) {
      try{
        dibi::begin();
        $agency_sql_args = array(
          'name%s'=>$agency_name,
          'timestamp%sql'=>'CURRENT_TIMESTAMP()'
        );
        dibi::insert('pa', $agency_sql_args)->execute();
        dibi::commit();
      }catch(\Exception $ex){
        dibi::rollback();
      }
    }
  }

  public function edit_farmer($uid, $paswd, $name, $surname, $ident_num, $email, $vat){
    $agency_id = user_model::get_agency_id(user_model::get_loged_user_id());
    user_model::edit_user($agency_id, $uid, $paswd, $name, $surname, $ident_num, $email, $vat, user_model::FARMER_ROLE);
  }

  public function deactivate_officer($oid){
    $ret=array('error' => '0', 'errorText' => '');
    $fields_update_array = array(
      'active%i' => 0
    );
    if (user_model::SUPERADMIN_ROLE !== $this->getUser_role()){
      $ret = array('error' => '1', 'errorText' => user_model::get_translate_from_db('officer_deactivate_error'));
    } else {
      try{
        dibi::begin();
        dibi::update('user', $fields_update_array)->where('id = %i', $oid)->execute();
        dibi::commit();
      }catch(Exception $ex){
        dibi::rollback();
      }
    }
    return $ret;
  }

  public function getUser_role(){
    return $this->user_role;
  }

  public function setUser_role($user_role){
    $this->user_role = $user_role;
  }

  public function setListSort($sortArray, $page){//and reset filter
      $return = "";
      if (!empty($sortArray)){
          $delim="";
          if(isset($sortArray['afterdeadline'])){
            $return .= "$delim due DESC";
            $delim = ",";
          }
          foreach ($sortArray as $key => $value){
              if ($key != 'afterdeadline'){
                $return .= "$delim $key $value";
                $delim = ",";
              }
          }
      }
      if (empty($return)){
          switch ($page) {
            case 'task_list':
              $return="sort.sortorder ASC, created DESC";
              break;
            case 'user_list':
              $return="u.id ASC";
              break;
          }
      } else {
        switch ($page) {
          case 'task_list':
            $return.=", created DESC";
            break;
        }
      }
      return $return;
  }

  public static function setListFilter($filterArray){
      $return = "";
      $and = "";
      if (!empty($filterArray)){
        foreach ($filterArray as $key => $value){
          if ($key == 'status'){
            $statuses = "";
            $delim = "";
            foreach ($value as $status => $status_val) {
              if ($status_val == 0){
                $statuses.=$delim."'".$status."'";
                $delim=",";
              }
            }
            if (!empty($statuses)){
              $return .= "$and t.status not in ($statuses)";
              $and = " and";
            } else {
              $return .= "";
            }
          } else if ($key == 'flag') {
            $flags = "";
            $delim = "";
            foreach ($value as $flag => $flag_val) {
              if ($flag_val == 0){
                $flags.=$delim.$flag;
                $delim=",";
              }
            }
            if (!empty($flags)){
              $return .= "$and IFNULL(tf.flag_id, 0) not in ($flags)";
              $and = " and";
            } else {
              $return .= "";
            }
          } else {
            $return .= "$and $key = '$value'";
            $and = " and";
          }
        }
      }
      //var_dump($return);
      return $return;
  }

  public static function setDefaultListFilter($sqlver = true, $page = "task_list"){
    unset($_SESSION[$page.'_filter']);
    if ($page == 'task_list') {
        $_SESSION['task_list_filter']['filter'] = array('status' => array('new' => 1,'open' => 1,'data provided' => 1, 'data checked' => 1, 'closed' => 1, 'returned' => 1), 'flag' => array('1' => 1, '2' => 1));
        $_SESSION[$page.'_filter']['search'] = "";
    }
    if ($sqlver) return index_model::setListFilter($_SESSION[$page.'_filter']['filter']);
  }

  public static function get_data_from_db($fields){
    return dibi::select($fields['db_col'].' as data')->from($fields['db_table'])->where('id = %i', $fields['db_id'])->fetchSingle();
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
