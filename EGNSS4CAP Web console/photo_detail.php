<?php
session_start();
function correctImageOrientation($filename) {
  if (function_exists('exif_read_data')) {
    $exif = @exif_read_data($filename);
    if($exif && isset($exif['Orientation'])) {
      $orientation = $exif['Orientation'];
      $img = imagecreatefromjpeg($filename);
      if($orientation != 1){
        $deg = 0;
        switch ($orientation) {
          case 3:
            $deg = 180;
            break;
          case 6:
            $deg = 270;
            break;
          case 8:
            $deg = 90;
            break;
        }
        if ($deg) {
          $img = imagerotate($img, $deg, 0);
        }
      }
      return $img;
    }
  }
}

function getWidth($img) {
   return imagesx($img);
}

function getHeight($img) {
   return imagesy($img);
}

function resizeToHeight($height, $img) {
   $ratio = $height / getHeight($img);
   $width = getWidth($img) * $ratio;
   $new_image = imagecreatetruecolor($width, $height);
   imagecopyresampled($new_image, $img, 0, 0, 0, 0, $width, $height, getWidth($img), getHeight($img));
   return $new_image;
}

function setNewImageOrientation($filename, $targetRotation = 0, $just_img = false) {
  //$tmpfolder = "../../tmp/egnss4cap/photos/";
  $tmpfolder = "photos/tmp/";
  if (file_exists ($filename)){
    $img = correctImageOrientation($filename);
    $img = imagerotate($img, -$targetRotation, 0);
    if ($just_img){
      $img = resizeToHeight(600, $img);
      imagejpeg($img);
      exit;
    }
    $newFilename = $tmpfolder."tmp_photo-".rand(999,100000).".jpg";
    clearTmpFolder($tmpfolder);
    if (imagejpeg($img, $newFilename, 95)) { return $newFilename; }
  }
  return $filename;
}

function clearTmpFolder($folder){
  $files = glob($folder."*");
  $now   = time();

  foreach ($files as $file) {
    if (is_file($file)) {
      if ($now - filemtime($file) >= 60 * 60 * 24 * 2) { // 2 days
        unlink($file);
      }
    }
  }
}

//if user is not logged in -> go to login page
if (!isset($_SESSION['user_id']) && (!isset($_GET['justimg']) || $_GET['justimg'] == '0')) { header("Location: index.php"); }

if (isset($_GET['img']) && isset($_GET['rotation'])){
  $src = $_GET['img'];
  $rotation = $_GET['rotation'];
  $just_img = (isset($_GET['justimg']) && $_GET['justimg'] == '1')?true:false;

  if ($just_img){
    Header("Content-type: image/jpeg");
    setNewImageOrientation($src, $rotation, $just_img);
  } else if (!empty($rotation) && $rotation != 0){
    $src = setNewImageOrientation($src, $rotation);
  }

?>

<script type="text/javascript" src="vendor\components\jquery\jquery.min.js?v=1.0.0."></script>

<script>
$(document).ready(function(){

  $('#zoomin').on('click',function(){
    $currwidth = parseInt($("#zoombox").width());
    $currwidth += 100;
    $("#zoombox").css('width', $currwidth+'px');
  });

  $('#zoomout').on('click',function(){
    $currwidth = parseInt($("#zoombox").width());
    $currwidth -= 100;
    $("#zoombox").css('width', $currwidth+'px');
  });

  $('#zoomreset').on('click',function(){
    $("#zoombox").css('width', 'auto');
  });
});

</script>
<style>
  button{ border-radius: 7px;padding: 3px 6px;border: 1px solid #787878;background: white;cursor: pointer;transition: 0.2s ease;user-select: none;}
  button:hover { background: #ccffcc; }
</style>
<div style="height:50px; width:100%; height: 100%; display:flex; justify-content: center; align-items:flex-end;position:fixed;z-index:100;">
  <!--For zoom in use CTRL + "+". For zoom out use CTRL + "-". For reset zoom use CTRL + "0"-->
  <div style="background: rgba(255,255,255,0.6);padding: 20px 40px;border-radius: 10px 10px 0 0;box-shadow: 0 0 3px;">
    <button id="zoomin">Zoom in</button>
    <button id="zoomout">Zoom out</button>
    <button id="zoomreset">Reset zoom</button>
  </div>
</div>
<div style="display: table; text-align: center; width: 100%; height:100%;">
  <div style="display:flex; justify-content: center; align-items: center; width:100%; height: 100%;">
    <img id="zoombox" style="margin-top: 0%; width: auto;" src="<?=$src?>">
  </div>
</div>

<?php }
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
?>
