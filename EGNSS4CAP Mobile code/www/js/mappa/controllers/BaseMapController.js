var BaseMapController = function($scope, $state, $ionicHistory, $rootScope, PopupFactory, $q, $ionicPlatform,
                                 sessionService, gpsDiagnosticService, MappaService, OL3Map_service,
                                 coordinatesService ,usbService, gpsService, OL3Layers_service,
                                 appService, JSTS_service, sqliteService, baseLayersTypes, $filter, $ionicLoading)
  {

        $scope.curBestLocation = null;
        $scope.curBestProvider = "";
        $scope.currAccuracy = 999;

        $scope.coordinatesNeedConversion = false;

        if(!$rootScope.deviceEnabled && !$rootScope.firstConnection){
          $rootScope.deviceEnabled = false;
          $rootScope.firstConnection = true;
          console.log("SETTATI DEVICE E FIRSTC")
        }
        $scope.firstTimeLoaded = false;
        $scope.showPopupInfoFlag = true;

        $scope.info_gnss_icon = "ion-clipboard";

        $scope.$on('$ionicView.leave', function(){
        	$scope.chiudiTuttiMenu();
        	$scope.removeListeners();
            OL3Map_service.removeAllLayers($scope.map);
            $scope.removeInteractions();
            $scope.map.removeOverlay($scope.overlayInfo);
        });

        $scope.$on('$ionicView.afterEnter', function() {
          $rootScope.deviceEnabled = usbService.isGpsActive();
          $rootScope.survey_active = false;
          $scope.loading_status.pos = false;
          $scope.loading_status.part = false;
          $scope.featureStartGeom = null;
          $scope.featureEndGeom = null;
          gpsDiagnosticService.checkAvailability(
            $scope.loadSettingsThenLayers,
            function(){console.log("GPS UNAVAILABLE!")}
          );
          $scope.createListeners();
        });

        $scope.loadSettingsThenLayers = function(){
            $scope.getSettings().then(function(){
              if($scope.metadata.GPSprovider !== 1){
                $rootScope.firstConnection = false;
                if(!$rootScope.deviceEnabled){
                  $scope.checkPreToggleUsbCommunication();
                  $rootScope.deviceEnabled = true;
                  usbService.setGpsStatus(true);
                }else{
                }
              }else{
                if($rootScope.deviceEnabled){
                  $rootScope.firstConnection = false;
                  $scope.toggleUsbCommunication();
                  usbService.setGpsStatus(false);
                }
              }
              $scope.initializeMap();
            }).then(function(){
              $scope.reloadBaseLayers();
          })
        };

        $scope.resetMapController = function(){
            if ($scope.firstTimeLoaded){
                $scope.loading_status.pos = false;
                $scope.loading_status.part = false;
                $scope.removeListeners();
                OL3Map_service.removeAllLayers($scope.map);
                $scope.removeInteractions();
                $scope.map.removeOverlay($scope.overlayInfo);
                gpsService.stopBackgroundTracking();
                usbService.stopCommunication();

            } else {
                $scope.firstTimeLoaded = true;
            }
        };

        $scope.initializeMap = function(){
            	var deferred = $q.defer();
            	if(!$scope.map){
           			$scope.map = OL3Map_service.createMap($scope.map_widget_id, 3, sessionService.getAuthId(), sessionService.getCodiRegi(), $scope.default_tile);
                    $scope.map.getView().setCenter([0,0]);
           			$scope.map.on('singleclick', $scope.showPopupInfo);
            	}else{
            		console.log("mappa già creata");
            	}
            	deferred.resolve($scope.afterMapInitialized());
            	return deferred.promise;
        };

        $scope.gotoListaPart = function(){
          var deferred = $q.defer();
          $state.go('lavorazioni');
          deferred.resolve("");
          return deferred.promise;
        };

        $rootScope.deviceEnabled = false;

        $scope.goToGpsSettings = function(){
          var deferred = $q.defer();
          $state.go('sensoregps');
          deferred.resolve("");
          return deferred.promise;
        };

        $scope.toggleSamplingModeBtn = function(){
          var deferred = $q.defer();
          $rootScope.survey_active = !$rootScope.survey_active;
          if($rootScope.survey_active){
            usbService.attiva_interval_centroid();
            $scope.showSurveyLoadingPopup();
          }
          deferred.resolve("");
          return deferred.promise;
        };

        $scope.checkPreToggleUsbCommunication = function(){
          gpsDiagnosticService.checkAvailability(
            $scope.toggleUsbCommunication(),
            function(){console.log("GPS UNAVAILABLE!")});
        };

        $scope.toggleUsbCommunication = function(){
          console.log($scope.metadata);
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

        $scope.centerOnMe = function(){
          var deferred = $q.defer();
          var coord = $scope.getPositionFeature().getGeometry().getCoordinates();
          for(var i=0; i<coord.length; i++){
            if(typeof coord[i] === "string"){
              coord[i] = parseFloat(coord[i])
            }
          }

          if(!isNaN(coord[0]) && !isNaN(coord[1]) && !isNaN(coord[2])){
              $scope.map.getView().setCenter(coord);
              $scope.flag_center_on_me_first = false;
            }

          var zoomToReach;
          if($scope.default_tile.id == "OSM" || $scope.default_tile.id == "GOOGLE_MAPS"){
                  zoomToReach = 18;
          }else{
                  zoomToReach = 15;
          }
          if ($scope.map.getView().getZoom() < zoomToReach){
              $scope.map.getView().setZoom(zoomToReach);
          }
          $scope.chiudiMenu('zoom');
          deferred.resolve("");
          return deferred.promise;
        };

        $scope.centerOnPoint = function(coord){
            for(var i=0; i<coord.length; i++){
              if(isNaN(coord[i])){
                return;
              }
            }
            $scope.map.getView().setCenter(coord);
        };

        $scope.chiudiMenu = function(menu){
            $scope.menu_stati[menu].state = $scope.stati_validi.close;
        };

        $scope.chiudiTuttiMenu = function(){
        	Object.keys($scope.menu_stati).forEach(function(menu){
        		$scope.menu_stati[menu].state = $scope.stati_validi.close;
            });
        };

        $rootScope.$on('meanCentroidComputed', function(events, args){
          var lat, lon;

          lat = args[3];
          lon = args[2];

          try{
            var styles = {
              'meanCentroidMarker': new ol.style.Style({
                image: new ol.style.Circle({
                  radius: 13,
                  snapToPixel: false,
                  fill: new ol.style.Fill({color: 'green'}),
                  stroke: new ol.style.Stroke({
                    color: 'white', width: 2
                  })
                })
              })
            };

            var mean_centroid_marker = new ol.Feature({
              type: 'meanCentroidMarker',
              geometry: new ol.geom.Point([lon, lat, 0]),
              text: ''
            });

            var features_list = [mean_centroid_marker];
            var layersToAdd = [];

            var vectorLayer = new ol.layer.Vector({
              source: new ol.source.Vector({
                features: features_list
              }),
              style: function(feature) {
                return styles[feature.get('type')];
              }
            });
            layersToAdd.push(vectorLayer);
            OL3Map_service.addLayers($scope.map, layersToAdd);
          }catch(err){
            console.log(err);
          }
        });

        $scope.timestamp = "";
        $scope.flag_center_on_me_first = true;
        var d = new Date();
        var year = d.getUTCFullYear();
        var month = d.getUTCMonth() + 1;
        var day = d.getUTCDate();
        var hours = d.getUTCHours();
        var mins = d.getUTCMinutes();
        var secs = d.getUTCSeconds();
        $rootScope.timestamp_to_crypt = day + '-' + month + '-' + year + ' ' + hours + ':' + mins + ':' + secs;
        $rootScope.timestamp_source = 'phone';

        $scope.createListeners = function(){

            document.addEventListener("offline", $scope.goOffline, false);

            document.addEventListener("online", $scope.goOnline, false);

            $scope.surveyCountdownListener = $rootScope.$on('surveyCountdown', function(events, args){
              if(args[0] === 0){
                if($scope.surveyLoadingPopup !== undefined && $scope.surveyLoadingPopup !== null){
                  $scope.surveyLoadingPopup.close();
                }
              }else{
                if(document.getElementById("countdown_survey") !== null && document.getElementById("countdown_survey") !== undefined){
                  document.getElementById("countdown_survey").innerHTML = args[0];
                }
              }
            });

            $scope.mockupProviderListener = $rootScope.$on('mockupProvider', function(event, args){
              $ionicLoading.hide();
            });

            $scope.locationAcquiredListener = $rootScope.$on('locationAcquired', function(events, args){
                var center = new ol.geom.Point([args[1].x, args[1].y, args[1].z]);
                if(args[1].latitude !== "" && args[1].latitude !== " " && args[1].latitude !== undefined){
                  $scope.curr_lat = args[1].latitude;
                }
                if(args[1].longitude !== "" && args[1].longitude !== " " && args[1].longitude !== undefined) {
                  $scope.curr_lon = args[1].longitude;
                }
                if(args[1].n_sat !== "" && args[1].n_sat !== " " && args[1].n_sat !== undefined) {
                  $scope.tot_sat_number = args[1].n_sat;
                }
                var goodPoint = true;
                if(isNaN(args[1].x) || isNaN(args[1].y) || args[1].x === 'NaN' || args[1].y === 'NaN'){
                  goodPoint = false;
                }

                $scope.curBestLocation = args[1];
                $scope.curBestProvider = args[0];

                $rootScope.timestamp_to_crypt = args[2];
                $rootScope.timestamp_source = 'gps';

                if($scope.curBestProvider === 'device'){
                    if(goodPoint){
                      $ionicLoading.hide();
                      $scope.positionFeatureExt.setGeometry(center);
                      $scope.layerPositionExt.setVisible($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 0);
                    }
                }
                else{
                  if(!$rootScope.survey_active){
                    $ionicLoading.hide();
                    $scope.positionFeature.setGeometry(center);
                    $scope.layerPosition.setVisible($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 1);
                  }
                }

                $scope.locationAcquired($scope.curBestProvider, center, [args[0], args[1]]);

                if(!$scope.loading_status.pos){
                    if ($scope.featureStart && !$scope.featureStart.getGeometry()){
                        $scope.featureStart.setGeometry(center.clone());
                    }

                    appService.safeApply($rootScope, function () {
                        $scope.loading_status.pos = true;
                    });

                    if($rootScope.deviceEnabled) {
                      $scope.positionFeatureExt.setGeometry(center.clone());
                    }
                    else {
                      $scope.positionFeature.setGeometry(center.clone());
                    }

                    $scope.centerOnMe();
                }

                if($scope.flag_center_on_me_first){
                  $scope.centerOnMe();
                }
            });

            $scope.nmeaGSAListener = $rootScope.$on('nmeaGSAReceived', function(events, args){
                var data = args[0];
                var network = args[1];

                if(network == 'gn'){
                    $scope.fix_mode = data.fix_mode;
                    $scope.hdop = data.hdop;
                    $scope.pdop = data.pdop;
                    $scope.vdop = data.vdop;
                    $scope.currAccuracy = parseFloat(data.hdop);
                }
            });

          $scope.nmeaTotSatNumberListener = $rootScope.$on('nmeaTotSatNumber', function(events, args){
            $scope.tot_sat_number = args[0];
          });

          $rootScope.$on('meansnrMessage', function(events, args){
            $scope.mean_snr = args[0];
          });

          $scope.deviceErrorListener = $rootScope.$on('deviceError', function(events, args){
               if($rootScope.deviceEnabled){
                $rootScope.firstConnection = true;
                $rootScope.deviceEnabled = false;
                usbService.setGpsStatus($rootScope.deviceEnabled);
                PopupFactory.showPopup("baseMap.popups.extDeviceError");
              }
          });
        };

        $scope.removeListeners = function(){

            document.removeEventListener("offline", $scope.goOffline, false);
            document.removeEventListener("online", $scope.goOnline, false);

            $scope.locationAcquiredListener();
            $scope.nmeaGSAListener();
            $scope.deviceErrorListener();

        };

        $scope.getSettings = function(){
        	var deferred = $q.defer();
            sqliteService.getMetadata(
            		function(resmetadata){
                    $scope.metadata = resmetadata;
                    $scope.default_tile = null;
                    $scope.abilitaMacroaree = $scope.metadata['macroAree'] === 0;
                    sqliteService.getDefaultTileLayerId(
                        function(reslayers){
                            $scope.default_tile = baseLayersTypes[reslayers.rows.item(0).value];
                            if($scope.default_tile.id == "OSM" || $scope.default_tile.id == "GOOGLE_MAPS"){
                            	$scope.coordinatesNeedConversion = false;
                            }else{
                            	$scope.coordinatesNeedConversion = true;
                            }
                            deferred.resolve("");
                        }, function(error){
                        	deferred.resolve("");
                        });
                }, function(error){
                	deferred.resolve("");
                });
            return deferred.promise;
        };

        $scope.reloadBaseLayers = function(){

            $scope.layerOSM_online = OL3Layers_service.getLayerOnlineOSM("12", "3004");
            $scope.layerOSM_online.set("name", "ONLINE OSM");

            $scope.layerGoogleMaps_online = OL3Layers_service.getLayerOnlineGoogleMaps("12", "3004");
            $scope.layerGoogleMaps_online.set("name", "ONLINE GOOGLE MAPS");

            var layer_pos = OL3Layers_service.getLayerPosition();
            $scope.layerPosition = layer_pos.layer;
            $scope.positionFeature = layer_pos.feature;
            $scope.layerPosition.set("name", $filter('translate')('BaseMapContrller.position'));

            var layer_pos_external = OL3Layers_service.getLayerPositionExt();
            $scope.layerPositionExt = layer_pos_external.layer;
            $scope.positionFeatureExt = layer_pos_external.feature;
            $scope.layerPositionExt.set("name", $filter('translate')('BaseMapContrller.device position'));

            var layersToAdd = [];

            if($scope.default_tile.id === "GOOGLE_MAPS"){
              layersToAdd.push($scope.layerGoogleMaps_online);
            }
            if($scope.default_tile.id === "OSM"){
            	layersToAdd.push($scope.layerOSM_online);
            }

            if (navigator.connection.type === Connection.NONE) {
                $scope.layerOSM_online.setVisible(false);
            } else {
                $scope.layerOSM_online.setVisible(true);
            }

            layersToAdd.push($scope.layerPosition);
            layersToAdd.push($scope.layerPositionExt);

            $scope.layerState = [];

            $scope.gnssInfo = [
              {"name": "Lat", "value":$scope.curr_lat, "acceso" : true, 'layerObj' : [$scope.layerSuoli]},
              {"name": "Lon", "value":$scope.curr_lon, "acceso" : true, 'layerObj' : [$scope.layerParticelle]}
            ];

            OL3Map_service.addLayers($scope.map, layersToAdd);
            $scope.reloadExternalLayers();

            $scope.addInteractions();

            $scope.overlayInfo = new ol.Overlay({
                element: $scope.popupContainer,
                autoPan: true,
                stopEvent: false,
                autoPanAnimation: {
                    duration: 250
                }
            });

            $scope.map.addOverlay($scope.overlayInfo);

            gpsService.startBackgroundTracking(sessionService.getAuthId(),  ($scope.default_tile.id == "OSM" || $scope.default_tile.id == "GOOGLE_MAPS" ? false : true));
        };

        $scope.getPositionFeature = function(){
          if($scope.metadata.GPSprovider === 2 || $scope.metadata.GPSprovider === 1){
            var geom = [NaN, NaN];
            if($scope.positionFeatureExt.getGeometry().getCoordinates() !== undefined){
              geom = $scope.positionFeatureExt.getGeometry().getCoordinates();
            }
            if($scope.metadata.GPSprovider === 2 && $scope.positionFeatureExt !== undefined && geom[0] !== undefined && !isNaN(geom[0]) && geom[1] !== undefined && !isNaN(geom[1])){
              return $scope.positionFeatureExt;
            }else{
              return $scope.positionFeature;
            }
          }else{
            if( $rootScope.deviceEnabled && !$rootScope.from_mock) {
              return $scope.positionFeatureExt;
            }
            else {
              return $scope.positionFeature;
            }
          }
        };

        $scope.toggleLayerVisibility = function(layer){
            layer.layerObj.forEach(function(olLayer){
                olLayer.setVisible(layer.acceso);
            });
        };

        $scope.showSurveyLoadingPopup = function(){
          var deferred = $q.defer();
          $scope.chiudiMenu("zoom");
          $scope.surveyLoadingPopup = PopupFactory.popupWithTemplateNoButtons("baseMap.popups.surveyCountdown", "templates/mappa/surveyLoadingPopup.html", $scope);
          deferred.resolve("");
          return deferred.promise;
        };

        $scope.showVisibilityPopup = function(){
          var deferred = $q.defer();
          $scope.chiudiMenu("zoom");
          PopupFactory.popupWithTemplate("baseMap.popups.layerVisibility", "templates/mappa/visibilityPopup.html", $scope);
          deferred.resolve("");
          return deferred.promise;
        };

        $scope.showGnssInfoPopup = function(){
          var deferred = $q.defer();
          $scope.chiudiMenu("zoom");
          PopupFactory.popupWithTemplate("baseMap.popups.gnssInfo", "templates/mappa/gnssPopup.html", $scope);
          deferred.resolve("");
          return deferred.promise;
        }
    };

BaseMapController.$inject = ['$scope', '$state', '$ionicHistory', '$rootScope', 'PopupFactory', '$q', '$ionicPlatform',
  'sessionService', 'gpsDiagnosticService', 'MappaService', 'OL3Map_service',
  'coordinatesService', 'usbService', 'gpsService', 'OL3Layers_service',
  'appService',  'JSTS_service', 'sqliteService', 'baseLayersTypes','$filter', '$ionicLoading'];
angular.module('mappa.module').controller('BaseMapController', BaseMapController);
