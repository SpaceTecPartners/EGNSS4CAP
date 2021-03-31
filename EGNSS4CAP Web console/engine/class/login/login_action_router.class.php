<?php
namespace login;

class login_action_router extends \action_router{
  protected $actions = array(
    'login',
    'logout',
    'login_fail',
    'lang_en',
    'lang_cz',
  );

  public function __construct(){
    parent::__construct($this->actions);
  }

  protected function login($params){
    $model =  new login_model();
    $login = isset($params['login']) ? $params['login'] : "";
    $pass = isset($params['pass']) ? sha1($params['pass']) : "";
    $success = $model->verify_user_login($login, $pass);
    if($success) header('Location: ' . SYSTEM_HOST . '/index.php');
    else header('Location: ' . SYSTEM_HOST . '/login.php?act=login_fail');
    exit;
  }

  protected function logout($params){
    login_model::logout();
    exit;
  }

  protected function login_fail($params){
    $template = new login_view();
    echo $template->load_login_html_page(array('fail'=>true));
    exit;
  }

  protected function lang_en($params){$_SESSION['lang'] = 'en';}
  protected function lang_cz($params){$_SESSION['lang'] = 'cz';}

}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
