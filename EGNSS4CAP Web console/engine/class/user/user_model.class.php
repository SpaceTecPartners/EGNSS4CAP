<?php

namespace user;

use dibi;

class user_model{

  public const FARMER_ROLE = 1;
  public const OFFICER_ROLE = 2;
  public const SUPERADMIN_ROLE = 3;

  public static function get_user_role(int $id){
    return dibi::select('role_id')->from('user_role')->where('user_id = %i', $id)->fetchSingle();
  }

  public static function get_users_credencials(int $id){
    return dibi::select('surname, name, id')->from('user')->where('id = %i', $id)->fetch();
  }

  public static function is_agency(){
    $role = SELF::get_user_role(SELF::get_loged_user_id());
    if ($role == SELF::OFFICER_ROLE){
      return true;
    }
    return false;
  }

  public static function get_users_gallery_unassigned(int $id, $photos = array()){
    $photos_sql = dibi::select('concat(p.path, p.file_name) as src, p.path, p.file_name, p.id, p.note, lat, lng, altitude, bearing, photo_angle, photo_heading as azimuth, roll, pitch, orientation, horizontal_view_angle as hvangle, vertical_view_angle as vvangle, accuracy, concat(device_manufacture, " - ", device_model, " - ", device_platform, " - ", device_version) as device, sats_info, nmea_msg, network_info, distance, nmea_distance, date_format(timestamp, "%Y-%m-%d %H:%i:%s") as timestamp, date_format(created, "%Y-%m-%d %H:%i:%s") as created_date, rotation_correction as rotation, flg_checked_location, flg_original, efkLatGpsL1, efkLngGpsL1, efkAltGpsL1, date_format(efkTimeGpsL1, "%Y-%m-%d %H:%i:%s") as efkTimeGpsL1, efkLatGpsL5, efkLngGpsL5, efkAltGpsL5, date_format(efkTimeGpsL5, "%Y-%m-%d %H:%i:%s") as efkTimeGpsL5, efkLatGpsIf, efkLngGpsIf, efkAltGpsIf, date_format(efkTimeGpsIf, "%Y-%m-%d %H:%i:%s") as efkTimeGpsIf, efkLatGalE1, efkLngGalE1, efkAltGalE1, date_format(efkTimeGalE1, "%Y-%m-%d %H:%i:%s") as efkTimeGalE1, efkLatGalE5, efkLngGalE5, efkAltGalE5, date_format(efkTimeGalE5, "%Y-%m-%d %H:%i:%s") as efkTimeGalE5, efkLatGalIf, efkLngGalIf, efkAltGalIf, date_format(efkTimeGalIf, "%Y-%m-%d %H:%i:%s") as efkTimeGalIf')
    ->from('photo p')
    ->where('p.user_id = %i', $id)
    ->where('p.flg_deleted = 0')
    ->where('p.task_id is null');
    if (!empty($photos)){
      $photos_sql->where('p.id IN %in', $photos);
    }
    $photos_sql->orderBy('created_date DESC');
    return $photos_sql->fetchAll();
  }

  public static function get_users_tasks(int $id){
    $tasks_sql = dibi::select('t.name, t.id')
    ->from('task t')
    ->where('t.user_id = %i', $id)
    ->and('(t.status = %s or t.status = %s)', 'new', 'open')
    ->and('flg_deleted = 0')
    ->orderBy('t.timestamp');
    return $tasks_sql->fetchAll();
  }

  public static function get_users_paths(int $id){
    $ret = array();
    $paths = dibi::select('id, name, start, end, area, concat(device_manufacture, " - ", device_model, " - ", device_platform, " - ", device_version) as device')
    ->from('path')
    ->where('user_id = %i', $id)
    ->and('flg_deleted = 0')
    ->orderBy('timestamp')->fetchAll();

    foreach ($paths as $path){
      $ret[$path['id']] = array(
        "id" => $path['id'],
        "name" => $path['name'],
        "area" => $path['area'],
        "device" => $path['device'],
        "start" => date_format($path['start'], 'Y-m-d H:i:s'),
        "end" => date_format($path['end'], 'Y-m-d H:i:s'),
        "points" => SELF::get_path_points($path['id']),
        "point_labels" => SELF::get_path_points_labels(),
      );
    }
    return $ret;
  }

  public static function get_path_points(int $id){
    return dibi::select('lat, lng, altitude, accuracy, date_format(created, "%Y-%m-%d %H:%i:%s") as created_date')
    ->from('path_point')
    ->where('path_id = %i', $id)
    ->orderBy('created')->fetchAll();
  }

  public static function get_path_points_labels(){
    return $label_array = array(
      "point" => SELF::get_translate_from_db("path_popup_point"),
      "path" => SELF::get_translate_from_db("path_popup_path"),
      "lat" => SELF::get_translate_from_db("path_popup_lat"),
      "lng" => SELF::get_translate_from_db("path_popup_lng"),
      "accu" => SELF::get_translate_from_db("path_popup_accu"),
      "alt" => SELF::get_translate_from_db("path_popup_alt"),
      "created" => SELF::get_translate_from_db("path_popup_created")
    );
  }

