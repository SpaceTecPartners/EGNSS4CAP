<?php

namespace task;

use dibi;
use Exception;
use index\index_view;
use index\index_model;
use user\user_model;

class task_model{
  public const STATUS = array('NEW', 'OPEN', 'DATA PROVIDED', 'DATA CHECKED', 'CLOSED', 'RETURNED');
  public const VALID = 1;
  public const INVALID = 2;
  public const RETURNED = 3;

  public const FILTER_FARMERS = 'user';
  public const FILTER_TASKS = 'task';
  public const FILTER_TASKS_DETAIL = 'task_detail';
  public const FILTER_USERS_GALLERY = 'users_gallery';

  public static function add_new_task($user_id, $created_id, $status, $name, $text, $task_due_date, $note, $type){
    $ret=array('error' => '0', 'errorText' => '');
    if ($task_due_date < date("Y-m-d")){
      $ret=array('error' => '1', 'errorText' => user_model::get_translate_from_db('task_due_date_error'));
    } else {
      try {
        dibi::begin();
        $sql_task_arg = array(
          'user_id%i'=> $user_id,
          'created_id%i'=> $created_id,
          'type_id%i' => $type,
          'status'=> $status,
          'name'=> $name,
          'text'=> $text,
          'date_created%sql'=> 'CURRENT_TIMESTAMP()',
          'task_due_date%dt'=> $task_due_date.' 23:59:59',
          'note%s'=> $note,
          'timestamp%sql'=> 'CURRENT_TIMESTAMP()',
          'flg_deleted%i' => 0
        );
        dibi::insert('task', $sql_task_arg)->execute();
        dibi::commit();
      } catch (\Exception $ex) {
        dibi::rollback();
      }
    }
    return $ret;
  }

  public function load_task_info($task_id){
    $tasks_sql = dibi::select('t.id, t.status, t.name, type.name as type_name, t.created_id as creator, t.flg_deleted as deleted, t.note, t.text, t.text_returned, t.text_reason, DATE_FORMAT(t.date_created, "%d-%m-%Y") as date_created, DATE_FORMAT(t.task_due_date, "%d-%m-%Y") as task_due_date')
    ->select('tf.flag_id')
    ->from('task t')
    ->leftJoin('task_flag tf')->on('t.id = tf.task_id')
    ->leftJoin('pa_flag pf')->on('pf.id = tf.flag_id')
    ->leftJoin('task_type type')->on('type.id = t.type_id')
    ->innerJoin('user u')->on('t.user_id = u.id')
    ->where('t.id = %i', $task_id);
    return $tasks_sql->fetchAll();
  }

  public function load_task_photos($task_id, $photos = array()){
    $photos_sql = dibi::select('id, path, file_name, lat, lng, note, altitude, bearing, photo_heading as azimuth, photo_angle, roll, pitch, orientation, horizontal_view_angle as hvangle, vertical_view_angle as vvangle, accuracy, concat(device_manufacture, " - ", device_model, " - ", device_platform, " - ", device_version) as device, sats_info, nmea_msg, network_info, distance, nmea_distance, date_format(timestamp, "%Y-%m-%d %H:%i:%s") as timestamp, date_format(created, "%Y-%m-%d %H:%i:%s") as created_date, rotation_correction as rotation, flg_checked_location, flg_original, efkLatGpsL1, efkLngGpsL1, efkAltGpsL1, date_format(efkTimeGpsL1, "%Y-%m-%d %H:%i:%s") as efkTimeGpsL1, efkLatGpsL5, efkLngGpsL5, efkAltGpsL5, date_format(efkTimeGpsL5, "%Y-%m-%d %H:%i:%s") as efkTimeGpsL5, efkLatGpsIf, efkLngGpsIf, efkAltGpsIf, date_format(efkTimeGpsIf, "%Y-%m-%d %H:%i:%s") as efkTimeGpsIf, efkLatGalE1, efkLngGalE1, efkAltGalE1, date_format(efkTimeGalE1, "%Y-%m-%d %H:%i:%s") as efkTimeGalE1, efkLatGalE5, efkLngGalE5, efkAltGalE5, date_format(efkTimeGalE5, "%Y-%m-%d %H:%i:%s") as efkTimeGalE5, efkLatGalIf, efkLngGalIf, efkAltGalIf, date_format(efkTimeGalIf, "%Y-%m-%d %H:%i:%s") as efkTimeGalIf')
    ->from('photo')->where('task_id = %i', $task_id)
    ->where('flg_deleted = 0');
    if (!empty($photos)){
      $photos_sql->where('id IN %in', $photos);
    }
    $photos_sql->orderBy('timestamp');
    return $photos_sql->fetchAll();
  }

