var map; //google map api
var coordinates; //from DB lat lng from photos
var markers = []; //point of interest
var popups = [];
var markerCluster;
let popup_photo;
let infoWindow;
var lands = [];
var landNames = [];
var polygons = [];

var paths = [];
var pathNames = [];
var path_points = [];


if(!map_property){
 var map_property = {zoom: 4, popup_bool: false};
}

async function initMap() {
  clear_vars();
  if (FILTER_TYPE != 'user_paths'){
    await load_tasks_gps_points();
  }
  let map_coordinate = coordinates[0];

  google.maps.Polygon.prototype.getBoundingBox = function() {
    var bounds = new google.maps.LatLngBounds();  this.getPath().forEach(function(element,index) {
      bounds.extend(element)
    });  return(bounds);
  };
  // Add center calculation method
  google.maps.Polygon.prototype.getApproximateCenter = function() {
      var boundsHeight = 0,
          boundsWidth = 0,
          centerPoint,
          heightIncr = 0,
          maxSearchLoops,
          maxSearchSteps = 10,
          n = 1,
          northWest,
          polygonBounds = this.getBoundingBox(),
          testPos,
          widthIncr = 0;

      // Get polygon Centroid
      centerPoint = polygonBounds.getCenter();

      if (google.maps.geometry.poly.containsLocation(centerPoint, this)) {
          // Nothing to do Centroid is in polygon use it as is
          return centerPoint;
      } else {
          maxSearchLoops = maxSearchSteps / 2;

          // Calculate NorthWest point so we can work out height of polygon NW->SE
          northWest = new google.maps.LatLng(polygonBounds.getNorthEast().lat(), polygonBounds.getSouthWest().lng());

          // Work out how tall and wide the bounds are and what our search increment will be
          boundsHeight = google.maps.geometry.spherical.computeDistanceBetween(northWest, polygonBounds.getSouthWest());
          heightIncr = boundsHeight / maxSearchSteps;
          boundsWidth = google.maps.geometry.spherical.computeDistanceBetween(northWest, polygonBounds.getNorthEast());
          widthIncr = boundsWidth / maxSearchSteps;

          // Expand out from Centroid and find a point within polygon at 0, 90, 180, 270 degrees
          for (; n <= maxSearchLoops; n++) {
              // Test point North of Centroid
              testPos = google.maps.geometry.spherical.computeOffset(centerPoint, (heightIncr * n), 0);
              if (google.maps.geometry.poly.containsLocation(testPos, this)) {
                  break;
              }

              // Test point East of Centroid
              testPos = google.maps.geometry.spherical.computeOffset(centerPoint, (widthIncr * n), 90);
              if (google.maps.geometry.poly.containsLocation(testPos, this)) {
                  break;
              }

              // Test point South of Centroid
              testPos = google.maps.geometry.spherical.computeOffset(centerPoint, (heightIncr * n), 180);
              if (google.maps.geometry.poly.containsLocation(testPos, this)) {
                  break;
              }

              // Test point West of Centroid
              testPos = google.maps.geometry.spherical.computeOffset(centerPoint, (widthIncr * n), 270);
              if (google.maps.geometry.poly.containsLocation(testPos, this)) {
                  break;
              }
          }

          return(testPos);
      }
  };

  if(map_coordinate){
    map = new google.maps.Map(document.getElementById('map'),{
      zoom: map_property.zoom,
      center: {lat:map_coordinate.lat, lng:map_coordinate.lng}
    });
  }else{
    map = new google.maps.Map(document.getElementById('map'),{
      zoom: 4,
      center: {lat: 49.8037633, lng: 15.4749126} //cze gps
    });
  }

  if (map_property.popup_bool){
    map.addListener('tilesloaded', function() {
        var zoom = map.getZoom();
        if (zoom >= 17){
          markerCluster.setMap(null);
          for (i = 0; i < markers.length; i++) {
            markers[i].setMap(null);
          }
          for (i = 0; i < popups.length; i++) {
            popups[i].setMap(map);
          }
        } else {
          markerCluster.setMap(map);
          /*for (i = 0; i < markers.length; i++) {
            markers[i].setMap(map);
          }*/
          for (i = 0; i < popups.length; i++) {
            popups[i].setMap(null);
          }
        }
    });
  }
  map.addListener('idle', async function() {
    if (FILTER_TYPE != 'user_paths'){
      if (map.getZoom() >= 16){
        await setPolygons(map);
      } else {
        hidePolygons();
      }
    } else {
      await setUserPaths(map);
    }
  });
  if (FILTER_TYPE != 'user_paths'){
    setMarkers(map, coordinates);
  }
  if (markers.length > 0){
    markerCluster = new MarkerClusterer(map, markers,
            {imagePath: 'img/group_marker/m'});
    var bounds = new google.maps.LatLngBounds();
    for (var i = 0; i < markers.length; i++) {
      bounds.extend(markers[i].getPosition());
    }
    map.fitBounds(bounds);

    var listener = google.maps.event.addListener(map, "idle", function() {
    if (map.getZoom() > 17) map.setZoom(17);
      google.maps.event.removeListener(listener);
    });
  }
  if (map_property.popup_bool){ await setPopups(map, coordinates); }

  if (FILTER_TYPE != 'user_paths'){
    infoWindow = new google.maps.InfoWindow();
    if (map.getZoom() >= 16){
      await setPolygons(map);
    } else {
      hidePolygons();
    }
  } else {
    await setUserPaths(map, true);
  }
}

