<?php
session_start();
if (isset($_GET['land']) && isset($_SESSION['user_id'])){
  $text = $_GET['land'];
  $textLength = strlen($text);
  if (isset($_GET['zoom']) ){
    $zoom = $_GET['zoom'];
    if ($zoom == 15){
      $fontSize = 8;
    } else if ($zoom > 15 && $zoom < 18) {
      $fontSize = 10;
    } else {
      $fontSize = 12;
    }
  } else {
    $fontSize = 12;
  }

  $textHeight = $fontSize*1.25;

  // create image handle
  $image = ImageCreate($textLength*($textHeight-($fontSize/2)),$textHeight*1.3);

  // set colours
  $backgroundColour = imagecolorallocatealpha($image,255,255,255,0); // transparent
  isset($_GET['color'])?$fontColors=explode("|", $_GET['color']):$fontColors=array(120,120,120);
  $textColour = ImageColorAllocate($image,$fontColors[0],$fontColors[1],$fontColors[2]);

  $font = dirname(__FILE__)."/fonts/arial.ttf";
  // set text
  imagettftext($image, $fontSize, 0, 0, $textHeight, $textColour, $font, $text);
  // set correct header
  header('Content-type: image/png');

  // create image
  echo ImagePNG($image);
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