  public function load_single_photo($photo_id){
    $photos_sql = dibi::select('path, file_name, lat, lng, note, altitude, bearing, photo_heading as azimuth, photo_angle, roll, pitch, orientation, horizontal_view_angle as hvangle, vertical_view_angle as vvangle, accuracy, concat(device_manufacture, " - ", device_model, " - ", device_platform, " - ", device_version) as device, sats_info, nmea_msg, network_info, distance, nmea_distance, date_format(timestamp, "%Y-%m-%d %H:%i:%s") as timestamp, date_format(created, "%Y-%m-%d %H:%i:%s") as created_date, rotation_correction as rotation, flg_checked_location, flg_original, efkLatGpsL1, efkLngGpsL1, efkAltGpsL1, date_format(efkTimeGpsL1, "%Y-%m-%d %H:%i:%s") as efkTimeGpsL1, efkLatGpsL5, efkLngGpsL5, efkAltGpsL5, date_format(efkTimeGpsL5, "%Y-%m-%d %H:%i:%s") as efkTimeGpsL5, efkLatGpsIf, efkLngGpsIf, efkAltGpsIf, date_format(efkTimeGpsIf, "%Y-%m-%d %H:%i:%s") as efkTimeGpsIf, efkLatGalE1, efkLngGalE1, efkAltGalE1, date_format(efkTimeGalE1, "%Y-%m-%d %H:%i:%s") as efkTimeGalE1, efkLatGalE5, efkLngGalE5, efkAltGalE5, date_format(efkTimeGalE5, "%Y-%m-%d %H:%i:%s") as efkTimeGalE5, efkLatGalIf, efkLngGalIf, efkAltGalIf, date_format(efkTimeGalIf, "%Y-%m-%d %H:%i:%s") as efkTimeGalIf')
    ->from('photo')->where('id = %i', $photo_id)->where('flg_deleted = 0');
    return $photos_sql->fetchAll();
  }

  public function accept_task_photos($task_id, $reload){
    $ret=array('error' => '0', 'errorText' => '', 'reload' => $reload);
    $fields_insert_array = array(
      'task_id%i' => $task_id,
      'flag_id%i' => self::VALID,
      'timestamp%sql' => 'CURRENT_TIMESTAMP()'
    );
    $fields_update_array = array(
      'status%s' => self::STATUS[3]
    );
    $photos_count = dibi::select('count(id) as photos')->from('photo')->where('task_id = %i', $task_id)->where('flg_deleted = 0')->fetchSingle();
    if ($photos_count === 0){
      $ret = array('error' => '1', 'errorText' => user_model::get_translate_from_db('task_photo_accept_error'), 'reload' => $reload);
    } else {
      try{
        dibi::begin();
        dibi::insert('task_flag', $fields_insert_array)->execute();
        dibi::update('task', $fields_update_array)->where('id = %i', $task_id)->execute();
        dibi::commit();
      }catch(Exception $ex){
        dibi::rollback();
      }
    }
    return $ret;
  }

  public function delete_task_photos($task_id, $farmer_id = 0){
    $ret=array('error' => '0', 'errorText' => '', 'farmer_id' => $farmer_id);
    $fields_update_array = array(
      'flg_deleted%i' => 1
    );
    $creator_id = dibi::select('created_id')->from('task')->where('id = %i', $task_id)->fetchSingle();
    if ($creator_id !== user_model::get_loged_user_id()){
      $ret = array('error' => '1', 'errorText' => user_model::get_translate_from_db('task_photo_delete_error'), 'farmer_id' => 0);
    } else {
      try{
        dibi::begin();
        dibi::update('task', $fields_update_array)->where('id = %i', $task_id)->execute();
        dibi::commit();
      }catch(Exception $ex){
        dibi::rollback();
      }
    }
    return $ret;
  }

  public function decline_task_photos($task_id, $text_dec=""){
    $fields_insert_array = array(
      'task_id%i' => $task_id,
      'flag_id%i' => self::INVALID,
      'timestamp%sql' => 'CURRENT_TIMESTAMP()'
    );
    $fields_update_array = array(
      'status%s' => self::STATUS[3],
      'text_reason%s' => $text_dec
    );
    try{
      dibi::begin();
      dibi::insert('task_flag', $fields_insert_array)->execute();
      dibi::update('task', $fields_update_array)->where('id = %i', $task_id)->execute();
      dibi::commit();
    }catch(Exception $ex){
      dibi::rollback();
    }
  }

  public function move_from_open_task_photos($task_id, $note){
    $ret=array('error' => '0', 'errorText' => '');
    $fields_update_array = array(
      'note%s' => $note,
      'status%s' => self::STATUS[2]
    );
    $photos_count = dibi::select('count(id) as photos')->from('photo')->where('task_id = %i', $task_id)->where('flg_deleted = 0')->fetchSingle();
    if ($photos_count === 0){
      $ret = array('error' => '1', 'errorText' => user_model::get_translate_from_db('task_move_from_open_error'));
    } else {
      try{
        dibi::begin();
        dibi::update('task', $fields_update_array)->where('id = %i', $task_id)->execute();
        dibi::commit();
      }catch(Exception $ex){
        dibi::rollback();
      }
    }
    return $ret;
  }