  public static function generate_path_kml_file(int $id, $name, $desc=""){
    $xml="";
    if ($id){
      $points = SELF::get_path_points($id);
      $xml = "<?xml version='1.0' encoding='UTF-8'?>";
      $xml.= "<kml xmlns='http://earth.google.com/kml/2.1'>";
      $xml.= "<Folder>";
        $xml.= "<Placemark id='path-".$id."'>";
          $xml.= "<name>".$name."</name>";
          $xml.= "<description>".$name." - ".$desc." m²</description>";
          $xml.= "<LineString>";
            $xml.= "<extrude>1</extrude>";
            $xml.= "<altitudeMode>relativeToGround</altitudeMode>";
            $xml.= "<coordinates>";
            foreach($points as $point){
              $point_string = $point->lng.",".$point->lat." ";
              $xml.= $point_string;
            }
            $xml.= "</coordinates>";
          $xml.= "</LineString>";
         $xml.= "</Placemark>";
        $xml.= "</Folder>";
      $xml.= "</kml>";
    }
    return $xml;
  }

  public static function delete_user_path($path_id, $reload){
    $ret=array('error' => '0', 'errorText' => '', 'reload' => $reload);
    try{
      dibi::begin();
      $path_update_array = array(
        'flg_deleted%i' => 1
      );
      dibi::update('path', $path_update_array)->where('id = %i', $path_id)->execute();
      dibi::commit();
    }catch(\Exception $ex){
      dibi::rollback();
    }
    return $ret;
  }

  public static function get_agency_id(int $user_id){
    return dibi::select('pa_id')->from('user')->where('id = %i', $user_id)->fetchSingle();
  }

  public static function add_new_user($agency_id, $login, $paswd, $name, $surname, $ident_num, $email, $vat, $role){
    try{
      dibi::begin();
      $user_sql_args = array(
        'pa_id%i'=>$agency_id,
        'login%s'=>$login,
        'pswd%s'=>sha1($paswd),
        'name%s'=>$name,
        'surname%s'=>$surname,
        'identification_number%i'=>empty($ident_num) ? null : $ident_num,
        'email%s'=>$email,
        'vat%s'=>$vat,
        'timestamp%sql'=>'CURRENT_TIMESTAMP()'
      );
      dibi::insert('USER', $user_sql_args)->execute();
      $user_role_sql_args = array(
        'user_id%i'=> dibi::getInsertId(),
        'role_id%i'=> $role,
        'timestamp%sql'=> 'CURRENT_TIMESTAMP()'
      );
      dibi::insert('USER_ROLE', $user_role_sql_args)->execute();
      dibi::commit();
    }catch(\Exception $ex){
      dibi::rollback();
    }
  }

  public static function edit_user($agency_id, $id, $paswd, $name, $surname, $ident_num, $email, $vat, $role){
    try{
      dibi::begin();
      $user_update_array = array(
        'name%s'=>$name,
        'surname%s'=>$surname,
        'identification_number%i'=>empty($ident_num) ? null : $ident_num,
        'email%s'=>$email,
        'vat%s'=>$vat
      );
      if(!empty($paswd)){
        $user_update_paswd_array = array(
          'pswd%s'=>sha1($paswd),
        );
        $user_update_array = array_merge($user_update_array, $user_update_paswd_array);
      }
      dibi::update('user', $user_update_array)->where('id = %i', $id)->execute();
      dibi::commit();
    }catch(\Exception $ex){
      dibi::rollback();
    }
  }

  public static function get_users_credencials_from_task($taks_id){
    return dibi::select('u.surname, u.name, t.name as task_name')
    ->from('task t')
    ->innerJoin('user u')->on('t.user_id = u.id')
    ->where('t.id = %i', $taks_id)
    ->groupBy('u.id')
    ->fetch();
  }

  public static function get_users_credencials_from_photo($photo_id){
    return dibi::select('u.surname, u.name')
    ->from('photo p')
    ->innerJoin('user u')->on('p.user_id = u.id')
    ->where('p.id = %i', $photo_id)
    ->groupBy('u.id')
    ->fetch();
  }

  public static function get_loged_user_id(){
    if (isset($_SESSION['user_id'])) return $_SESSION['user_id']; else return -1;
  }

  public static function get_user_name(){
    return $_SESSION['user_name'];
  }

  public static function get_translate_from_db($tmp_param){
    return dibi::select($_SESSION['lang'])->from('page_lang')->where('template_param = %s', $tmp_param)->where('page_id = %i', $_SESSION['page_id'])->fetchSingle();
  }

  public static function save_photo_rotation_to_db($photo_id, $rotation){
    try{
      dibi::begin();
      $photo_update_array = array(
        'rotation_correction%i'=>$rotation
      );
      dibi::update('photo', $photo_update_array)->where('id = %i', $photo_id)->execute();
      dibi::commit();
    }catch(\Exception $ex){
      dibi::rollback();
    }
    return true;
  }
  public static function delete_unassigned_photo($photo_id, $reload){
    $ret=array('error' => '0', 'errorText' => '', 'reload' => $reload);
    try{
      dibi::begin();
      $photo_update_array = array(
        'flg_deleted%i' => 1
      );
      dibi::update('photo', $photo_update_array)->where('id = %i', $photo_id)->execute();
      dibi::commit();
    }catch(\Exception $ex){
      dibi::rollback();
    }
    return $ret;
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
