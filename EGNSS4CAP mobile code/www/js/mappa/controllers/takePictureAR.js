var takePictureARCtrl = function ($scope, $state, $ionicNavBarDelegate, $ionicPlatform, $rootScope, $ionicHistory, $stateParams, $timeout, $q, MappaService, sessionService, gpsService, coordinatesService, JSTS_service, $interval,usbService,sqliteService, appService, baseLayersTypes, httpService, $filter) {

        	var canStart = false;
        	$scope.Math = Math;
        	$scope.hideButtons = true;
        	$scope.hideControls = true;
        	$scope.hideQr = false;
        	$scope.hideInclinometer = true;
        	$scope.polyFill = false;
        	$scope.coordinatesNeedConversion = false;
            $scope.startPointLat = $stateParams.startPointLat;
            $scope.startPointLng = $stateParams.startPointLng;
            $scope.motiviFoto = $stateParams.reasons;
            $scope.manualControls = false;
            $scope.fakeGpsPosFrommap= $stateParams.fakegpspos;
            $scope.posizioneGPSIniziale;
            $scope.hideInitialCountdown = false;
            $scope.startingPoint = {"x":0, "y":0};
            $scope.initialCountdown = 2; //30;
            $scope.gpsHeight = 0;
            $scope.nmea_height = 0;
            $scope.fakePosition;
            $scope.positionAcquired = false;
            $scope.maincamera;
            $scope.sprites = [];
            $scope.nmea_chars = 0;
            $scope.max_nmea_chars = 10000;
            $scope.mean_centroid_arrived = false;
            $scope.precision = "n/d";

            $scope.toggleViewTransparent = function(flag){
            	if(flag){
            		$("#page3").addClass("transparentView");
            		$("#sideMenus").addClass("transparentView");
            		$("#side-leftMenu").addClass("hide");
            		$("#side-menuAlsoRight").addClass("hide");
            		$("#sidemenucontent").addClass("transparentView");
                $("#sidemenucontent").removeClass("sideMenuContentVisible");
              }else{
            		$("#page3").addClass("transparentView");
            		$("#sideMenus").removeClass("transparentView");
            		$("#side-leftMenu").removeClass("hide");
            		$("#side-menuAlsoRight").removeClass("hide");
                $("#sidemenucontent").addClass("sideMenuContentVisible");
            		$("#sidemenucontent").removeClass("transparentView");
            	}
            };

            gpsService.stopBackgroundTracking();
            $scope.default_tile;
            $scope.isFakeGPSEnabled = true;

            $scope.getSettings = function(){
              var deferred = $q.defer();
                sqliteService.getMetadata(
                    function(resmetadata){
                        console.log(resmetadata);
                        $scope.metadata = resmetadata;
                        $scope.isFakeGPSEnabled = ($scope.metadata.fakephotogps == '1'? false : true);
                        $scope.default_tile = null;
                        $scope.abilitaMacroaree = $scope.metadata['macroAree'] === 0;
                        sqliteService.getDefaultTileLayerId(
                            function(reslayers){
                                $scope.default_tile = baseLayersTypes[reslayers.rows.item(0).value];
                                //osm per 4326
                                if($scope.default_tile.id === 'OSM' || $scope.default_tile.id === 'GOOGLE_MAPS'){
                                	$scope.coordinatesNeedConversion = false;
                                }else{
                                	$scope.coordinatesNeedConversion = true;
                                }
                                deferred.resolve("");
                            }, function(error){
                            	console.log("Error loading default tile settings");
                            	deferred.resolve("");
                            });
                    }, function(error){
                    	console.log("Error loading metadata");
                    	deferred.resolve("");
                    });
                return deferred.promise;
            };

            $scope.takePicture = function(){
              var heading = $scope.currentHeadingNum;
              if($rootScope.correctionModePicture === 1){
                $scope.takeSnapshot([$scope.mean_centroid.x, $scope.mean_centroid.y], heading, 1);
              }else{
                if($rootScope.correctionModePicture === 2){
                  //correzione ntrip
                }else{
                  $scope.takeSnapshot([$stateParams.startPointLat, $stateParams.startPointLng], heading, 0); //Timeout all'interno
                }
              }
            };

            $scope.container;
            $scope.cameraFOV;
            $scope.headingWatcher;
            $scope.currentHeadingTxt = "0° N  0° Tilt";
            $scope.currentHeadingNum = 0;

            $scope.compassSuccess = function(heading){
            	appService.safeApply($rootScope, function () {
            		$scope.currentHeadingNum = heading.magneticHeading; //verticale
            		$scope.currentHeadingTxt = Math.round($scope.currentHeadingNum,0) +"° N  "+Math.round($scope.tiltAngle,0)+"° Tilt";
            		if(heading.magneticHeading !== 0 && typeof $scope.startingPoint !== 'undefined'){
            			canStart = true;
            		}
                });
            };

            $scope.compassError = function(compassError){
            	$scope.currentHeadingTxt = "Error  "+Math.round($scope.tiltAngle,0)+"° Tilt";
            };

            $scope.createScene = function(){
            	$scope.init([]);
            };

            if (window.ezar) {
                ezar.initializeVideoOverlay(
                    function() {

                    	$scope.headingWatcher = navigator.compass.watchHeading($scope.compassSuccess, $scope.compassError);
                    	$scope.maincamera = ezar.getBackCamera();
                    	$scope.cameraFOV =  $scope.maincamera.getHorizontalViewAngle();
                    	$scope.getSettings().then(function(){
                    		 $scope.maincamera.start(function success(data){
     	                    	$timeout(function(){
     	                    		$scope.toggleViewTransparent(true);
     	                    	},1000);
     	                    	$scope.startCountdown();

     	                    },function failure(err){
     	                    	alert('unable to init backcamera for ezar: ' + err);
     	                    });
                    	});

                    },
                    function(err) {
                    alert('unable to init ezar: ' + err);
                }, {
                            backgroundColor: '#FFFFFF',
                            fitWebViewToCameraView: false
                         });
            } else {
                alert('Unable to detect the ezAR plugin');
            }

            $scope.currentCamPosition = 0;
            $scope.posWatcher;
            var startTimer;
            $scope.startCountdown = function(){

              if($rootScope.correctionModePicture === 1){
                //CONVEX HULL CORRECTION
                $rootScope.survey_active = true;
                $scope.hideInitialCountdown = false;
                $scope.getStartingPosition();
                startTimer = $interval(function(){
                  $scope.hideButtons = true;
                  if($scope.mean_centroid_arrived && canStart){
                    $scope.initialCountdown = 0;
                    $scope.hideButtons = false;
                    $scope.hideInitialCountdown = true;
                    $scope.createScene();
                    $interval.cancel(startTimer);
                  }
                },1000);

              }else{
                if($rootScope.correctionModePicture === 2){
                  //NTRIP CORRECTION

                }else{
                  //NO CORRECTION
                  $scope.hideInitialCountdown = false;
                  $scope.getStartingPosition();
                  startTimer = $interval(function(){
                    $scope.initialCountdown--;
                    $scope.hideButtons = true;
                    if($scope.initialCountdown <= 0 && canStart){
                      $scope.initialCountdown = 0;
                      $scope.hideButtons = false;
                      $scope.hideInitialCountdown = true;
                      $scope.createScene();
                      $interval.cancel(startTimer);
                    }
                  },1000);
                }
              }
            };

            $scope.getStartingPosition = function(){
              if(!usbService.isGpsActive()){
                usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
                usbService.setGpsStatus(true);
              }
            	gpsService.startForegroundTracking(false);
            };

            $scope.inclinometerInterval;
            $scope.inclinationX = 0;
            $scope.inclinationZ = 0;
            $scope.sceneGroup;
            $scope.tiltAngle = 0;
            $scope.no_take = false;
            $scope.alert_txt = "";

            $scope.getTiltAngle = function(eventData) {
            // gamma is the left-to-right tilt in degrees, where right is positive
              var tiltLR = eventData.gamma;

              // beta is the front-to-back tilt in degrees, where front is positive
              var tiltFB = eventData.beta;

              if($scope.screen_orientation === "landscape-primary" || $scope.screen_orientation === "landscape-secondary"){
                $scope.tiltAngle = Math.abs(tiltLR);
                $scope.min_tilt = 60;
                $scope.max_tilt = 90;
              }else{
                $scope.tiltAngle = tiltFB;
                $scope.min_tilt = 50;
                $scope.max_tilt = 100;
              }
              if($scope.tiltAngle < $scope.min_tilt || $scope.tiltAngle > $scope.max_tilt){
                if(!$scope.no_take){
									if($scope.screen_orientation === "landscape-primary" || $scope.screen_orientation === "landscape-secondary"){
											$scope.alert_txt = $filter('translate')('picturePath.popups.tilt_error_msg_h');
									}
									else{
										$scope.alert_txt = $filter('translate')('picturePath.popups.tilt_error_msg_v');
									}

                  document.getElementById("camera_btn").disabled = true;
                  document.getElementById("camera_btn_icon").class = "icon ion-android-camera stable";
                  $scope.no_take = true;
                }
              }else{
                if($scope.no_take){
                  $scope.alert_txt = "";
                  document.getElementById("camera_btn").disabled = false;
                  document.getElementById("camera_btn_icon").class = "icon ion-android-camera energized";
                  $scope.no_take = false;
                }
              }

              var dir = eventData.alpha;
          };

          $scope.screen_orientation = "portrait-primary";

          $scope.init = function(poligons){

              window.screen.orientation.unlock();

              window.addEventListener('deviceorientation', $scope.getTiltAngle, false);
              window.addEventListener("orientationchange", function(){
                $scope.screen_orientation = screen.orientation.type;
              });

              $scope.container = document.getElementById( 'container' );

            };

            $scope.takeSnapshot = function(latLng, currentHeading, centroid_true_false){

                window.cell_info.getNetworkInfo(
                  function(networkInfoData){
                    if(networkInfoData.mcc === 0 || networkInfoData.mnc === 0 || networkInfoData.netOp === ""){
                      alert($filter('translate')('picturePath.popups.no_sim_msg'));
                    }else{
                      window.removeEventListener('deviceorientation', $scope.getTiltAngle);
                      navigator.compass.clearWatch($scope.headingWatcher);

                      var photoDataObject = {
                        "pointLat": latLng[1],
                        "pointLng": latLng[0],
                        "heading": currentHeading,
                        "altitude": $scope.nmea_height,
                        "altitude_locmanager": $scope.lm_altitude,
                        "date" : $scope.getCurrentDate(),
                        "uuid" : window.device.uuid,
                        "fov" : $scope.cameraFOV,
                        "canvas_h" : window.innerHeight,
                        "canvas_w" : window.innerWidth,
                        "manual" : $scope.manualControls,
                        "uploaded" : false,
                        "ext" : "jpeg",
                        "network_info":networkInfoData,
                        "tilt_angle":Math.round($scope.tiltAngle),
                        "username":localStorage.getItem("username"),
                        "s_lat":$scope.s_lat,
                        "s_lon":$scope.s_lon,
                        "device_manufacturer":device.manufacturer,
                        "device_model":device.model,
                        "device_platform":device.platform,
                        "device_version":device.version,
                        "centroid_used":centroid_true_false,
                        "sats_info":$rootScope.curr_sats_info,
                        "accuracy":$rootScope.loc_manager_accuracy,
                        "precision":$scope.precision,
                        "no_extra":$rootScope.no_extras,
                        "extra_sat_count":$rootScope.extra_sat_number
                      };
                      $rootScope.curr_sats_info = [];

                      appService.safeApply($rootScope, function () {
                        $scope.hideButtons = true;
                        $scope.hideControls = true;
                        $ionicNavBarDelegate.title("");
                      });

                      $timeout(function(){
                        ezar.snapshot(function success(dataUrl){

                          var url = dataUrl.replace("data:image/1;","data:image/jpeg;");

                          //Metadata Collecting
                          var metadata = $rootScope.timestamp_to_crypt+' '+$rootScope.timestamp_source+"_"+latLng[0]+"_"+latLng[1]+"_"+window.device.uuid;
                          var exifIfd = {};
                          exifIfd[piexif.ExifIFD.UserComment] = metadata;
                          var exifObj = {"Exif":exifIfd};
                          var exifBytes = piexif.dump(exifObj);
                          var exifModifiedUrl = piexif.insert(exifBytes, url);

                          photoDataObject.uri_photo = exifModifiedUrl;
                          photoDataObject.photoblob = httpService.dataURItoBlob(exifModifiedUrl);

                          photoDataObject.nmea_foto = $scope.nmea_foto;
                          if(photoDataObject.nmea_foto === "" || photoDataObject.nmea_foto === undefined || isNaN(photoDataObject.nmea_foto)){
                            $rootScope.curr_sats_info = [];
                            photoDataObject.sats_info = $rootScope.curr_sats_info;
                          }

                          MappaService.insertPhotoUri( photoDataObject ).then(function(){
                            $scope.goBack();
                          });
                      },function failure(err){
                        console.log(err);
                      },{
                        "name": null,
                        "saveToPhotoGallery": false,
                        "encoding": ezar.ImageEncoding.JPEG ,
                        "quality": 100,
                        "scale": 100,
                        "includeCameraView": true,
                        "includeWebView": true});
                    },500);
                  }
                  },
                  function(err){console.log("ERROR NETWORKINFO!! "+err)});
            };

        	$ionicPlatform.onHardwareBackButton(function(event) {
       		 	event.preventDefault();
       		 	event.stopPropagation();
       		 	if($scope.initialCountdown <= 0){
       		 		$scope.goBack();
       		 	}
       	  	});

        	var deregisterHardBack = $ionicPlatform.registerBackButtonAction(function (event) {
        		event.preventDefault();
        		event.stopPropagation();
        		if($scope.initialCountdown <= 0){
       		 		$scope.goBack();
       		 	}
        		return false;
        	}, 900);

            $scope.goBack = function(){

              $scope.nmeaListener();
              $scope.nmea_foto = "";
              $scope.nmea_chars = 0;
                $ionicPlatform.offHardwareBackButton(function(event) {
                    console.log("rimosso blocco tasto indietro");
                });

              $rootScope.survey_active = false;

            	try{
            	  navigator.compass.clearWatch($scope.headingWatcher);
                window.removeEventListener('deviceorientation', $scope.getTiltAngle);
              }catch(err){
            	  console.log(err);
              }

            	ezar.getBackCamera().stop(function success(data){
            		$scope.toggleViewTransparent(false);
            		$ionicNavBarDelegate.showBar(true);
            		document.getElementById( 'container' ).innerHTML = "";
            		$timeout(function(){
                        window.screen.orientation.lock("portrait");
            			$state.go("pictureMapOl");
            		},500);
            	},function failure(err){
            		alert("critical failure, cannot stop back camera")
            	});
            };

            $scope.getCurrentDate = function(){
                var today = new Date();
                var dd = today.getDate();
                var mm = today.getMonth()+1; //January is 0!
                var yyyy = today.getFullYear();
                var h = today.getHours();
                var m = today.getMinutes();
                var s = today.getSeconds();
                if(dd<10) { dd='0'+dd; }
                if(mm<10) { mm='0'+mm; }
                if(h<10) { h='0'+h; }
                if(m<10) { m='0'+m; }
                if(s<10) { s='0'+s; }
                today = dd+'/'+mm+'/'+yyyy+'-'+h + ":" + m + ":" + s;
                return today;
            };


            $scope.b64toBlob = function(b64Data, contentType, sliceSize) {
            	  contentType = contentType || '';
            	  sliceSize = sliceSize || 512;
            	  var byteCharacters = atob(b64Data);
            	  var byteArrays = [];
            	  for (var offset = 0; offset < byteCharacters.length; offset += sliceSize) {
            	    var slice = byteCharacters.slice(offset, offset + sliceSize);
            	    var byteNumbers = new Array(slice.length);
            	    for (var i = 0; i < slice.length; i++) {
            	      byteNumbers[i] = slice.charCodeAt(i);
            	    }
            	    var byteArray = new Uint8Array(byteNumbers);
            	    byteArrays.push(byteArray);
            	  }
            	  var blob = new Blob(byteArrays, {type: contentType});
            	  return blob;
            };

            $scope.b64toBArray = function(b64Data){
            	var byteCharacters = atob(b64Data);

            	var byteNumbers = new Array(byteCharacters.length);
            	for (var i = 0; i < byteCharacters.length; i++) {
            	    byteNumbers[i] = byteCharacters.charCodeAt(i);
            	}

            	var byteArray = new Uint8Array(byteNumbers);

            	return byteArray;
            };

            $scope.points_to_go = "";
            $scope.surveyCountdownListener = $rootScope.$on('surveyCountdown', function(events, args){
              $scope.points_to_go = args[0];
            });

            $scope.closeCalibrationPopup = function(){
              $rootScope.correctionModePicture = 0;
              $scope.hideButtons = false;
              $scope.hideInitialCountdown = true;
              $scope.createScene();
              $interval.cancel(startTimer);
            };

             $scope.mean_centroid = {x:$stateParams.startPointLat, y:$stateParams.startPointLng};

             $scope.s_lat = "";
             $scope.s_lon = "";
             $scope.centroidListener = $rootScope.$on('photoCentroidComputed', function(events, args){
               var convertedPos;
               var authId = sessionService.getAuthId();
               $scope.s_lat = "";
               $scope.s_lon = "";
               if($scope.default_tile.id !== 'OSM' && $scope.default_tile.id !== 'GOOGLE_MAPS'){
                 convertedPos = {"x":args[1], "y":args[0]};
               }else{
                 convertedPos = {"x":args[1], "y":args[0]};
               }
               if(!isNaN(convertedPos.x) && !isNaN(convertedPos.y) && convertedPos.x !== undefined && convertedPos.y !== undefined){
                 $scope.mean_centroid = convertedPos;
               }
               $scope.mean_centroid_arrived = true;
             });

            $rootScope.$on('precisionAcquired', function(events, args){
              var precision = args[0];
              $scope.precision = precision.toFixed(2);
            });

            $rootScope.$on('nmeaGGAReceived', function(events, args){
              var data = args[0];
              $scope.nmea_height = data.altitude;
            });

       	    $scope.$on('$destroy', function () {
       	    	deregisterHardBack();
       	    });

          $scope.nmea_foto = "";
          $scope.nmeaListener = $rootScope.$on('nmeaRawMessage', function(events,args){
            if(args[0] !== undefined){
              if($scope.nmea_chars+args[0].length <= $scope.max_nmea_chars){
                $scope.nmea_foto += args[0];
                $scope.nmea_chars += args[0].length;
              }
            }
          })
        };


takePictureARCtrl.$inject = ['$scope', '$state', '$ionicNavBarDelegate',
	'$ionicPlatform', '$rootScope', '$ionicHistory', '$stateParams','$timeout','$q', 'MappaService',
	'sessionService', 'gpsService',
	'coordinatesService','JSTS_service','$interval','usbService','sqliteService','appService', 'baseLayersTypes', 'httpService', '$filter'];
angular.module('mappa.module').controller('takePictureARCtrl', takePictureARCtrl);