  public function return_task_photos($task_id, $text_ret=""){
    /*$fields_insert_array = array(
      'task_id%i' => $task_id,
      'flag_id%i' => self::RETURNED,
      'timestamp%sql' => 'CURRENT_TIMESTAMP()'
    );*/
    $fields_update_array = array(
      'status%s' => self::STATUS[5],
      'text_returned%s' => $text_ret
    );
    try{
      dibi::begin();
      //dibi::insert('task_flag', $fields_insert_array)->execute();
      dibi::update('task', $fields_update_array)->where('id = %i', $task_id)->execute();
      dibi::commit();
    }catch(Exception $ex){
      dibi::rollback();
    }
  }

  public function get_tasks_gps($search, $filter_type, $spec_ids=''){
    if($filter_type == self::FILTER_TASKS_DETAIL){
      return $this->get_all_task_gps($search, $spec_ids);
    } else if ($filter_type == self::FILTER_USERS_GALLERY){
      if (!empty($search)){
        return $this->get_all_users_unassigned_gps($search, $spec_ids);
      }
    }
    $user_role = user_model::get_user_role(user_model::get_loged_user_id());
    if($user_role == user_model::OFFICER_ROLE && index_view::AGENCY_FARMERS_PAGE_ID == $_SESSION['page_id']){
      $pa = user_model::get_agency_id(user_model::get_loged_user_id());

      $users = dibi::translate('SELECT id FROM user WHERE pa_id = %i', $pa);
      if($filter_type == self::FILTER_FARMERS && !empty($search)){
        $users = dibi::translate('SELECT id FROM user WHERE pa_id = %i AND (name LIKE %~like~ OR surname LIKE %~like~ OR identification_number LIKE %~like~)', $pa, $search, $search, $search);
      }

      $users_tasks = dibi::translate('SELECT id FROM task WHERE flg_deleted = 0 and user_id IN (%sql)', $users);
      return $this->get_photos_gps_info($users_tasks);
    }else{
      $filter = index_model::setListFilter($_SESSION['task_list_filter']['filter']);
      if (!empty($filter)){
        $filter.= " AND ";
      } else {
        $filter = "";
      }
      $user_id = empty($_SESSION['users_id_task']) ? user_model::get_loged_user_id() : $_SESSION['users_id_task'];
      $users_tasks = dibi::translate('SELECT t.id FROM task t LEFT JOIN task_flag tf ON t.id = tf.task_id WHERE '.$filter.' t.user_id = %i AND flg_deleted = 0 ', $user_id);
      if($filter_type == self::FILTER_TASKS && !empty($search)){
        $users_tasks = dibi::translate('SELECT t.id FROM task t LEFT JOIN task_flag tf ON t.id = tf.task_id WHERE '.$filter.' t.user_id = %i AND flg_deleted = 0 AND t.name LIKE %~like~', $user_id, $search);
      }
      return $this->get_photos_gps_info($users_tasks);
    }
  }

  public function get_polygons_gps($actualBounds){
    $polygons = array();
    $polygons = dibi::select('distinct id, identificator, wgs_geometry')->from('land')
    ->where('wgs_min_lat < ? and wgs_max_lat > ? and wgs_min_lng < ? and wgs_max_lng > ?', floatval($actualBounds['lat']['max']), floatval($actualBounds['lat']['min']), floatval($actualBounds['lng']['max']), floatval($actualBounds['lng']['min']))
    //->or('(wgs_min_lat < ? and wgs_min_lat > ? and wgs_min_lng < ? and wgs_min_lng > ?))', floatval($actualBounds['lat']['max']), floatval($actualBounds['lat']['min']), floatval($actualBounds['lng']['max']), floatval($actualBounds['lng']['min']))
    ->where('wgs_geometry is not null')
    ->fetchAll();
    return $polygons;
  }

  private function get_all_task_gps($task_id, $spec_ids=""){
    if (!empty($spec_ids)){
      return dibi::select('p.lat, p.lng, p.task_id, p.id as photo_id, t.status, flg.flag_id')
      ->from('photo p')->leftJoin('task_flag flg')->on('flg.task_id = p.task_id')->join('task t')->on('t.id = p.task_id')->where('p.task_id = %i', $task_id)->where('p.id in (%sql)',$spec_ids)->where('p.flg_deleted = 0')->fetchAll();
    } else {
      return dibi::select('p.lat, p.lng, p.task_id, p.id as photo_id, t.status, flg.flag_id')->from('photo p')->leftJoin('task_flag flg')->on('flg.task_id = p.task_id')->join('task t')->on('t.id = p.task_id')->where('p.task_id = %i', $task_id)->where('p.flg_deleted = 0')->fetchAll();
    }
  }