$(document)
  /*.on('mouseleave', '.js_popup', function() {
    popup.hide();
  })*/
  .on('click', '.js_popup', function() {
    if (FILTER_TYPE=='users_gallery'){
      findAndSelectPhoto($(this).data('photo_id'));
    } else {
      document.location.href = 'task.php?act=task_detail&id=' + $(this).data('task_id');
    }
  })



function insert_popup(num){
  $('main').append('<div id="js_map_popup_'+num+'"></div>'); //je třeba, refresh mapy totiž sežere div
}

function load_photos(id, photo_id){
  return new Promise(options =>{
    options(
      $.ajax({
        type: "post",
        url: "task.php",
        async: true,
        data: {
          act: "get_tasks_photos",
          type: FILTER_TYPE,
          id: id,
          photo_id: photo_id
        },
        dataType: "HTML",
        success: function (photo_html) {
          popup_photo = photo_html;
        }
      })
    )
  });
}

function setMarkers(map, locations) {
  $.each(locations, function (index, val) {
    var marker_icon = val.status.replace(" ", "");
    if (marker_icon == 'datachecked'){
      marker_icon += "_"+val.flag_id;
    }
    var myLatlng = new google.maps.LatLng(val.lat,val.lng);
    var marker =  new google.maps.Marker({
      position: myLatlng,
      icon:{
        url: "img/map_marker/marker_"+marker_icon+".png"
      }
    });
    // Add the new marker to the markers array
    markers.push(marker);
    if (map.getZoom() >= 17){
      marker.setMap(null);
    } else {
      marker.setMap(map);
    }
  });
}

async function setPolygons(map){
  var actualbounds = map.getBounds();
  await getCurentPolygons(map, actualbounds.getNorthEast(), actualbounds.getSouthWest());
  hidePolygons();

  $.each(polygons, function (index, polygon) {
    geometry = JSON.parse(polygon.wgs_geometry);
    var landCoords = [];
    $.each(geometry, function(index, coords){
      var myLatlng, lat, lng;
      var latlnglit = [];
      $.each(coords, function(index, coord){
        lat = coord[0];
        lng = coord[1];
        myLatlng = new google.maps.LatLng(lat,lng);
        latlnglit.push(myLatlng);
      });
      landCoords.push(latlnglit);
    });
      // Construct the polygon.
      const land = new google.maps.Polygon({
          paths: landCoords,
          strokeColor: "#ea3122",
          strokeOpacity: 0.7,
          strokeWeight: 2,
          fillColor: "#ea3122",
          fillOpacity: 0.2,
          name: polygon.identificator
        });
      const landName = new google.maps.Marker({
        position: land.getApproximateCenter(),
        icon:{
          url: "landNameGenerator.php?land="+polygon.identificator+"&zoom="+map.getZoom()
        }
      });

      lands.push(land);
      landNames.push(landName);
      if (map.getZoom() >= 16){
        land.setMap(map);
        landName.setMap(map);
      } else {
        hidePolygons();
      }

  });
}

