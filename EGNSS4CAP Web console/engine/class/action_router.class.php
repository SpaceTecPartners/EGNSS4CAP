<?php

class action_router{

  private $actions = array(
  );

  public function __construct($actions = array())
  {
    $this->extend_actions($actions);
  }

  protected function extend_actions($actions){
    $this->actions = array_merge($this->actions, $actions);
  }

  public function redirect($filename)
  {
    if (!headers_sent($f, $ln)) {
      header('Location: ' . $filename, true, 303);
      die;
    } else {
      echo '<script type="text/javascript">';
      echo 'window.location.href="' . $filename . '";';
      echo '</script>';
      echo '<noscript>';
      echo '<meta http-equiv="refresh" content="0;url=' . $filename . '" />';
      echo '</noscript>';
    }
  }

  public function take_action($act, $params)
  {
    if (in_array($act, $this->actions)) {
      call_user_func_array(array($this, $act), $params);
    } else {
      $this->default_action($act, $params);
    }
  }

  protected function default_action($act, $params)
  {
    var_dump($act, $params);
    die('Uknown action fired!');
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
