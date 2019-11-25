var pictureMapOlCtrl = function (
  $scope, $state, $rootScope, $ionicModal, $cordovaCamera, $controller, $q,
  $timeout, $ionicPlatform, PopupFactory, $filter,
  OL3Map_service, OL3Layers_service,  appService,
  take_picture_menus, pics_menu_states, pics_loading_status,
  MappaService, coordinatesService,  httpService, sessionService, JSTS_service, $translate, gpsDiagnosticService, $ionicLoading
) {

  $scope.menu_stati = take_picture_menus;
  $scope.on_me = $filter('translate')('picturePath.zoom.on_me');
  $scope.on_part = $filter('translate')('picturePath.zoom.on_part');
  $scope.on_path = $filter('translate')('picturePath.zoom.on_path');
  $scope.layer_visibility = $filter('translate')('picturePath.zoom.layer_visibility');
  $scope.open_gallery = $filter('translate')('picturePath.pics.open_gallery');
  $scope.take_picture = $filter('translate')('picturePath.pics.take_picture');
  $scope.take_picture_hull = $filter('translate')('picturePath.pics.take_picture_hull');
  $scope.take_picture_ntrip = $filter('translate')('picturePath.pics.take_picture_ntrip');
  $scope.upload_pics = $filter('translate')('picturePath.server.upload_tracks');
  $scope.download_pics = $filter('translate')('picturePath.server.download_tracks');
  $scope.gnss_info = $filter('translate')('baseMap.gnss_info');
  $scope.loading_status = pics_loading_status;
  $scope.stati_validi = pics_menu_states;
  $scope.map_widget_id = "map_canvas_picture";
  $scope.title = $filter('translate')('picturePath.title');
  $scope.raw_data_btn_txt_on = $filter('translate')('baseMap.raw_data_btn_txt_on');
  $scope.raw_data_btn_txt_off = $filter('translate')('baseMap.raw_data_btn_txt_off');
  $scope.popupInfo = {id: "popup-picture", contentId:"popup-content-picture"};
  $rootScope.correctionModePicture = 0;
  $ionicPlatform.ready(function () {
    $scope.popupContainer = document.getElementById($scope.popupInfo.id);
    $scope.popupContent = document.getElementById($scope.popupInfo.contentId);
  });

  $scope.locationAcquired = function(location){};
  $scope.removeInteractions = function(){};
  $scope.addInteractions = function(){};
  $scope.reloadExternalLayers = function(){
    $scope.layerPhotos = OL3Layers_service.getLayerPhotos();

    var layersToAdd = [
      $scope.layerPhotos
    ];

    $scope.layerPhotos.set("name", "PHOTO");
    $scope.loadPhotos(false);
    $scope.layerState.push({"name": "Photo", "acceso": true, "layerObj": [$scope.layerPhotos]});
    OL3Map_service.addLayers($scope.map, layersToAdd);
  };

  $scope.fakeGpsIconPosition = null;

  $scope.clickMap = function(evt){

    var trovati = function (selected) {
      var daTornare = [];
      if(selected && selected.length > 0){
        for(var i = 0; i < selected.length; i++){
          if(selected[i].get("type") === 'PHOTO'){
            daTornare.push(selected[i]);
          }
        }
      }
      return daTornare;
    };

    var photosSelected = $scope.map.getFeaturesAtPixel(evt.pixel, {
      layerFilter: function (layer) {
        return layer === $scope.layerPhotos;
      }
    });
    $scope.photosSelected = trovati(photosSelected);

    if ($scope.photosSelected.length > 0){
      $scope.showPopupInfoFlag = false;
      $scope.showImage($scope.photosSelected);
    }else{
      $scope.showPopupInfoFlag = true;
    }
  };

  $scope.afterMapInitialized = function(){
    $scope.map.on('click', $scope.clickMap);
  };

  $scope.createExternalListeners = function(){};
  $scope.removeExternalListeners = function(){};

  $controller('BaseMapController', { $scope: $scope });
  // -------------------------- FINE OVERRIDE --------------------------------------------------------------------
  // -------------------------- ------------- --------------------------------------------------------------------
  // -------------------------- ------------- --------------------------------------------------------------------


  //------------------------------------- MENUS ----------------------------------------------------------------------

  $scope.menu_zoom_toggled = function(){
    $scope.menu_stati.server.state = $scope.stati_validi.close;
    $scope.menu_stati.pics.state = $scope.stati_validi.close;
    $scope.menu_stati.gnss.state = $scope.stati_validi.close;
  };

  $scope.menu_pics_toggled = function(){
    $scope.menu_stati.server.state = $scope.stati_validi.close;
    $scope.menu_stati.zoom.state = $scope.stati_validi.close;
    $scope.menu_stati.gnss.state = $scope.stati_validi.close;

  };

  $scope.menu_server_toggled = function(){
    $scope.menu_stati.zoom.state = $scope.stati_validi.close;
    $scope.menu_stati.pics.state = $scope.stati_validi.close;
    $scope.menu_stati.gnss.state = $scope.stati_validi.close;

  };

  $scope.menu_gnss_toggled = function(){
    $scope.menu_stati.server.state = $scope.stati_validi.close;
    $scope.menu_stati.zoom.state = $scope.stati_validi.close;
    $scope.menu_stati.pics.state = $scope.stati_validi.close;
  };

  //------------------------------------- PHOTOS ---------------------------------------------------------------------

  $scope.takePictureOld = function(val) {
    $rootScope.correctionModePicture = val;

    $scope.chiudiMenu('pics');

    if($scope.getPositionFeature().getGeometry() !== undefined){
      var geom = $scope.getPositionFeature().getGeometry().getCoordinates();
      if(!isNaN(geom[0]) && !isNaN(geom[1]) && geom[0] !== "NaN" && geom[1] !== "NaN"){
        if($scope.default_tile.id !== 'OSM' && $scope.default_tile.id !== 'GOOGLE_MAPS'){
          $timeout(function(){
            $state.go("takePictureAR", {'startPointLat': geom[0],
              'startPointLng': geom[1],
              'reasons': "",
              'fakegpspos':$scope.fakeGpsIconPosition});
          },500);
        }else{
          var start_pos = coordinatesService.from3857toWGS84({"latitude":geom[1]+"", "longitude":geom[0]+""});
          $timeout(function(){
            $state.go("takePictureAR", {'startPointLat': start_pos.x,
              'startPointLng': start_pos.y,
              'reasons': "",
              'fakegpspos':$scope.fakeGpsIconPosition});
          },500);
        }
      }else{
        alert($filter('translate')('picturePath.popups.minimumInfoNeeded'));
      }
    }else{
      alert($filter('translate')('picturePath.popups.minimumInfoNeeded'));
    }
  };

  $scope.createPhotoFeature = function(photoData, fromWeb){
    var feat = new ol.Feature();
    var posWGS = coordinatesService.from30033004to3857([photoData.pointLat, photoData.pointLng], sessionService.getAuthId());
    var posGEOSERVER = coordinatesService.from3857to30033004({"latitude":photoData.pointLat, "longitude":photoData.pointLng});
    if(typeof photoData.id !== 'undefined' && photoData.id != '' && photoData.id != null){
      feat.set("id", photoData.id);
    }
    if(fromWeb){
      feat.set("uploaded", true);
    }else{
      feat.set("uploaded", photoData.uploaded);
    }
    if(photoData.photoblob !== undefined){
      feat.set("url", window.URL.createObjectURL(photoData.photoblob));
    }else{
      feat.set("url", "");
    }
    feat.set("date", photoData.date);
    feat.set("type","PHOTO");
    feat.set("heading",photoData.heading);
    feat.set("tilt_angle", photoData.tilt_angle);
    feat.set("posMM",{"y":photoData.pointLat, "x":photoData.pointLng});
    feat.set("posWGS",{"lat":photoData.pointLat, "lng":photoData.pointLng});
    if(photoData.altitude !== "" || (photoData.altitude === "" && photoData.altitude_locmanager === 0)){
      feat.set("text", "Data " + photoData.date + "<br>"+
        "Direction "+ Math.round(photoData.heading,2) +"° N"  + "<br>" +
        "Lat. " +  photoData.pointLat + "<br>" +
        "Long. "+ photoData.pointLng + "<br>" +
        "Altitude "+ Math.round(photoData.altitude,2) + " mt<br>" +
        "UUID " + photoData.uuid + "<br>" +
        "Camera:<br>"+
        "Fov " + photoData.fov +"<br>" +
        "Tilt Angle " + photoData.tilt_angle +"°<br>"
      );
    }else{
      feat.set("text", "Data " + photoData.date + "<br>"+
        "Direction "+ Math.round(photoData.heading,2) +"° N"  + "<br>" +
        "Lat. " +  photoData.pointLat + "<br>" +
        "Long. "+ photoData.pointLng + "<br>" +
        "Altitude "+ Math.round(photoData.altitude_locmanager,2) + " mt<br>" +
        "UUID " + photoData.uuid + "<br>" +
        "Camera:<br>"+
        "Fov " + photoData.fov +"<br>" +
        "Tilt Angle " + photoData.tilt_angle +"°<br>"
      );
    }

    feat.set("uri_photo", photoData.uri_photo);
    feat.set("fullObject", photoData); //per mandarlo al server
    if($scope.default_tile.id == 'GEOSERVER_2014_RGB'){
      feat.setGeometry(new ol.geom.Point([posGEOSERVER.x, posGEOSERVER.y]));
    }else{
      var newPos = coordinatesService.fromWGS84to3857({"latitude":photoData.pointLat,"longitude":photoData.pointLng});
      feat.setGeometry(new ol.geom.Point([newPos.x, newPos.y]));
    }
    $scope.layerPhotos.getSource().addFeature(feat);
  };

  $scope.removeAlsoArc = function(feature){
    $scope.layerPhotos.getSource().getFeatures().forEach(function(f){
      if(f.get("type") == 'ARC' && f.get("date") == feature.get("date")){
        $scope.layerPhotos.getSource().removeFeature(f);
      }
    });
  };

  $scope.deletePhoto = function(feature, serverBool){
    var deferred = $q.defer();
    var idfoto = feature.get('fullObject').id;
    var dateFoto = feature.get('fullObject').date;
    if(serverBool){
      httpService.deleteFoto("0",
       "0",
       idfoto,
        function success(res){
            MappaService.deleteFoto(dateFoto).then(function(){
            $scope.layerPhotos.getSource().removeFeature(feature);
            /* Rimuovo anche arco */
            $scope.removeAlsoArc(feature);
            deferred.resolve(true);
          });
        },
        function error(err){
          if(err.status === 403){
            alert($filter('translate')('baseMap.popups.errorToken.text'));
            localStorage.setItem("logged","false");
            localStorage.setItem("user", null);
            $state.go("login");
          }
          deferred.resolve(false);
        })
    }else{
      MappaService.deleteFoto(dateFoto).then(function(){
        $scope.layerPhotos.getSource().removeFeature(feature);
        /* Rimuovo anche arco */
        $scope.removeAlsoArc(feature);
        deferred.resolve(true);
      });
    }
    return deferred.promise;
  };

  $scope.deleteMarker = function(feature){
    $scope.closeModal();
    appService.show();
    if(feature.get("uploaded")){
      $scope.deletePhoto(feature, true).then(function(esito){
        if(!esito){
          appService.hide();
          alert($filter('translate')('picturePath.popups.deletePhotoError.subtitle'));
        }else{
          appService.hide();
          alert($filter('translate')('picturePath.popups.deletePhotoSuccess.subtitle'));
        }
      });
    }else{
      $scope.deletePhoto(feature, false);
      appService.hide();
    }
  };

  //---------------------------------------- MARKERS -----------------------------------------------------------------

  $scope.setMarkerGPS = function(marker){
    if ($scope.gpsPosMarker) {
      $scope.gpsPosMarker.remove();
    }
    $scope.gpsPosMarker = marker;
  };

  //---------------------------------------- MAPS --------------------------------------------------------------------

  $scope.photoMarkers = [];
  $scope.arcMarkers = [];

  $scope.createArcFeature = function(photoData){
    var feat = new ol.Feature();
    if(photoData.photoblob !== undefined){
      feat.set("url", window.URL.createObjectURL(photoData.photoblob));
    }else{
      feat.set("url", "");
    }
    feat.set("date", photoData.date);
    feat.set("type","ARC");
    feat.set("fullObject", photoData); //per mandarlo al server
    var newPos = coordinatesService.fromWGS84to3857({"latitude":photoData.pointLat,"longitude":photoData.pointLng});
    feat.setGeometry(new ol.geom.Point([newPos.x, newPos.y]));
    $scope.layerPhotos.getSource().addFeature(feat);
    $scope.arcMarkers[photoData.date] = $scope.createHeadingCone(photoData.heading);
  };

  $scope.createHeadingCone = function(heading){
    var canvas = document.createElement("canvas");
    var ctx = canvas.getContext("2d");
    canvas.height = 300;
    canvas.width = 300;
    var radiansConst = 0.01745329252;
    var headInRadians = heading * radiansConst;
    var northDegrees = 270;
    var north = northDegrees * radiansConst;
    var arcStartDegrees = northDegrees + heading;
    if (arcStartDegrees > 360) {
      arcStartDegrees = arcStartDegrees - 360;
    }
    var coneStartD = arcStartDegrees - 28;
    var coneEndD = arcStartDegrees + 28;
    var coneStartR = coneStartD * radiansConst;
    var coneEndR = coneEndD * radiansConst;
    var arcEnd = (northDegrees + heading) * radiansConst;
    ctx.lineWidth = 4;
    ctx.strokeStyle = 'green';
    ctx.fillStyle = 'rgba(0,255,0,0.3)';
    ctx.beginPath();
    ctx.moveTo(150,150);
    ctx.arc(150,150,145,coneStartR,coneEndR);
    ctx.lineTo(150,150);
    ctx.stroke();
    ctx.fill();
    return canvas.toDataURL();
  };

  $scope.createPhotoMiniature = function(photoData){
    var deferred = $q.defer();
    var canvas = document.createElement('canvas');
    canvas.height = 150;
    canvas.width = 150;
    var ctx = canvas.getContext("2d");
    var img = new Image();
    img.src = window.URL.createObjectURL(photoData.photoblob);
    img.onload = function(){
      var factor =  img.width / img.height;
			var imgnewwidth;
      var imgnewheight;
      var startonx;
			var startony = 10;
			var strokewidth = 3;
			if(factor >= 1){ //orizzontale
				imgnewwidth = canvas.width/2; //(canvas.width / 2) * factor;
				imgnewheight = canvas.height/2; //(canvas.height / 2) * factor;
				startonx = ((canvas.width / 2) - (imgnewwidth / 2));
			}else{
				imgnewwidth = (canvas.width/2) * factor;
	      imgnewheight = (canvas.height/2) * factor;
	      startonx = ((canvas.width/2) - (imgnewwidth/2));
			}
      ctx.fillStyle = "rgba(200, 0, 0, 1)";
      ctx.strokeStyle = "rgba(200, 0, 0, 1)";
      ctx.fillRect(startonx - strokewidth, startony - strokewidth, imgnewwidth + (strokewidth*2), imgnewheight + (strokewidth*2));
      ctx.drawImage(img, startonx, startony, imgnewwidth, imgnewheight);
      ctx.lineWidth=""+strokewidth;
      ctx.rect(startonx - strokewidth, startony - strokewidth, imgnewwidth + (strokewidth*2), imgnewheight + (strokewidth*2));
      ctx.stroke();
      ctx.fillRect(canvas.width/2 - (strokewidth/2), imgnewheight + (startony+strokewidth),  strokewidth, (canvas.height-imgnewheight-(startony+strokewidth) ));
      var not_uploaded = canvas.toDataURL();

      ctx.fillStyle = "rgba(0, 200, 0, 1)";
      ctx.strokeStyle = "rgba(0, 200, 0, 1)";
      ctx.fillRect(startonx - strokewidth, startony - strokewidth, imgnewwidth + (strokewidth*2), imgnewheight + (strokewidth*2));
      ctx.drawImage(img, startonx, startony, imgnewwidth, imgnewheight);
      ctx.lineWidth=""+strokewidth;
      ctx.rect(startonx - strokewidth, startony - strokewidth, imgnewwidth + (strokewidth*2), imgnewheight + (strokewidth*2));
      ctx.stroke();
      ctx.fillRect(canvas.width/2 - (strokewidth/2), imgnewheight + (startony+strokewidth),  strokewidth, (canvas.height-imgnewheight-(startony+strokewidth) ));
      var uploaded = canvas.toDataURL();
      $scope.photoMarkers[photoData.date] = {"not_uploaded":not_uploaded, "uploaded": uploaded};
      deferred.resolve();
    };
    return deferred.promise;
  };

  $scope.photosPromises = [];

  $scope.createMarkersFromCollection = function(collection, fromWeb){
    $scope.photosPromises = [];
    var defer = $q.defer();
    for (var i = 0; i < collection.length; i++){
      var markerFoto = collection[i];
      if(markerFoto.username === localStorage.getItem("username")){
        var latLng = [markerFoto.pointLat, markerFoto.pointLng];
        $scope.createPhotoFeature(markerFoto, fromWeb);
        $scope.createArcFeature(markerFoto);
        $scope.photosPromises.push($scope.createPhotoMiniature(markerFoto));
        if(fromWeb){
          markerFoto.uploaded = true;
          MappaService.upsertPhotoUri( markerFoto );
        }
      }
    }

    $q.all($scope.photosPromises).then(function(){
      $scope.downloadedPhotos = null;
      $scope.layerPhotos.setStyle(function style(feature, resolution){
        var icon = feature.get('url');
        var type = feature.get('type');
        if (type == 'ARC'){
          var style = new ol.style.Style({
            image: new ol.style.Icon({
              src: $scope.arcMarkers[feature.get("date")], //icon,
              anchor: [0.5, 0.5],
              anchorXUnits: 'fraction',
              anchorYUnits: 'fraction',
              opacity: 1,
              rotateWithView: true,
              scale: (resolution > 0 && resolution < 0.5 ? 0.6 : 0.2 )
            })
          });
        }else{
          var src = (typeof feature.get("uploaded") !== 'undefined' && feature.get("uploaded") ? $scope.photoMarkers[feature.get("date")].uploaded : $scope.photoMarkers[feature.get("date")].not_uploaded);
          var style = new ol.style.Style({
            image: new ol.style.Icon({
              src: src, //$scope.photoMarkers[feature.get("date")], //icon,
              anchor: [0.5, 1],
              anchorXUnits: 'fraction',
              anchorYUnits: 'fraction',
              opacity: 1,
              scale: (resolution > 0 && resolution < 0.5 ? 0.8 : 0.4 )
            })
          });
        }
        return style;
      });
      $ionicLoading.hide();
    });
    appService.safeApply($rootScope, function () {
      $scope.loading_status.photo = true;
    });
  };

  $scope.downloadedPhotos;

  $scope.loadPhotos = function(fromWeb){
    if (typeof $scope.layerPhotos !== 'undefined'){
      appService.show();
      if(!fromWeb){
        $scope.layerPhotos.getSource().clear();
      }else{
        $scope.layerPhotos.getSource().getFeatures().forEach(function(feature){
          if(feature.get("uploaded") == true){
            $scope.layerPhotos.getSource().removeFeature(feature);
            $scope.removeAlsoArc(feature);
          }
        });
      }
      $scope.photosPromises = [];
      if(!fromWeb){
        MappaService.selectFoto(
          "0",
          "0")
          .then(function(data){
            $scope.createMarkersFromCollection(data, false);
          });
      }else{
        $scope.createMarkersFromCollection($scope.downloadedPhotos, true);
      }
    }
  };

  $scope.updatePhotos = function(){
    var deferred = $q.defer();
    appService.show();
      httpService.dowloadPhotoList((data)=>{
        console.log(data);
        $scope.checkPhotosWithLocal(data.data.photos).then((remotePhotos)=>{
          console.log("finito");
          if(remotePhotos.length > 0){
            PopupFactory.confirmPopupWVariable("picturePath.popups.downloadPhotos", remotePhotos.length,
            function ok(){
              let photoPromises = [];
              for(let i=0;i<remotePhotos.length;i++){
                photoPromises.push(httpService.downloadPhoto(remotePhotos[i].id));
              }
              $q.all(photoPromises).then(function(photos){
                console.log("finite le foto",photos);
                //$scope.createMarkersFromCollection(photos, true);
                for(let i=0;i<photos.length;i++){
                  photos[i].uploaded = true;
                  MappaService.upsertPhotoUri( photos[i] );
                }
                $scope.loadPhotos(false);
                PopupFactory.showPopup("picturePath.popups.updatePhotoSuccess");
                deferred.resolve();
              });
            },function cancel(){
              console.log("cancelled");
              deferred.resolve("");
            });
          }else{
            PopupFactory.showPopup("picturePath.popups.downloadNoPhotos");
          }
        })
        appService.hide();
      },(error)=>{
        console.error(error);
        appService.hide();
        deferred.reject("");
      })
    return deferred.promise;
  };

  $scope.checkPhotosWithLocal = function(remotePhotos){
    let deferred = $q.defer();
    let photoPromises;
    MappaService.selectFotoAll().then(function(localPhotos){
      for(let i=0; i<localPhotos.length; i++){
        if(localPhotos[i].uploaded){
          let found = false;
          for(let j=0;j<remotePhotos.length;j++){
            if(typeof localPhotos[i].id !== 'undefined' && remotePhotos[j].id == localPhotos[i].id){
              found = true;
              remotePhotos.splice(j,1);
            }
          }
          if(!found){
            $scope.layerPhotos.getSource().getFeatures().forEach(function(feat){
                if(feat.get("type") == 'PHOTO' && feat.get('uploaded') == true && feat.get("fullObject").id == localPhotos[i].id){
                  $scope.deletePhoto(feat);
                }
            });

          }
        }
      }
      deferred.resolve(remotePhotos);
    });
    return deferred.promise;
  };

  $scope.checkPreToggleUsbCommunication_pic = function(){
    var deferred = $q.defer();
    gpsDiagnosticService.checkAvailability(
      $scope.toggleUsbCommunication,
      function(){console.log("GPS UNAVAILABLE!")});
    deferred.resolve("");
    return deferred.promise;
  };

  $scope.send_lock = false;
  $scope.error403_shown = false;
  $scope.sendPhotos = function(){
    if($scope.send_lock){
      return;
    }else{
      $scope.send_lock = true;
    }
    var deferred = $q.defer();
    var photosArray = $scope.layerPhotos.getSource().getFeatures();
    var tot = 0;
    photosArray.forEach(function(f){
      if(!f.get('uploaded') && f.get("type") == 'PHOTO'){
        tot +=1 ;
      }
    });
    if(tot>0){
      alert($filter('translate')('picturePath.popups.sending')+" "+tot+" "+$filter('translate')('picturePath.popups.photos'));
      appService.show();
      httpService.postFoto("0","0", photosArray,
        function(resp){
          if(typeof resp.data !== 'undefined' && typeof resp.data.errorMessage == 'undefined' && typeof resp.data.arrayIdFoto !== 'undefined'){
            appService.hide();
            alert($filter('translate')('picturePath.popups.sendPhotoSuccess'));
            $scope.layerPhotos.getSource().getFeatures().forEach(function(feat){
              for(var i = 0; i < resp.data.arrayIdFoto.length; i++){
                if(feat.get("type") == 'PHOTO' && feat.get('uploaded') == false && feat.get("date") == resp.data.arrayIdFoto[i].date){
                  feat.set('uploaded',true);
                  let fullObject = feat.get('fullObject');
                  fullObject.id =resp.data.arrayIdFoto[i].idFotoNew;
                  feat.set('fullObject', fullObject);
                  feat.set('id', resp.data.arrayIdFoto[i].idFotoNew);
                  feat.set('uri_photo', feat.get("date"));
                }
              }
            });
            MappaService.setPhotosAsUploaded("0", "0", resp.data.arrayIdFoto);
            $scope.send_lock = false;
            deferred.resolve("");
          }else if(typeof resp.data !== 'undefined' && typeof resp.data.errorMessage !== 'undefined'){
            appService.hide();
            alert($filter('translate')('picturePath.popups.sendPhotoErrorSave'));
            $scope.send_lock = false;
            deferred.resolve("");
          }else{
            appService.hide();
            alert($filter('translate')('picturePath.popups.sendPhotoErrorConnect'));
            $scope.send_lock = false;
            deferred.resolve("");
          }
        },
        function(err){
          appService.hide();
          if(err.status === 403){
            if(!($scope.error403_shown)){
              $scope.error403_shown = true;
              alert($filter('translate')('baseMap.popups.errorToken.text'));
              localStorage.setItem("logged","false");
              localStorage.setItem("user", null);
              $state.go("login");
            }
          }else{
            alert($filter('translate')('picturePath.popups.sendPhotoErrorConnect'));
            $scope.send_lock = false;
          }
          deferred.resolve("");
        },
        function(err){
          appService.hide();
          alert($filter('translate')('picturePath.popups.sendPhotoErrorNoNewPhoto'));
          $scope.send_lock = false;
          deferred.resolve("");
        });
    }else{
      appService.hide();
      alert($filter('translate')('picturePath.popups.sendPhotoErrorNoNewPhoto'));
      $scope.send_lock = false;
      deferred.resolve("");
    }
    return deferred.promise;
  };

  $scope.$on('$ionicView.beforeLeave', function(){
    window.removeEventListener('native.keyboardshow',function(){});
    window.removeEventListener('native.keyboardhide',function(){});
    $scope.map.un('click', $scope.clickMap);
  });

  $ionicModal.fromTemplateUrl('templates/mappa/pictureModal.html', {
    scope: $scope,
    animation: 'slide-in-up'
  }).then(function(modal) {
    $scope.modalPicture = modal;

    $scope.openModal = function() {
      $scope.modalPicture.show();
    };

    $scope.closeModal = function() {
      $scope.imageFeatures = [];
      $scope.modalPicture.hide();
    };

    $scope.imageFeatures = [];

    $scope.showImage = function(features) {
      features.forEach(function(feature){
        if(feature.get("type") == 'PHOTO'){
          $scope.imageFeatures.push(feature);
        }
      });

      $scope.openModal();
    };
  });

  $scope.$on('$destroy', function() {
    $scope.modalPicture.remove();
  });

  $scope.$on('modal.hidden', function() {});

  $scope.$on('modal.removed', function() {});

  $scope.$on('modal.shown', function() {
    console.log("mostrato");
  });

};
pictureMapOlCtrl.$inject = ['$scope',  '$state',  '$rootScope', '$ionicModal', '$cordovaCamera', '$controller', '$q',
  '$timeout', '$ionicPlatform', 'PopupFactory', '$filter',
  'OL3Map_service', 'OL3Layers_service',  'appService',
  'take_picture_menus', 'pics_menu_states', 'pics_loading_status',
  'MappaService', 'coordinatesService',  'httpService', 'sessionService', 'JSTS_service', '$translate', 'gpsDiagnosticService', '$ionicLoading'];
angular.module('mappa.module').controller('pictureMapOlCtrl', pictureMapOlCtrl);
