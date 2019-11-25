var gpsDiagnosticService = function () {

        var self = this;

        //-------------------------------- GPS CHECK FUNCTIONS ---------------------------------------------------------

        self.checkAvailability = function(success_f, error_f){
        	if(ionic.Platform.isIOS()){
        		cordova.plugins.diagnostic.isLocationEnabled(function(available){
        			if(!available){
                        self.checkAuthorization(success_f, error_f);
                    } else {
                        console.log("GPS location is ready to use");
                        success_f();
                    }
        		});
        	}else{
        		cordova.plugins.diagnostic.isGpsLocationAvailable(function(available){
                    console.log("GPS location is " + (available ? "available" : "not available"));
                    if(!available){
                        self.checkAuthorization(success_f, error_f);
                    }else{
                        console.log("GPS location is ready to use");
                        success_f();
                    }
                }, function(errormsg){
                    console.error("The following error occurred: "+errormsg);
                    error_f();
                });
        	}
        };

        self.checkAuthorization = function(success_f, error_f){
            cordova.plugins.diagnostic.isLocationAuthorized(function(authorized){
                console.log("Location is " + (authorized ? "authorized" : "unauthorized"));
                if(authorized){
                    self.checkDeviceSetting(success_f, error_f);
                }else{
                    cordova.plugins.diagnostic.requestLocationAuthorization(function(status){
                        switch(status){
                            case cordova.plugins.diagnostic.permissionStatus.GRANTED:
                                console.log("Permission granted");
                                self.checkDeviceSetting(success_f, error_f);
                                break;
                            case cordova.plugins.diagnostic.permissionStatus.DENIED:
                                console.log("Permission denied");
                                error_f();
                                navigator.app.exitApp();
                                // User denied permission
                                break;
                            case cordova.plugins.diagnostic.permissionStatus.DENIED_ALWAYS:
                                console.log("Permission permanently denied");
                                error_f();
                                // User denied permission permanently
                                break;
                        }
                    }, function(errormsg){
                        console.error(errormsg);
                        error_f();
                    });
                }
            }, function(errormsg){
                console.error("The following error occurred: "+errormsg);
                error_f();
            });
        };

        self.checkDeviceSetting =function(success_f, error_f){
            cordova.plugins.diagnostic.isGpsLocationEnabled(function(enabled){
                console.log("GPS location setting is " + (enabled ? "enabled" : "disabled"));
                if(!enabled){
                    cordova.plugins.locationAccuracy.request(function (successmsg){
                        console.log("Successfully requested high accuracy location mode: "+successmsg.message);
                        success_f();
                    }, function onRequestFailure(error){
                        console.error("Accuracy request failed: error code="+error.code+"; error message="+error.message);
                        error_f();
                        if(error.code !== cordova.plugins.locationAccuracy.ERROR_USER_DISAGREED){
                            if(confirm("Failed to automatically set Location Mode to 'High Accuracy'. Would you like to switch to the Location Settings page and do this manually?")){
                                cordova.plugins.diagnostic.switchToLocationSettings();
                            }

                        }else{
                          navigator.app.exitApp();
                        }
                    }, cordova.plugins.locationAccuracy.REQUEST_PRIORITY_HIGH_ACCURACY);
                }
                else {
                    success_f();
                }
            }, function(errormsg){
                console.error("The following error occurred: "+errormsg);
                error_f();
            });
        };

        self.checkGPS_locationMode = function(gpsDetected, lowGpsDetected, noGpsDetected){
            cordova.plugins.diagnostic.getLocationMode(function(state){

                switch(state){
                    case cordova.plugins.diagnostic.locationMode.LOCATION_OFF:
                        noGpsDetected();
                        break;

                    case cordova.plugins.diagnostic.locationMode.DEVICE_ONLY:
                    case cordova.plugins.diagnostic.locationMode.BATTERY_SAVING:
                        lowGpsDetected();
                        break;

                    case cordova.plugins.diagnostic.locationMode.HIGH_ACCURACY:
                        gpsDetected();
                        break;
                }
            });
        };

        self.checkGPS_stateChange = function(gpsDetected, lowGpsDetected, noGpsDetected){
            cordova.plugins.diagnostic.registerLocationStateChangeHandler(function(state){

                switch(state){
                    case cordova.plugins.diagnostic.locationMode.LOCATION_OFF:
                        noGpsDetected();
                        break;

                    case cordova.plugins.diagnostic.locationMode.DEVICE_ONLY:
                    case cordova.plugins.diagnostic.locationMode.BATTERY_SAVING:
                        lowGpsDetected();
                        break;

                    case cordova.plugins.diagnostic.locationMode.HIGH_ACCURACY:
                        gpsDetected();
                        break;
                }
            });
        };

    };

    gpsDiagnosticService.$inject = [];

angular.module('mappa.module').service('gpsDiagnosticService', gpsDiagnosticService);
