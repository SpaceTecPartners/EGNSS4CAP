var gpsService = function($rootScope, coordinatesService, sqliteService) {
        var self = this;
        self.watchID = null;
        self.foregroundOptions = {
            maximumAge: 3000,
            timeout: 5000,
            enableHighAccuracy: true
        };


        self.startForegroundTracking = function(authid, needsConversion){
            self.watchID = navigator.geolocation.watchPosition(
                function(position){

                    var coords = position.coords;
                    var accuracy = position.coords.accuracy;
                    var altitude = position.coords.altitude;
                    // console.log("ACCURACY: "+accuracy);

                    // console.log("coordsprima", coords);
                    if(needsConversion){
                    	coords = coordinatesService.from3857to30033004(coords, authid);
                    }else{
                    	// coords = {"x":coords.longitude, "y":coords.latitude, "z":coords.altitude};
                      coords = coordinatesService.fromWGS84to3857(coords);
                    }
                    // console.log("coordsdopo", coords);

                    var d = new Date(position.timestamp);
                    var format_timestamp = d.getUTCDate()+'-'+(d.getUTCMonth()+1)+'-'+d.getUTCFullYear()+' '+d.getUTCHours()+':'+d.getUTCMinutes()+':'+d.getSeconds();
                    $rootScope.$broadcast('locationAcquired', ['foreground', coords, format_timestamp, accuracy, altitude]);
                    $rootScope.loc_manager_accuracy = accuracy;
                },
                function(err){
                    console.log("error on start foreground tracking");
                    console.log(err);
                },
                self.foregroundOptions
            );
        };

        self.stopForegroundTracking = function(){

            if(self.watchID){
                console.log("stop foreground");
                navigator.geolocation.clearWatch(self.watchID);
            }

        };

        self.startBackgroundTracking = function(authid, needsConversion){

            navigator.geolocation.getCurrentPosition( //SPEED UP FOR FIRST POSITION'S ACQUISITION
                function(position){
                  // console.log("STAMPO POSITION BACK");
                  // console.log(position);
                    var coords = position.coords;
                    var accuracy = position.coords.accuracy;
                  var altitude = position.coords.altitude;

                  // console.log("ACCURACY: "+accuracy);
                    if(needsConversion){
                    	coords = coordinatesService.from3857to30033004(coords, authid);
                    }else{
                      // coords = {"x":coords.longitude, "y":coords.latitude, "z":coords.altitude};
                      coords = coordinatesService.fromWGS84to3857(coords);
                    }
                  var d = new Date(position.timestamp);
                  var format_timestamp = d.getUTCDate()+'-'+(d.getUTCMonth()+1)+'-'+d.getUTCFullYear()+' '+d.getUTCHours()+':'+d.getUTCMinutes()+':'+d.getSeconds();

                  $rootScope.loc_manager_accuracy = accuracy;
                  $rootScope.$broadcast('locationAcquired', ['foreground', coords, format_timestamp, accuracy, altitude]);
                },
                function(err){
                    console.log("error");
                },
                self.foregroundOptions);

            sqliteService.getMetadata(
                function(resmetadata) {
                    self.backgroundOptions = {
                        desiredAccuracy: 0,
                        stationaryRadius: 0,
                        distanceFilter: resmetadata.distanceFilter,
                        locationProvider: backgroundGeolocation.provider.ANDROID_ACTIVITY_PROVIDER,
                        interval: 1000,
                        fastestInterval: 1000,
                        activitiesInterval: 1000,
                        stopOnStillActivity: false,
                        debug: false
                    };
                    backgroundGeolocation.configure(
                        function(location_orig){

                          var ts = location_orig.time;
                          var accuracy = location_orig.accuracy;
                          var altitude = 0;
                          if(location_orig.altitude !== undefined && !isNaN(location_orig.altitude)){
                            altitude = location_orig.altitude;
                          }

                        	if(needsConversion){
                        		var location = coordinatesService.from3857to30033004(location_orig, authid);
                        	}else{
                            // var location = {"x":location_orig.longitude, "y":location_orig.latitude, "z":location_orig.altitude};
                            var location = coordinatesService.fromWGS84to3857(location_orig);
                        	}

                          // console.log("LOCATION: ");
                          // console.log(location_orig);

                          var d = new Date(ts);
                          var format_timestamp = d.getUTCDate()+'-'+(d.getUTCMonth()+1)+'-'+d.getUTCFullYear()+' '+d.getUTCHours()+':'+d.getUTCMinutes()+':'+d.getSeconds();

                          $rootScope.loc_manager_accuracy = accuracy;
                          $rootScope.$broadcast('locationAcquired', ['background', location, format_timestamp, accuracy, altitude]);


                        },
                        function(err){

                            console.log("background acquisition error", err);

                        },
                        self.backgroundOptions
                    );

                    backgroundGeolocation.start(
                        function () {
                            console.log("background started success");

                            backgroundGeolocation.switchMode(backgroundGeolocation.mode.FOREGROUND);

                        },
                        function (error) {
                            console.log("background start error:", error);

                        }
                    );

                },
                function(error){
                	console.log("ERROR",error);
                }
            );
        };

        self.stopBackgroundTracking = function(){

            console.log("stop background");
            backgroundGeolocation.stop();

        };

        self.valutaNuovaLocation = function(newLoc, curBestLoc, prov, curBestLocProv){
          var TWO_MINUTES = 1000 * 60 * 2;
          if(curBestLoc == null){
            console.log("PRIMA RILEVAZIONE, RITORNA");
            return true;
          }
          else{
            var isLessAccurate = false;
            var isMoreAccurate = false;
            var isSignificantlyLessAccurate = false;
            var isFromSameProvider = false;
            //FAI CONTROLLO SU ACCURACY E SETTA BOOLEANO
            if(newLoc.accuracy != null && curBestLoc.accuracy != null){

              // Check whether the new location fix is newer or older
              var timeDelta = newLoc.time - curBestLoc.time;
              var isSignificantlyNewer = timeDelta > TWO_MINUTES;
              var isSignificantlyOlder = timeDelta < -TWO_MINUTES;
              var isNewer = timeDelta > 0;

              console.log("timeNew: "+newLoc.time);
              console.log("timeOld: "+curBestLoc.time);
              console.log("timeDelta: "+timeDelta);

              // If it's been more than two minutes since the current location, use the new location
              // because the user has likely moved
              if (isSignificantlyNewer) {
                return true;
                // If the new location is more than two minutes older, it must be worse
              } else if (isSignificantlyOlder) {
                return false;
              }

              var accuracyDelta = newLoc.accuracy - curBestLoc.accuracy;
              if(accuracyDelta > 0){
                isLessAccurate = true;
              }
              if(accuracyDelta < 0){
                isMoreAccurate = true;
              }
              if(accuracyDelta > 200){
                isSignificantlyLessAccurate = true;
              }
              if(prov == curBestLocProv){
                isFromSameProvider = true;
              }

              console.log("accuracyDelta: "+accuracyDelta);

              //CONTROLLA BOOLEANI E DECIDI SE AGGIORNARE POSIZIONE O USARE QUELLA CORRENTE
              // Determine location quality using a combination of timeliness and accuracy
              if (isMoreAccurate) {
                return true;
              }
              else if (isNewer && !isLessAccurate) {
                return true;
              }
              else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
                return true;
              }
              return false;
            }
            else{
              return true;
            }
          }
          //FAI CONTROLLO SU AGE E SETTA BOOLEANO
        }

    };

gpsService.$inject = ['$rootScope', 'coordinatesService', 'sqliteService'];
angular.module('mappa.module').service('gpsService', gpsService);