  private function get_all_users_unassigned_gps($user_id, $spec_ids=''){
    if (!empty($spec_ids)){
      return dibi::select('lat, lng, id as photo_id, task_id, "unassigned" as status, "0" as flag_id')
      ->from('photo')->where('id in (%sql)',$spec_ids)->where('lat is not null')->where('lng is not null')->where('task_id is null')->where('user_id = %i', $user_id)->where('flg_deleted = 0')->fetchAll();
    } else {
      return dibi::select('lat, lng, id as photo_id, task_id, "unassigned" as status, "0" as flag_id')
      ->from('photo')->where('lat is not null')->where('lng is not null')->where('task_id is null')->where('user_id = %i', $user_id)->where('flg_deleted = 0')->fetchAll();
    }
  }

  private function get_photos_gps_info($users_tasks){
    $photos = dibi::translate("SELECT MAX(id) as photo_id FROM photo WHERE task_id in (%sql) GROUP BY task_id", $users_tasks);

    return dibi::select('p.task_id, p.lat, p.lng, p.id as photo_id, t.status, flg.flag_id')
    ->from('photo p')->leftJoin('task_flag flg')->on('flg.task_id = p.task_id')->join('task t')->on('t.id = p.task_id')
    ->where('p.lat is not null')
    ->where('p.lng is not null')
    ->where('p.id in (%sql)', $photos)
    ->where('p.flg_deleted = 0')
    ->fetchAll();
  }

  public function get_task_azimuth($task_id){
    $azimuth_sql = dibi::select('photo_heading as magnetic_azimuth')
    ->from('photo')->where('task_id = %i', $task_id)->where('flg_deleted = 0')->orderBy('timestamp');
    return $azimuth_sql->fetchAll();
  }

  public function get_photo_azimuth($photo_id){
    $azimuth_sql = dibi::select('photo_heading as magnetic_azimuth')
    ->from('photo')->where('id = %i', $photo_id)->where('flg_deleted = 0');
    return $azimuth_sql->fetchAll();
  }

  public function get_task_photo_count($task_id){
    return dibi::select('COUNT(task_id)')->as('count')->from('photo')->where('task_id = %i', $task_id)->where('flg_deleted = 0')->fetchSingle();
  }

  public static function get_statuses_texts(){
    return dibi::select('template_param, %sql', $_SESSION['lang'])->from('page_lang')->where('page_id = %i', index_view::TASKS_PAGE_ID)->where('template_param = "ack" OR template_param = "decline" OR template_param = "returned" OR template_param = "wait"')->fetchPairs();
  }

  public function assign_photos($user_id, $photos, $task_id){
    $task_status = dibi::select('status')->from('task')->where('id = %i', $task_id)->fetchSingle();
    $fields_update_task_array = array();
    if (strtolower($task_status) == strtolower(self::STATUS[0])){
      $fields_update_task_array = array(
        'status%s' => self::STATUS[1]
      );
    }
    $fields_update_photos_array = array(
      'task_id%i' => $task_id
    );
    try{
      dibi::begin();
      if (!empty($fields_update_task_array)){
        dibi::update('task', $fields_update_task_array)->where('id = %i', $task_id)->execute();
      }
      foreach ($photos as $photo) {
        dibi::update('photo', $fields_update_photos_array)->where('id = %i', $photo)->execute();
      }
      dibi::commit();
    }catch(Exception $ex){
      dibi::rollback();
    }
  }

  public static function get_task_types(){
    return dibi::select('id, name')
    ->from('task_type')->fetchAll();
  }

  public static function photos_verified_status($task_id){
    $return = 'incomplete';
    $verified = true;
    $photos = dibi::select('id, flg_checked_location, flg_original')->from('photo')->where('task_id = %i', $task_id)->where('flg_deleted = 0')->fetchAll();
    foreach ($photos as $key => $photo) {
      if ($photo['flg_checked_location'] === 0){
        $return = 'not_verified';
        $verified = false;
      } else if (empty($photo['flg_checked_location'])){
        if ($return!='not_verified'){
          $return = 'incomplete';
        }
        $verified = false;
      } else {
        if ($verified){
          $return = 'verified';
        }
      }
      if ($photo['flg_original'] === 0){
        $return = 'not_verified';
        $verified = false;
      } else if (empty($photo['flg_original'])){
        if ($return!='not_verified'){
          $return = 'incomplete';
        }
        $verified = false;
      } else {
        if ($verified){
          $return = 'verified';
        }
      }
    }
    return $return;
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
