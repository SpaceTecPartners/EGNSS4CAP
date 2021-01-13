<?php
error_reporting(0);
use task\task_view;
use task\task_model;
use user\user_model;

require_once '_includes.php';

function create_pdf($task_id, $user_id){
  require_once __DIR__.'/vendor/autoload.php';

  $template = new task_view();
  $task_model = new task_model();
  if ($task_id == 0){
    $template_array = array(
      'unassigned' => true,
      'user' => user_model::get_users_credencials($user_id),
      'images' => user_model::get_users_gallery_unassigned($user_id),
      'task' => array(),
      'tasks_verified' => "not_verified"
    );
    $header_text = $template_array['user']['name'].' '.$template_array['user']['surname'].' - '.user_model::get_translate_from_db('heading');
  } else {
    $template_array = array(
      'unassigned' => false,
      'user' => user_model::get_users_credencials($user_id),
      'images' => $task_model->load_task_photos($task_id),
      'task' => $task_model->load_task_info($task_id)[0],
      'tasks_verified' => $task_model->photos_verified_status($task_id)
    );
    $header_text = $template_array['user']['name'].' '.$template_array['user']['surname'].' - '.user_model::get_translate_from_db('header').' '.$template_array['task']['name'];
  }

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
                  <td class="pdf_top_header_table_td2">EGNSS4CAP</td>
                  <td class="pdf_top_header_table_td3">'.$header_text.'</td>
              </tr>
          </table>
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
//      echo $content;

      $mpdf->Output(date("Y.m.d").'_'.$header_text.'.pdf','I');
  } catch (\Mpdf\MpdfException $e) {
      echo $e->getMessage();
  }
}

if(isset($_GET['task_id']) && isset($_GET['user_id'])){
  create_pdf($_GET['task_id'],$_GET['user_id']);
}
