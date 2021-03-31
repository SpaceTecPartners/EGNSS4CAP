<?php
use task\task_view;
use task\task_model;
use user\user_model;

require_once '_includes.php';
$_SESSION['page_id'] = 91;

function create_pdf($task_id, $user_id, $photos = false, $map_prefix = ""){
  require_once __DIR__.'/vendor/autoload.php';

  $template = new task_view();
  $task_model = new task_model();
  $e_photos; $a_photos;
  $photos_array = array();
  if ($photos !== false){
    $photos_array = explode(',',$photos);
  }
  if ($task_id == 0){
    $template_array = array(
      'unassigned' => true,
      'user' => user_model::get_users_credencials($user_id),
      'images' => user_model::get_users_gallery_unassigned($user_id, $photos_array),
      'task' => array(),
      'tasks_verified' => "not_verified",
      'map_prefix' => $map_prefix
    );
    if (!empty($photos_array)){
      $e_photos = count($template_array['images']);
      $a_photos = count(user_model::get_users_gallery_unassigned($user_id));
    } else {
      $e_photos = $a_photos = count($template_array['images']);
    }
    $header_text = $template_array['user']['name'].' '.$template_array['user']['surname'].' - '.user_model::get_translate_from_db('heading');
  } else {
    $template_array = array(
      'unassigned' => false,
      'user' => user_model::get_users_credencials($user_id),
      'images' => $task_model->load_task_photos($task_id, $photos_array),
      'task' => $task_model->load_task_info($task_id)[0],
      'tasks_verified' => $task_model->photos_verified_status($task_id),
      'map_prefix' => $map_prefix
    );
    if (!empty($photos_array)){
      $e_photos = count($template_array['images']);
      $a_photos = count($task_model->load_task_photos($task_id));
    } else {
      $e_photos = $a_photos = count($template_array['images']);
    }
    $header_text = $template_array['user']['name'].' '.$template_array['user']['surname'].' - '.user_model::get_translate_from_db('header').' '.$template_array['task']['name'];
  }

  $export_date = user_model::get_translate_from_db('export_date').': '.date('Y-m-d H:i:s');
  $export_photos_count = user_model::get_translate_from_db('photo_exported').' '.$e_photos.' '.user_model::get_translate_from_db('photo_out_of').' '.$a_photos.' '.user_model::get_translate_from_db('photo_photo');

  try {
      $mpdf = new \Mpdf\Mpdf(['tempDir' => '../../tmp/egnss4cap', 'mode' => 'utf-8', 'format' => 'A4', 'default_font_size' => 12, 'default_font' => 'Menlo', 'margin_left' => 5, 'margin_right' => 5, 'margin_top' => 25, 'margin_bottom' => 22, 'margin_header' => 5, 'margin_footer' => 5]);
      $mpdf->shrink_tables_to_fit = 1;
      $mpdf->img_dpi = 300;
      $mpdf->SetTitle(date("Y.m.d").'_'.$header_text.'.pdf');

      $header='
      <div class="pdf_top_header">
          <table class="pdf_top_header_table text">
              <tr>
                  <td class="pdf_top_header_table_td1"><img class="pdf_top_header_logo_img" src="img/logo.png"/></td>
                  <td class="pdf_top_header_table_td2">EGNSS4CAP export</td>
                  <td class="pdf_top_header_table_td3">'.$header_text.'</td>
              </tr>
          </table>
      </div>
      <div class="pdf_second_header">
        <span>'.$export_date.'</span><br>
        <span>'.$export_photos_count.'</span>
      </div>
      ';
      $footer='
      <div class="pdf_footer">
          <table class="pdf_footer_table">
              <tr>
                  <td class="pdf_footer_table_td">{PAGENO}/{nbpg}</td>
              </tr>
          </table>
      </div>
      ';
      $mpdf->SetHTMLHeader($header);
      $mpdf->SetHTMLFooter($footer);

      $content = $template->load_pdf_photos_html($template_array);
      $mpdf->WriteHTML($content);

      foreach ($photos_array as $photo) {
        if (file_exists("img/map_tmp/".$map_prefix."-".$photo.".png")){
          unlink("img/map_tmp/".$map_prefix."-".$photo.".png");
        }
      }
      if (isset($_GET['raw']) && $_GET['raw'] == '1'){
        echo $content;
      } else {
        $mpdf->Output(date("Y.m.d").'_'.$header_text.'.pdf','I');
      }
  } catch (\Mpdf\MpdfException $e) {
      echo $e->getMessage();
  }
}

function open_prepare_pdf($task_id, $user_id, $photos = false){
  require_once __DIR__.'/vendor/autoload.php';

  $template = new task_view();
  $task_model = new task_model();
  $photos_array = array();
  if ($photos !== false){
    $photos_array = explode(',',$photos);
  } else {
    $photos = "";
  }

  if ($task_id == 0){
    $template_array = array(
      'mapjs_filter_type' => 'users_gallery',
      'unassigned' => true,
      'user' => user_model::get_users_credencials($user_id),
      'images' => user_model::get_users_gallery_unassigned($user_id, $photos_array),
      'task' => array(),
      'tasks_verified' => "not_verified",
      'search_string' => $user_id,
      'photos_string' => $photos,
      "pdf_url" => "pdf_export.php?act=create&user_id=".$user_id."&task_id=0&specified_photos=".$photos."&map_prefix="
    );
  } else {
    $template_array = array(
      'mapjs_filter_type' => 'task_detail',
      'unassigned' => false,
      'user' => user_model::get_users_credencials($user_id),
      'images' => $task_model->load_task_photos($task_id, $photos_array),
      'task' => $task_model->load_task_info($task_id)[0],
      'tasks_verified' => $task_model->photos_verified_status($task_id),
      'search_string' => $task_id,
      'photos_string' => $photos,
      "pdf_url" => "pdf_export.php?act=create&user_id=".$user_id."&task_id=".$task_id."&specified_photos=".$photos."&map_prefix="
    );
  }

  echo $template->load_prepare_pdf_photos_html($template_array);
}

if(isset($_GET['act']) && isset($_GET['task_id']) && isset($_GET['user_id'])){
  $photos = false;
  if (isset($_GET['specified_photos'])){ $photos = $_GET['specified_photos']; }
  if ($_GET['act'] == "prepare"){
    open_prepare_pdf($_GET['task_id'], $_GET['user_id'], $photos);
  } else if ($_GET['act'] == "create"){
    create_pdf($_GET['task_id'], $_GET['user_id'], $photos, $_GET['map_prefix']);
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
