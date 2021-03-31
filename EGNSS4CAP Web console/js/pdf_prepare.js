const PDF_PREPARE = true;

var map_property = {zoom: 16, popup_bool: true};
var photos = [];
var cur_photo = 0;
var map_prefix = "";
var maps_done = false;

$(document)
  .ready(function() {

    console.log('ready');
    disable_generate_button();
    photos = $('.js_photos_input').val().split(',');
    $('.js_photo_ids').val(photos[0]);
    //initMap("map_for-"+photos[0]);

  })
  .on('click', "#js_confirm_pdf_generate", function(){
    $("#process_counter_max").text(photos.length);
    disable_generate_button();
    map_prefix = Math.random().toString(36).substr(2, 20);
    getMapImg();
  })
function show_loader(){
  $(".pdf_loader").show();
}
function hide_loader(){
  $(".pdf_loader").hide();
}
function disable_generate_button(){
  show_loader();
  $("#js_confirm_pdf_generate").prop("disabled", true);
  $("#js_confirm_pdf_generate").css("cursor","wait");
}

function enable_generate_button(){
  setTimeout(function () {
    hide_loader();
    $("#js_confirm_pdf_generate").prop("disabled", false);
    $("#js_confirm_pdf_generate").removeAttr("disabled")
    $("#js_confirm_pdf_generate").css("cursor","pointer");
  }, 1000);
}

function clear_vars_pdf(){
  map = ""; //google map api

  markers = []; //point of interest
  popups = [];
  markerCluster = "";
  popup_photo = "";
  infoWindow = "";
  coordinates = "";

  lands = [];
  landNames = [];
  polygons = [];

  paths = [];
  pathNames = [];
  path_points = [];
  path_points_circles = [];

}

function getMapImg($mapId=0){
    //blocl only for google map//
    //get transform value
    var transform = $(".gm-style>div:first>div:first>div:last>div").css("transform");
    var comp = transform.split(","); //split up the transform matrix
    var mapleft = parseFloat(comp[4]); //get left value
    var maptop = parseFloat(comp[5]);  //get top value
    $(".gm-style>div:first>div:first>div:last>div").css({ //get the map container
      "transform": "none",
      "left": mapleft,
      "top": maptop,
    });
    //endblock only for google map//
    //specify element for screenshot
    if ($mapId === 0){
      cur_photo = 0;// set to default - starting of generate
      $mapId = "map_for-"+photos[0];
    }
    $("#process_counter_cur").text(cur_photo+1);

    html2canvas($("#"+$mapId)[0],{ useCORS: true }).then(function(canvas) {
      var dataUrl= canvas.toDataURL("image/png");
      console.log(canvas.width);
      console.log(canvas.height);
      console.log(dataUrl);
      //location.href=dataUrl;
      var map_name = map_prefix+"-"+photos[cur_photo];
      var base64URL = canvas.toDataURL('image/jpeg').replace('image/jpeg', 'image/octet-stream');
      $.post({
        url: 'saveMapImg.php',//some php upload url
        data: {image: base64URL, name: map_name},
        success: function(data){
           console.log('Upload successfully');
        }
      });
      //block only for google map//
      $(".gm-style>div:first>div:first>div:last>div").css({
        left:0,
        top:0,
        "transform":transform
      });
      //endblock only for google map//
      if (photos[cur_photo+1] !== undefined){//continue generating
        cur_photo++;
        getMapImg("map_for-"+photos[cur_photo]);
      } else {//maps are complete -> run PDF
        setTimeout(function () {
          var url = $("#js_pdf_create_url").val();
          window.open(url + map_prefix, '_blank');
          enable_generate_button();
        }, 1000);
      }
    });
}
//Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
