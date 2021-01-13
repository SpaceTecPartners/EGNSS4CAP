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
    $class = str_replace('\\', '/', $class);  //hrube reseni, bude to chtit osetrit
    $required_file = INCLUDE_PATH . 'engine/class/' . $class . '.class.php'; //klasika chceme class
    if (!file_exists($required_file)) // možná jsme chtěli interface
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
?>