function hidePolygons(){
  $.each(lands, function(index, curLand){
    curLand.setMap(null);
  });
  lands = [];
  $.each(landNames, function(index, curLandName){
    curLandName.setMap(null);
  });
  landNames = [];
}

function getCurentPolygons(map, actNE, actSW){
  return new Promise(options =>{
    options(
      $.ajax({
        type: "post",
        url: "task.php",
        async: true,
        data: {
          act: "get_polygons_gps_points",
          lat_min: actSW.lat(),
          lat_max: actNE.lat(),
          lng_min: actSW.lng(),
          lng_max: actNE.lng()
        },
        dataType: "JSON",
        success: function (res) {
          polygons = res;
        }
      })
    )
  });
}

async function setUserPaths(map, setMapBounds = false){
  var bounds;
  if (setMapBounds){
    bounds = new google.maps.LatLngBounds();
  }
  await getCurentUserPaths(map, $(".js_search_input").val());
  hideUserPaths();

  $.each(path_points, function (index, polygon) {
    coords = polygon.points;
    var pathCoords = [];
    var myLatlng, lat, lng;
    var latlnglit = [];
    $.each(coords, function(index, coord){
      lat = coord.lat;
      lng = coord.lng;
      myLatlng = new google.maps.LatLng(lat,lng);
      if (setMapBounds){
        bounds.extend(myLatlng);
      }
      latlnglit.push(myLatlng);
    });
    pathCoords.push(latlnglit);

      // Construct the polygon.
      const path = new google.maps.Polygon({
          paths: pathCoords,
          strokeColor: "#404040",
          strokeOpacity: 0.7,
          strokeWeight: 2,
          fillColor: "#a6a6a6",
          fillOpacity: 0.2,
          name: polygon.name
        });
      const pathName = new google.maps.Marker({
        position: path.getApproximateCenter(),
        icon:{
          url: "landNameGenerator.php?land="+polygon.name+"&zoom="+map.getZoom()
        }
      });

      paths.push(path);
      pathNames.push(pathName);
      path.setMap(map);
      pathName.setMap(map);
  });
  if (setMapBounds){
    map.fitBounds(bounds);
  }
}

function hideUserPaths(){
  $.each(paths, function(index, curPath){
    curPath.setMap(null);
  });
  paths = [];
  $.each(pathNames, function(index, curPathName){
    curPathName.setMap(null);
  });
  pathNames = [];
}

function getCurentUserPaths(map, id){
  return new Promise(options =>{
    options(
      $.ajax({
        type: "post",
        url: "index.php",
        async: true,
        data: {
          act: "get_user_paths_gps_points",
          user_id: id
        },
        dataType: "JSON",
        success: function (res) {
          path_points = res;
        }
      })
    )
  });
}

function setPopups(map, locations) {
    $.each(locations, async function (index, val) {
      insert_popup(index);
      if (FILTER_TYPE=='users_gallery'){
        var id = val.photo_id
      } else {
        var id = val.task_id
      }
      $("#js_map_popup_"+index).append(await load_photos(id,val.photo_id));
      var map_popup = $("#js_map_popup_"+index).find(".js_popup")[0];
      var map_azimuth = $("#js_map_popup_"+index).find(".js_azimuth")[0];

      Popup = createPopupClass();
      var popup = new Popup(new google.maps.LatLng(val.lat, val.lng), map_popup, map_azimuth, id);

      // Add the new marker to the markers array
      popups.push(popup);
      if (map.getZoom() >= 17){
        popup.setMap(map);
      } else {
        popup.setMap(null);
      }
    });
}

