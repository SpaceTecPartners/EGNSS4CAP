var opzioniCtrl = function ($scope, $stateParams, $state, sqliteService, $q,
                   $ionicPopup, $rootScope, $ionicPlatform, appService, httpService, baseLayersTypes, $translate) {

   $scope.settings = {'metadata': [],
                     'layers': [],
                     'selected_id_layer': ''};

  $scope.sto_scaricando = false;
  $scope.sto_scaricando_index = -1;

  $scope.$on('$ionicView.enter', function() {
    $scope.createKnob();
    $scope.reloadMetadata();
  });

   $scope.updateFilterFields = function(){
     document.getElementById("min_snr").innerHTML = "min snr: "+$scope.settings['metadata']['min_snr'];
     document.getElementById("min_n_sat").innerHTML = "min n sat: "+$scope.settings['metadata']['min_n_sat'];
     document.getElementById("min_hdop").innerHTML = "min hdop: "+$scope.settings['metadata']['min_hdop'];
   };

   $scope.createKnob = function(){
     try{
       var knob = document.getElementById("filter_knob");
       $scope.updateFilterFields();
       knob.value = 3;
       $(".my_dial").knob({
         'min':0,
         'max':10,
         'cursor':10,
         'thickness':.3,
         'displayinput' : true,
         'inputcolor' : '#222222',
         'fgColor' : '#222222',
         'angleOffset' : -125,
         'angleArc' : 250,
         'release' : function (v) { $scope.setFilterValues(v) }
       });
     }catch(err){
       console.log("ERROR: "+err);
     }
   };

   $scope.min_filter_values = {
     'snr':20,
     'n_sat':10,
     'hdop':2.0
   };
   $scope.filter_increase_step = {
     'snr':1.5,
     'n_sat':2,
     'hdop':0.2
   };

   $scope.setFilterValues = function(val){
     var new_snr = $scope.min_filter_values.snr + (val*$scope.filter_increase_step.snr);
     var new_n_sat = $scope.min_filter_values.n_sat + (val*$scope.filter_increase_step.n_sat);
     var new_hdop = ($scope.min_filter_values.hdop - (val*$scope.filter_increase_step.hdop)).toFixed(2);
     if(new_hdop < 0.1){
       new_hdop = 0.1;
     }
     $scope.minSnrChanged(new_snr);
     $scope.minNSatChanged(new_n_sat);
     $scope.minHdopChanged(new_hdop);
   };

  $scope.goto_gps_advanced = function(){
    var deferred = $q.defer();
    $state.go('opzioni_gps_avanzate');
    deferred.resolve("");
    return deferred.promise;
  };

  $scope.reloadMetadata = function(){
      sqliteService.getMetadata(
          function(resmetadata){

              appService.safeApply($rootScope, function(){
                  $scope.settings['metadata'] = resmetadata;
              });

              appService.safeApply($rootScope, function(){
                $scope.settings['layers'] = baseLayersTypes;
              });

              $scope.updateFilterFields();
          },
          function(err){
              console.log(err);
          }
      );

  };

   $scope.selectedLayerChanged = function(id_Layer){
     var deferred = $q.defer();
     if (!$scope.sto_scaricando){
         sqliteService.updateMetadata( id_Layer, 'default_tile_id',
             function(res){
               console.log("success update", res);
               $rootScope.$broadcast('settingsChanged');
               deferred.resolve("");
             },
             function(err){
               console.log(err);
               deferred.resolve("");
             });
     } else {
       $scope.askStopDownload();
       deferred.resolve("");
     }
     return deferred.promise;
   };

   $scope.selectedGPSChanged = function(idGPS){
     var deferred = $q.defer();
     sqliteService.updateMetadata( idGPS, 'GPSprovider',
         function(res){
           console.log("success update", res);
           $rootScope.$broadcast('settingsChanged');
           deferred.resolve("");
         },
         function(err){
           console.log(err);
           deferred.resolve("");
         });
     return deferred.promise;
   };

   $scope.zoomMinimoChanged = function(value){
     console.log($scope.settings['metadata']);
       if (!$scope.sto_scaricando){
           if ($scope.settings['metadata']['minzoom'] > $scope.settings['metadata']['maxzoom']) {
               $scope.settings['metadata']['minzoom'] = $scope.settings['metadata']['maxzoom'];
           }
           sqliteService.updateMetadata( $scope.settings['metadata']['minzoom'], 'minzoom',
               function(res){
                   $rootScope.$broadcast('settingsChanged');
               },
               function(err){
                   console.log(err);
               });
       } else {
           $scope.askStopDownload();
       }
   };

   $scope.zoomMassimoChanged = function(value){
       if (!$scope.sto_scaricando){
           if ($scope.settings['metadata']['maxzoom'] < $scope.settings['metadata']['minzoom']) {
               $scope.settings['metadata']['minzoom'] = $scope.settings['metadata']['maxzoom'];
           }
           console.log($scope.settings['metadata']['maxzoom']);
           sqliteService.updateMetadata( $scope.settings['metadata']['maxzoom'], 'maxzoom',
               function(res){
                   $rootScope.$broadcast('settingsChanged');
               },
               function(err){
                   console.log(err);
               });
       } else {
           $scope.askStopDownload();
       }
   };

  $scope.numCampionamentiChanged = function(value){
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['n_campionamenti'], 'n_campionamenti',
        function(res){
          $rootScope.$broadcast('settingsChanged');
        },
        function(err){
          console.log(err);
        });
    } else {
      $scope.askStopDownload();
    }
  };

  $scope.minHdopChanged = function(value){
    $scope.settings['metadata']['min_hdop'] = value;
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['min_hdop'], 'min_hdop',
        function(res){
          $rootScope.$broadcast('settingsChanged');
        },
        function(err){
          console.log(err);
        });
    } else {
      $scope.askStopDownload();
    }
  };

  $scope.minFixChanged = function(value){
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['min_fix'], 'min_fix',
        function(res){
          $rootScope.$broadcast('settingsChanged');
        },
        function(err){
          console.log(err);
        });
    } else {
      $scope.askStopDownload();
    }
  };

  $scope.minNSatChanged = function(value){
    $scope.settings['metadata']['min_n_sat'] = value;
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['min_n_sat'], 'min_n_sat',
        function(res){
          $rootScope.$broadcast('settingsChanged');
        },
        function(err){
          console.log(err);
        });
    } else {
      $scope.askStopDownload();
    }
  };

  $scope.minSnrChanged = function(value){
    $scope.settings['metadata']['min_snr'] = value;
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['min_snr'], 'min_snr',
        function(res){
          $rootScope.$broadcast('settingsChanged');
        },
        function(err){
          console.log(err);
        });
    } else {
      $scope.askStopDownload();
    }
  };

  $scope.centroidComputationTimeChanged = function(value){
    $scope.settings['metadata']['centroid_computation_time'] = value;
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['centroid_computation_time'], 'centroid_computation_time',
        function(res){
          $rootScope.$broadcast('settingsChanged');
        },
        function(err){
          console.log(err);
        });
    } else {
      $scope.askStopDownload();
    }
 };

 $scope.cancellaDatabase = function(){
   var deferred = $q.defer();
   sqliteService.populateDatabase(
       function success(){
     localStorage.clear();
     var myPopup = $ionicPopup.show({
         template: 'Database successfully deleted',
         title: 'Success',
         scope: $scope,
         buttons: [
           {
             text: '<b>OK</b>',
             type: 'button-positive',
           }
         ]
       });
       myPopup.then(function(res) {
         console.log('Tapped!', res);
       });
     deferred.resolve("");
   }, function error(){
     deferred.resolve($ionicPopup.alert({
             title: 'Error',
             template: 'Error in deleting database'
         }));
   });
   return deferred.promise;
 };

  $ionicPlatform.ready(function(){
      $rootScope.$on('downloadTiles', function(events, args){
          $scope.sto_scaricando = args[0];
          $scope.sto_scaricando_index = args[1];
      });
  });

  $scope.askStopDownload = function(){
    $scope.reloadMetadata();
    var confirmPopup = $ionicPopup.confirm({
          title: 'Warning',
          template: 'Settings are not editable, you have to stop tile ' +
          'download from bookmarks section first. Stop tile download?'
    });

    confirmPopup.then(function (res) {
        if (res){
            $rootScope.$broadcast('stopDownloadTiles', $scope.sto_scaricando_index);
        }
    });
  };

  $rootScope.$on('settingsChanged', function(events, args){
    $scope.reloadMetadata();
  });
};

opzioniCtrl.$inject = ['$scope', '$stateParams', '$state', 'sqliteService', '$q', '$ionicPopup', '$rootScope', '$ionicPlatform', 'appService', 'httpService', 'baseLayersTypes', '$translate'];
angular.module('opzioni.module').controller('opzioniCtrl', opzioniCtrl);
