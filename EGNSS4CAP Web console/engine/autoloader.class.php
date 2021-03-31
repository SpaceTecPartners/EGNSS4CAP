<?php

class autoloader
{

  final public function __construct()
  {
    throw new LogicException('Cannot instantiate static class ' . get_class($this));
  }

  private const not_load = array('dibi');

  private static function autoload($class)
  {
    $class_namespaces = explode(DIRECTORY_SEPARATOR, $class);
    if (in_array($class_namespaces[count($class_namespaces) - 1], self::not_load)) return true;
    $class = str_replace('\\', '/', $class);
    $required_file = INCLUDE_PATH . 'engine/class/' . $class . '.class.php';
    if (!file_exists($required_file))
    {
      $interface = INCLUDE_PATH . 'engine/class/' . $class . '.interface.php';
      if (file_exists($interface))
        require_once($interface);
    } else {
      require_once($required_file);
    }
    return true;
  }

  public static function register()
  {
    try {
      spl_autoload_register(array(__class__, 'autoload'), true);
    } catch (Exception $e) {
      var_dump($e);
    }
  }

}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