function findAndSelectPhoto(photo_id){
  if ($('#assign_photo_input_'+photo_id)[0].checked === true){
    $('#assign_photo_input_'+photo_id).prop('checked', false);
    $('#assign_photo_input_'+photo_id).removeAttr('checked');
  } else {
    $('#assign_photo_input_'+photo_id).prop('checked', true);
    $('#assign_photo_input_'+photo_id).attr('checked', "checked");
  }

}

function clear_vars(){
  markers = [];
  popups = [];
  lands = [];
  polygons = [];
  infoWindow = "";
  coordinates = "";
}

function load_tasks_gps_points(){
  var specified_ids="";
  if ( $( ".js_photo_ids" ).length ) {
    specified_ids = $('.js_photo_ids').val();
  }
  return new Promise(options =>{
    options(
      $.ajax({
        type: "post",
        url: "task.php",
        async: true,
        data: {
          act: "get_tasks_gps_points",
          filter_type: FILTER_TYPE, //definováno v zvláštních js skriptech
          search: $('.js_search_input').val(),
          specified_ids: specified_ids
        },
        dataType: "JSON",
        success: function (res) {
          coordinates = res;
        }
      })
    )
  });
}

function showArrays(event) {
  const polygon = this;
  const vertices = polygon.getPath();
  let contentString = "<b>"+ this.name +"</b>";
  infoWindow.setContent(contentString);
  infoWindow.setPosition(event.latLng);
  infoWindow.open(map);
}

function createPopupClass() {
  /**
   * A customized popup on the map.
   * @param {!google.maps.LatLng} position
   * @param {!Element} content The bubble div.
   * @constructor
   * @extends {google.maps.OverlayView}
   */
  function Popup(position, content, azimuth, id) {
    this.position = position;
    if (FILTER_TYPE=='users_gallery'){
      this.task_id = null;
      this.photo_id = id;
    } else {
      this.task_id = id;
    }

    content.classList.add('popup-bubble');

    // This zero-height div is positioned at the bottom of the bubble.
    this.bubbleAnchor = document.createElement('div');
    this.bubbleAnchor.classList.add('popup-bubble-anchor');
    this.bubbleAnchor.appendChild(content);
    this.bubbleAnchor.appendChild(azimuth);

    // This zero-height div is positioned at the bottom of the tip.
    this.containerDiv = document.createElement('div');
    this.containerDiv.classList.add('popup-container');
    this.containerDiv.appendChild(this.bubbleAnchor);

    // Optionally stop clicks, etc., from bubbling up to the map.
    google.maps.OverlayView.preventMapHitsAndGesturesFrom(this.containerDiv);
  }
  // ES5 magic to extend google.maps.OverlayView.
  Popup.prototype = Object.create(google.maps.OverlayView.prototype);

  /** Called when the popup is added to the map. */
  Popup.prototype.onAdd = function() {
    this.getPanes().floatPane.appendChild(this.containerDiv);
  };

  /** Called when the popup is removed from the map. */
  Popup.prototype.onRemove = function() {
    if (this.containerDiv.parentElement) {
      this.containerDiv.parentElement.removeChild(this.containerDiv);
    }
  };

  Popup.prototype.hide = function() {
    this.bubbleAnchor.classList.add('popup-bubble-anchor-hide');
  }

  /** Called each frame when the popup needs to draw itself. */
  Popup.prototype.draw = function() {
    var divPosition = this.getProjection().fromLatLngToDivPixel(this.position);

    // Hide the popup when it is far out of view.
    var display =
        Math.abs(divPosition.x) < 4000 && Math.abs(divPosition.y) < 4000 ?
        'block' :
        'none';

    if (display === 'block') {
      this.containerDiv.style.left = divPosition.x + 'px';
      this.containerDiv.style.top = divPosition.y + 'px';
    }
    if (this.containerDiv.style.display !== display) {
      this.containerDiv.style.display = display;
    }
  };

  return Popup;
}
