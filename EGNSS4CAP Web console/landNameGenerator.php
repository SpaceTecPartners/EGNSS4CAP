<?php
if (isset($_GET['land'])){
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

  //isset($_GET['font'])?$font = dirname(__FILE__) ."/fonts/".$_GET['font']:$font = dirname(__FILE__) ."/fonts/CooperHewitt-Medium.otf";
  $font = dirname(__FILE__)."/fonts/ArialCE.ttf";
  // set text
  //ImageString($image,$textHeight,0,0,$text,$textColour);
  imagettftext($image, $fontSize, 0, 0, $textHeight, $textColour, $font, $text);
  // set correct header
  header('Content-type: image/png');

  // create image
  echo ImagePNG($image);
}
