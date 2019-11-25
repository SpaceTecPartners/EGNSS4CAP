var trackPathMapOlCtrl = function (
            $scope, $controller, $rootScope, $ionicPopup, $ionicModal, $ionicPlatform,

            sqliteService, OL3Map_service, OL3Layers_service, appService, PopupFactory,
                  track_path_menus, track_path_menu_states, track_path_loading_status,
                  MappaService, coordinatesService, JSTS_service, httpService, sessionService, $q, $filter, usbService, $ionicLoading, gpsDiagnosticService) {

    // -------------------------- INIZIO OVERRIDE ------------------------------------------------------------------
    // -------------------------- ------------- --------------------------------------------------------------------
    // -------------------------- ------------- --------------------------------------------------------------------

    $scope.menu_stati = track_path_menus;
    $scope.on_me = $filter('translate')('trackPath.zoom.on_me');
    $scope.on_part = $filter('translate')('trackPath.zoom.on_part');
    $scope.on_path = $filter('translate')('trackPath.zoom.on_path');
    $scope.layer_visibility = $filter('translate')('trackPath.zoom.layer_visibility');
    $scope.rem_all_points = $filter('translate')('trackPath.track.rem_all_points');
    $scope.rem_last_point = $filter('translate')('trackPath.track.rem_last_point');
    $scope.add_curr_point = $filter('translate')('trackPath.track.add_curr_point');
    $scope.upload_tracks = $filter('translate')('trackPath.server.upload_tracks');
    $scope.download_tracks = $filter('translate')('trackPath.server.download_tracks');
    $scope.toggle_track = $filter('translate')('trackPath.control.toggle_track');
    $scope.gnss_info = $filter('translate')('baseMap.gnss_info');
    $scope.raw_data_btn_txt_on = $filter('translate')('baseMap.raw_data_btn_txt_on');
    $scope.raw_data_btn_txt_off = $filter('translate')('baseMap.raw_data_btn_txt_off');
    $scope.title = $filter('translate')('trackPath.title');
    $scope.pause_track = $filter('translate')('trackPath.control.pause_track');
    $scope.loading_status = track_path_loading_status;
    $scope.stati_validi = track_path_menu_states;
    $scope.map_widget_id = "map_canvas_track";
    $scope.popupInfo = {id: "popup-track", contentId:"popup-content-track"};

    //PER RACCOLTA GPX
    $scope.currentCoordinates_gpx = [];
    $scope.currentCoordinates_gpx_ext = [];
    $scope.speed_kmh = -1;
    /////////////////////

    $scope.color = '#005ce6';

    $ionicPlatform.ready(function () {
        $scope.popupContainer = document.getElementById($scope.popupInfo.id);
        $scope.popupContent = document.getElementById($scope.popupInfo.contentId);
    });

    $scope.addNumero = function (numero) {
        $scope.codici.inserito = $scope.codici.inserito + numero.toString()
    };

    $scope.showPopup = function(prefixFormLang){
        return $ionicPopup.show({
            templateUrl: "templates/mappa/"+prefixFormLang+".html",
            title: $filter('translate')('trackPath.popups.'+prefixFormLang+'.title'),
            scope: $scope,
            buttons: [
                {
                    text: "<b>" + $filter('translate')('trackPath.popups.'+prefixFormLang+'.submitButton') + "</b>",
                    type: 'button-positive'
                }],
            cssClass: "popupBorderRadius"
        })
    };

    $scope.showPopupTitle = function(prefixFormLang){
        return $ionicPopup.show({
            title: $filter('translate')('trackPath.popups.'+prefixFormLang+'.title'),
            subTitle: $filter('translate')('trackPath.popups.'+prefixFormLang+'.subtitle'),
            buttons: [
                {
                    text: "<b>" + $filter('translate')('trackPath.popups.'+prefixFormLang+'.submitButton') + "</b>",
                    type: 'button-positive'
                }],
        })
    };

    $scope.removeInteractions = function(){
        $scope.map.removeInteraction($scope.selectInteraction);
    };

    $scope.addInteractions = function(){

        $scope.selectInteraction = new ol.interaction.Select({
            layers: [$scope.layerTracks],
            multi: false
        });

        $scope.map.addInteraction($scope.selectInteraction);

    };

  $scope.afterMapInitialized = function(){
    $scope.map.on('dblclick',  $scope.onClickPolygon);
    $scope.map.on('singleclick',  $scope.onSingleClick);
  };

    $scope.reloadExternalLayers = function(){

        //$scope.layerTracks = OL3Layers_service.getLayerTracks();
    	var lastTrackingSession = sessionService.getAnyOpenTrackingSession();

    	$scope.layerTracks = (lastTrackingSession == null ? OL3Layers_service.getLayerTracks() : lastTrackingSession.layerTracks);
        $scope.layerVerticiTracks = (lastTrackingSession == null ? OL3Layers_service.getLayerVerticiTracks() : lastTrackingSession.layerVerticiTracks);
        $scope.layerTracks.set("name", "TRACCIATI");
        $scope.layerVerticiTracks.set("name", "VERTICI TRACCIATI");

        var layersToAdd = [
            $scope.layerTracks, $scope.layerVerticiTracks
        ];

        $scope.loadTracks();
        $scope.layerState.push({"name":  $filter('translate')('trackPath.popups.layerSelected.content.tracciati'), "acceso": true, "layerObj": [$scope.layerVerticiTracks, $scope.layerTracks]});
        OL3Map_service.addLayers($scope.map, layersToAdd);

        if(lastTrackingSession != null){
    		$scope.toggleTrackPath(true);
    	}
    };

    $scope.accendiSpegniTracks = function(toSet){
        $scope.layerVerticiTracks.setVisible(toSet);
        $scope.layerTracks.setVisible(toSet);
    };

  $scope.onClickPolygon = function(evt){
    $scope.popupCloser = document.getElementById('popup-closer');
    if(evt.originalEvent.target && evt.originalEvent.target.id != $scope.popupInfo.id &&
      evt.originalEvent.target.id != $scope.popupInfo.contentId &&
      evt.originalEvent.target.id != $scope.popupCloser.id){

      if (!$scope.tracciaPercorso.attivo){
        var trovati = function(selected) {
          if (selected) {
            if (selected.length > 1) {
              return [selected[0]];
            } else {
              return selected;
            }
          } else {
            return [];
          }
        };

        var tracksSelected = $scope.map.getFeaturesAtPixel(evt.pixel,{
          layerFilter: function(layer){ return  layer === $scope.layerTracks; }
        });
        $scope.tracksSelected = trovati(tracksSelected);

        $scope.handleFeatureClicked();

      }
    }
  };

    //PER RACCOLTA GPX
    $scope.info_trackPoint_int = [];
    $scope.info_trackPoint_ext = [];
    ///////////////////
    $scope.locationAcquired = function(provider, center, args){
      //PER RACCOLTA GPX
      var sat_data = args[1];
        ////////////////////
        var ancheInPausa = false;
        if ($scope.tracciaPercorso.pausa){
            ancheInPausa = $scope.metadata.fakeTracking === 0 && provider === "fakeProvider";
        } else {
            ancheInPausa = !($scope.metadata.fakeTracking === 0 && provider === "fakeProvider");
        }

      //  PER RACCOLTA GPX
      if(sat_data.x !== 0 && sat_data.y !== 0 && !isNaN(sat_data.x) && !isNaN(sat_data.y) && sat_data.x !== '0' && sat_data.y !== '0') {
        if (args[0] === 'device') {
          var info_utili = [sat_data.n_sat, sat_data.hdop, sat_data.fix_type, $scope.speed_kmh];
          $scope.info_trackPoint_ext.push(info_utili);
        } else {
          if(args[0] === 'background' || args[0] === 'foreground'){
            var info_utili = [-1, -1, -1, -1];
            $scope.info_trackPoint_int.push(info_utili);
          }
        }
      }
      //////////////////////
      $scope.addPointToPath(false, provider === "device", ancheInPausa);
    };

    $controller('BaseMapController', { $scope: $scope });
    // -------------------------- FINE OVERRIDE --------------------------------------------------------------------
    // -------------------------- ------------- --------------------------------------------------------------------
    // -------------------------- ------------- --------------------------------------------------------------------

    $scope.handleAfterFeatureClicked = function(){
      $scope.showPopupFeatureClicked($scope.tracksSelected, $scope.layerTracks);
    };

    $scope.handleFeatureClicked = function(){
        $scope.layerSelectedAfterClick = {'type': '-1'};
        var ultimoTrovato = null;
        var layerTrovati = 0;

        if ($scope.tracksSelected.length == 1){
            ultimoTrovato = 4;
            layerTrovati += 1;
        }

        if (layerTrovati == 1){
            $scope.layerSelectedAfterClick.type = ultimoTrovato;
            $scope.handleAfterFeatureClicked();
        } else if (layerTrovati > 1){
            $scope.showPopup("layerSelected")
                .then($scope.handleAfterFeatureClicked);
        }
    };

  $scope.showPopupFeatureClicked = function(features, layer){

    $scope.featureClicked = {'type': '-1', 'layer': layer};
    var feature = features[0];
    if (feature){
      $scope.showPopup("featureClicked")
        .then(function() { $scope.afterPopupFeatureClicked(feature, layer); });
    }

  };

  $scope.afterPopupFeatureClicked = function(feature, layer){
    var type_sel = parseInt($scope.featureClicked.type);
    if (type_sel === 1){ //cancella poligono
      if (layer === $scope.layerTracks){

        $scope.deleteSingleTrack(feature);

      }

    }
  };

  $scope.deleteSingleTrack = function(feature) {

    if(feature.get("uploaded")){
      var esito = $scope.deleteTrack(feature, true).then(
        function(){
          $scope.selectInteraction.getFeatures().clear();
          $scope.overlayInfo.setPosition(undefined);
        },
        function(){
          $scope.showPopupTitle("deleteTrackError");
        }
      );
    }else{
      $scope.deleteTrack(feature, false).then(function(){
        $scope.selectInteraction.getFeatures().clear();
        $scope.overlayInfo.setPosition(undefined);
      });
    }
  };

  $scope.deleteTrack = function(feature, boole) {//idTrackServer
    var deferred = $q.defer();
    MappaService.deleteTrack(feature.get("pkuid")).then(
      function () {
        console.log("feature cancellata con successo");
        $scope.layerTracks.getSource().removeFeature(feature);
        $scope.updateVertexLayer();
        deferred.resolve();
      },
      function (err) {
        console.log("feature non cancellata errore", err);
        deferred.reject();
      });
    return deferred.promise;
  };

    $scope.toggleUsbCommunication = function(){
      var deferred = $q.defer();
      appService.safeApply($rootScope, function(){
        if($rootScope.firstConnection){
          $ionicLoading.show({
            template: '<ion-spinner></ion-spinner> <br/> waiting for raw data'
          });
          $rootScope.firstConnection = false;
          $rootScope.deviceEnabled = true;
          usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
        }
        else{
          if(!$rootScope.deviceEnabled){
            $ionicLoading.show({
              template: '<ion-spinner></ion-spinner> <br/> waiting for raw data'
            });
            $rootScope.deviceEnabled = true;
            usbService.requestNmeaMessages($scope.coordinatesNeedConversion);
          }
          else{
            $rootScope.deviceEnabled = false;
            usbService.stopNmeaMessages();
          }
        }
        usbService.setGpsStatus($rootScope.deviceEnabled);
        deferred.resolve("");
      });
      return deferred.promise;
    };

    $scope.menu_zoom_toggled = function(){
			$scope.menu_stati.server.state = $scope.stati_validi.close;
			$scope.menu_stati.track.state = $scope.stati_validi.close;
			$scope.menu_stati.gnss.state = $scope.stati_validi.close;
			$scope.menu_stati.control.state = $scope.stati_validi.close;
		};

		$scope.menu_track_toggled = function(){
			$scope.menu_stati.server.state = $scope.stati_validi.close;
			$scope.menu_stati.zoom.state = $scope.stati_validi.close;
			$scope.menu_stati.control.state = $scope.stati_validi.close;
			$scope.menu_stati.gnss.state = $scope.stati_validi.close;
		};

		$scope.menu_server_toggled = function(){
			$scope.menu_stati.zoom.state = $scope.stati_validi.close;
			$scope.menu_stati.track.state = $scope.stati_validi.close;
			$scope.menu_stati.control.state = $scope.stati_validi.close;
			$scope.menu_stati.gnss.state = $scope.stati_validi.close;
		};

		$scope.menu_control_toggled = function(){
			$scope.menu_stati.zoom.state = $scope.stati_validi.close;
			$scope.menu_stati.track.state = $scope.stati_validi.close;
			$scope.menu_stati.server.state = $scope.stati_validi.close;
			$scope.menu_stati.gnss.state = $scope.stati_validi.close;
		};

    $scope.menu_gnss_toggled = function(){
      $scope.menu_stati.server.state = $scope.stati_validi.close;
      $scope.menu_stati.zoom.state = $scope.stati_validi.close;
      $scope.menu_stati.track.state = $scope.stati_validi.close;
      $scope.menu_stati.control.state = $scope.stati_validi.close;
    };


        $scope.loadTracks = function(){

            MappaService.selectTrack().then(function(res) {

                res.forEach(function (track) {
                	var geom = coordinatesService.WktToOLPolygon(track.trackWKT);

                    var feat = new ol.Feature({
                        geometry: geom,
                        date: track.date,
                        in_corso: false,
                        pkuid: track.pkuid,
                        from_device: track.fromDevice === 'true',
                        uploaded: track.uploaded
                    });
                    $scope.layerTracks.getSource().addFeature(feat);
                });

                $scope.updateVertexLayer();
            });
        };

          $scope.checkPreToggleUsbCommunication_track = function(){
            var deferred = $q.defer();
            gpsDiagnosticService.checkAvailability(
              $scope.toggleUsbCommunication_track,
              function(){});
            deferred.resolve("");
            return deferred.promise;
          };

          $scope.toggleUsbCommunication_track = function(){
            var deferred = $q.defer();
            appService.safeApply($rootScope, function(){
              if($rootScope.firstConnection){

                 if(!usbService.isGpsActive()){
                  if($rootScope.survey_active){
                    $scope.showSurveyLoadingPopup();
                  }else{
                    $ionicLoading.show({
                      template: '<ion-spinner></ion-spinner> <br/> waiting for raw data'
                    });
                  }
                  usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
                  $rootScope.firstConnection = false;
                  $rootScope.deviceEnabled = true;
                }
                $scope.prima_rilevaz = true;
              }
              else{
                if(!$rootScope.deviceEnabled){
                  if($rootScope.survey_active){
                    $scope.showSurveyLoadingPopup();
                  }else{
                    $ionicLoading.show({
                      template: '<ion-spinner></ion-spinner> <br/> waiting for raw data'
                    });
                  }
                  $rootScope.deviceEnabled = true;
                  $scope.prima_rilevaz = true;
                  usbService.requestNmeaMessages($scope.coordinatesNeedConversion);
                }
                else{
                  $rootScope.deviceEnabled = false;
                  $scope.prima_rilevaz = true;
                  usbService.stopNmeaMessages();
                }
              }
              usbService.setGpsStatus($rootScope.deviceEnabled);
              deferred.resolve("");
            });
            return deferred.promise;
          };

    $scope.tracciaPercorso = {'pausa': false, 'attivo': false};
    $scope.currentCoordinates = [];
    $scope.currentCoordinatesExt = [];
    $scope.currentFeature = null;
    $scope.currentFeatureExt = null;

    $scope.removeAllPoints = function(){
          var deferred = $q.defer();
          $scope.currentCoordinates = [];
          $scope.currentCoordinatesExt = [];
          $scope.drawBothTracks();

          //$scope.chiudiMenu('track');
          deferred.resolve("");
          return deferred.promise;

    };

    $scope.removeLastPoint = function(){
          var deferred = $q.defer();
                $scope.currentCoordinates.pop();
                $scope.currentCoordinatesExt.pop();
                $scope.drawBothTracks();

                //$scope.chiudiMenu('track');
                deferred.resolve("");
                return deferred.promise;
    };

	$scope.addPointToPath = function(both, fromDevice, ancheInPausa){
      var deferred = $q.defer();

        var ts = new Date();
        var ts_format = ts.getFullYear()+"-"+(ts.getMonth()+1)+"-"+ts.getDate()+"T"+ts.getHours()+":"+ts.getMinutes()+":"+ts.getSeconds()+":"+ts.getMilliseconds()+"Z";

        if ($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 1 ) {
            if ($scope.positionFeature.getGeometry()) {
                var coordinates = $scope.positionFeature.getGeometry().getCoordinates();
                if (both) {
                  $scope.currentCoordinates.push(coordinates);
                  $scope.currentCoordinates_gpx.push([coordinates, ts_format]);
                } else {
                    if (!fromDevice) {
                      $scope.currentCoordinates.push(coordinates);
                      $scope.currentCoordinates_gpx.push([coordinates, ts_format]);
                    }
                }
                if($scope.tracciaPercorso.attivo && ancheInPausa) {
                  $scope.centerOnPoint(coordinates);
                }
            }
          }

          if ($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 0 ){
            if ($scope.positionFeatureExt.getGeometry()){
                  var coordinatesExt = $scope.positionFeatureExt.getGeometry().getCoordinates();
                  if (both) {
                    $scope.currentCoordinatesExt.push(coordinatesExt);
                    $scope.currentCoordinates_gpx_ext.push([coordinatesExt, ts_format]);

                  } else {
                      if (fromDevice) {
                        $scope.currentCoordinatesExt.push(coordinatesExt);
                        $scope.currentCoordinates_gpx_ext.push([coordinatesExt, ts_format]);
                      }
                  }

                  for(var i=0; i<coordinatesExt.length; i++){
                    coordinatesExt[i] = parseFloat(coordinatesExt[i]);
                  }
                  if($scope.tracciaPercorso.attivo && ancheInPausa) {
                    $scope.centerOnPoint(coordinatesExt);
                  }
              }
          }

      if($scope.tracciaPercorso.attivo && ancheInPausa){
          $scope.drawBothTracks();
      }
      deferred.resolve("");
      return deferred.promise;
		};

		$scope.drawLastTrack = function(coordinates, feature){
            var invalidPoints = [];

            var myI = coordinates.length;
            var invalida = true;

            while (myI > 0 && invalida){
                var coords_to_set = [coordinates.slice(0, myI).concat([coordinates[0]])];
                if (coords_to_set[0].length >= 4){
                    var geom_to_set = new ol.geom.Polygon(coords_to_set);
                    if (JSTS_service.isGeomValid(geom_to_set)){
                        feature.setGeometry(geom_to_set);
                        invalida = false;
                    }
                }
                if (invalida) {
                    myI -= 1;
                    invalidPoints.push(coordinates[myI]);
                }
            }
            if (myI <= 2){
                var date = feature.get("date");
                $scope.layerTracks.getSource().removeFeature(feature);
                feature = new ol.Feature({
                    date: date,
                    in_corso: true,
                    pkuid: OL3Map_service.getNewPKUID($scope.layerTracks),
                    from_device: feature === $scope.currentFeatureExt,
                    uploaded: false
                });
                $scope.layerTracks.getSource().addFeature(feature);
            }

            return [feature, invalidPoints];
		};

		$scope.drawBothTracks = function(){
            var res = $scope.drawLastTrack($scope.currentCoordinates, $scope.currentFeature);
            var resExt = $scope.drawLastTrack($scope.currentCoordinatesExt, $scope.currentFeatureExt);
            $scope.currentFeature = res[0];
            $scope.currentFeatureExt = resExt[0];

            $scope.updateVertexLayer();

            res[1].forEach(function(coord, coordIndex){
                var featPoint = new ol.Feature({
                    is_valid: false,
                    from_device: false,
                    counter: coordIndex === res[1].length-1 ? 0 : coordIndex+1,
                    in_corso: true
                });
                featPoint.setGeometry(new ol.geom.Point(coord));
                $scope.layerVerticiTracks.getSource().addFeature(featPoint);
            });

            resExt[1].forEach(function(coord, coordIndex){
                var featPoint = new ol.Feature({
                    is_valid: false,
                    from_device: true,
                    counter: coordIndex === resExt[1].length-1 ? 0 : coordIndex+1,
                    in_corso: true
                });
                featPoint.setGeometry(new ol.geom.Point(coord));
                $scope.layerVerticiTracks.getSource().addFeature(featPoint);
            });
        };

		$scope.updateVertexLayer = function(){
            $scope.layerVerticiTracks.getSource().clear();
            $scope.layerTracks.getSource().getFeatures().forEach(function(feature){
                var geom_f = feature.getGeometry();
                if (geom_f){
                    var coordinates = geom_f.getCoordinates();
                    coordinates.forEach(function(ring){
                        ring.forEach(function(coord, coordIndex){
                            var featPoint = new ol.Feature({
                                is_valid: true,
                                from_device: feature.get("from_device"),
                                in_corso: feature.get("in_corso")
                            });
                            //counter: coordIndex === ring.length-1 ? 0 : coordIndex+1,
                            featPoint.setGeometry(new ol.geom.Point(coord));
                            $scope.layerVerticiTracks.getSource().addFeature(featPoint);
                        });
                    });
                }
            });
		};

		$scope.minSelPointChanged = function(){
		    if ($scope.rangePoints.min > $scope.rangePoints.max){
                $scope.rangePoints.max = $scope.rangePoints.min;
            }
        };

		$scope.maxSelPointChanged = function(){
            if ($scope.rangePoints.max < $scope.rangePoints.min){
                $scope.rangePoints.min = $scope.rangePoints.max;
            }

        };

		$scope.toggleTrackPath = function(riprendi){
            var deferred = $q.defer();




			if (!$scope.tracciaPercorso.attivo){ //appena accesso
                var deferred = $q.defer();

                usbService.setGetAltitudeFromServiceBoolean(true);

                $scope.currentCoordinates = [];
                $scope.currentCoordinatesExt = [];
                var starter = false;

                if ($scope.selectInteraction.getFeatures().getArray().length > 0){
                    $scope.featSelected = $scope.selectInteraction.getFeatures().getArray()[0];

                    $scope.rangePoints = {"min": 1, "max": 1};
                    PopupFactory.popupWithTemplate("trackPath.popups.selPoints", "templates/mappa/selPoints.html", $scope).then(function(){
                        var pointsSelected = $scope.featSelected.getGeometry().getCoordinates()[0].slice($scope.rangePoints.min-1, $scope.rangePoints.max);

                        if ($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 1 ) {
                            $scope.currentCoordinates = pointsSelected;
                        }
                        if ($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 0 ){
                            $scope.currentCoordinatesExt = pointsSelected;
                        }

                        $scope.selectInteraction.getFeatures().clear();
                        $scope.overlayInfo.setPosition(undefined);
                        deferred.resolve();
                        starter = true
                    });

                } else {
                    deferred.resolve();
                }

                deferred.promise.then(function(){
                    riprendi = riprendi || false;
                    $scope.tracciaPercorso.attivo = !$scope.tracciaPercorso.attivo;

                    if (starter) $scope.startPauseTrackPath();

                    $scope.currentFeature = new ol.Feature({
                        date: appService.getCurrentDate(),
                        in_corso: true,
                        from_device: false,
                        pkuid: OL3Map_service.getNewPKUID($scope.layerTracks),
                        uploaded: false
                    });

                    $scope.currentFeatureExt = new ol.Feature({
                        date: appService.getCurrentDate(),
                        in_corso: true,
                        from_device: true,
                        pkuid: OL3Map_service.getNewPKUID($scope.layerTracks),
                        uploaded: false
                    });

                    $scope.layerTracks.getSource().addFeature($scope.currentFeature);
                    $scope.layerTracks.getSource().addFeature($scope.currentFeatureExt);
                    $scope.toggle_track = $filter('translate')('trackPath.control.stop_track');
                    $scope.gnss_info = $filter('translate')('baseMap.gnss_info');
                    $scope.menu_stati.control.btns.toggle_track.icon = 'ion-stop';
                    $scope.centerOnMe();

                    if ($scope.metadata.utilizzo_attivo === 1) {
                        setTimeout(function(){
                            $scope.startPauseTrackPath();
                        }, parseInt($scope.metadata.utilizzo_tempo) * 1000);
                    }
                });



      } else { //appena spento

                riprendi = riprendi || false;
                $scope.tracciaPercorso.attivo = !$scope.tracciaPercorso.attivo;
                if(riprendi){
                    $scope.togglePauseTrackPath();
                }else{
                    $scope.stopPauseTrackPath();
                }
                $scope.toggle_track = $filter('translate')('trackPath.control.toggle_track');
                $scope.menu_stati.control.btns.toggle_track.icon = 'ion-play';

                $scope.currentFeature.set("in_corso", false);
                $scope.currentFeatureExt.set("in_corso", false);

                var features = [$scope.currentFeature, $scope.currentFeatureExt];

                features.forEach(function(feature){
                    if (feature && feature.getGeometry() && feature.getGeometry().getCoordinates()[0].length >= 3){
                        MappaService.updateCodiceTracks(
                            feature.get("date"),
                            feature.get("pkuid"),
                            coordinatesService.OLPolygonToWkt(feature.getGeometry()),
                            feature.get("from_device")
                        );
                    }

                });

                $scope.updateVertexLayer();

            //PER RACCOLTA GPX
            if(!$scope.prima_rilevaz){
              usbService.startSensorCommunication();
            }

            }
      deferred.resolve("");
      return deferred.promise;
		};

		$scope.togglePauseTrackPath = function () {
          var deferred = $q.defer();
                $scope.tracciaPercorso.pausa = !$scope.tracciaPercorso.pausa;
          if ($scope.tracciaPercorso.pausa){ //appena pausato
              $scope.pause_track = $filter('translate')('trackPath.control.riprendi_track');
              $scope.menu_stati.control.btns.pause_track.icon  = 'ion-play';
              }
          else { //appena ripreso
              if ($scope.metadata.utilizzo_attivo === 1 &&
                  $scope.toggle_track == $filter('translate')('trackPath.control.stop_track')) {
                  setTimeout(function () {
                      $scope.startPauseTrackPath();
                  }, parseInt($scope.metadata.utilizzo_tempo) * 1000);
              }
              $scope.pause_track = $filter('translate')('trackPath.control.pause_track');
              $scope.menu_stati.control.btns.pause_track.icon  = 'ion-pause';
          }

          $scope.showPopupInfoFlag =  (!$scope.tracciaPercorso.attivo || !$scope.tracciaPercorso.pausa);

          //$scope.chiudiMenu('control');
          deferred.resolve("");
          return deferred.promise;
    };

		$scope.stopPauseTrackPath = function(){
            $scope.tracciaPercorso.pausa = false;
            $scope.pause_track = $filter('translate')('trackPath.control.pause_track');
            $scope.menu_stati.control.btns.pause_track.icon  = 'ion-pause';
        };

		$scope.startPauseTrackPath = function(){
            $scope.tracciaPercorso.pausa = true;
            $scope.pause_track = $filter('translate')('trackPath.control.riprendi_track');
            $scope.menu_stati.control.btns.pause_track.icon  = 'ion-play';
        };

        $scope.$on('$ionicView.leave',function() {
        	$scope.map.un('dblclick',  $scope.onClickPolygon);
        	$scope.map.un('singleclick',  $scope.onSingleClick);
          if ($scope.tracciaPercorso.attivo) {
              $scope.toggleTrackPath(false);
              sessionService.pauseTrackingSession({'layerTracks':$scope.layerTracks, 'layerVerticiTracks':$scope.layerVerticiTracks});
          }else{
            sessionService.clearTrackingPauseDate();
          }
        });

        $scope.$on('$ionicView.enter',function() {
          $scope.prima_rilevaz = true;
          $scope.conta_rilevaz = 0;
        });

     //PER RACCOLTA GPX

          $rootScope.$on('deviceSensorSuccess1', function(events, args){
          });

          $scope.temp_first = "n/d";
          $scope.prima_rilevaz = true;
          $scope.temp_last = "n/d";
          $scope.conta_rilevaz = 0;

          $rootScope.$on('nmeaVTGReceived', function(events, args){
            var data = args[0];
            $scope.speed_kmh = data.speed_kmh;
          });

          $rootScope.$on('deviceSensorSuccess2', function(events, args){
            var meas = args[0];
            $scope.conta_rilevaz += 1;
            if($scope.conta_rilevaz >= 5){
              usbService.stopCommunication();
              if($scope.prima_rilevaz){
                $scope.prima_rilevaz = false;
                usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
                $scope.conta_rilevaz = 0;
              }else{
                usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
                $scope.prima_rilevaz = true;
                $scope.conta_rilevaz = 0;
              }
            }else{
              switch(meas){
                case 'meteo':
                  if(!isNaN(args[9]) && args[9] !== null && args[9] !== undefined){
                    usbService.stopCommunication();
                    if($scope.prima_rilevaz){
                      $scope.temp_first = args[9];
                      $scope.prima_rilevaz = false;
                      usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
                      $scope.conta_rilevaz = 0;
                    }else{
                      $scope.temp_last = args[9];
                      usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
                      $scope.prima_rilevaz = true;
                      $scope.conta_rilevaz = 0;
                    }
                  }
                  break;
                default:
                  break;
              }
            }
          });

	};


trackPathMapOlCtrl.$inject = ['$scope', '$controller', '$rootScope', '$ionicPopup', '$ionicModal', '$ionicPlatform',
  'sqliteService', 'OL3Map_service', 'OL3Layers_service', 'appService', 'PopupFactory',
  'track_path_menus', 'track_path_menu_states', 'track_path_loading_status',
  'MappaService', 'coordinatesService', 'JSTS_service', 'httpService', 'sessionService', '$q', '$filter', 'usbService', '$ionicLoading', 'gpsDiagnosticService'];
angular.module('mappa.module').controller('trackPathMapOlCtrl', trackPathMapOlCtrl);
