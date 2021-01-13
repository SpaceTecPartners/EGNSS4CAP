<?php



require_once('_includes.php');

var_dump(dibi::select('*')->from('user')->fetchAll());

?>