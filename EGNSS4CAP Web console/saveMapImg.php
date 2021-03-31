<?php
session_start();
if (isset($_SESSION['user_id'])){
  $image = $_POST['image'];
  $image_name = $_POST['name'];

  $location = "img/map_tmp/";
  $filename = $image_name.".png";
  $file = $location . $filename;
  $image_parts = explode(";base64,", $image);
  $image_base64 = base64_decode($image_parts[1]);


  if (file_put_contents($file, $image_base64)){
    echo $file;
  } else {
    echo "problem with uploading file! (map.png)";
  }
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
