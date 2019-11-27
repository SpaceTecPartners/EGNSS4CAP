var opzioniGpsAvanzateCtrl = function ($scope, $stateParams, $state, sqliteService, $q,
                            $ionicPopup, $rootScope, $ionicPlatform, appService, httpService, baseLayersTypes, $translate) {

  $scope.settings = {'metadata': [],
    'layers': [],
    'selected_id_layer': ''};

  $scope.sto_scaricando = false;
  $scope.sto_scaricando_index = -1;

  $rootScope.filterActive = false;

  $scope.$on('$ionicView.enter', function() {
    $scope.reloadMetadata();
  });

  $scope.reset_gps_default = function(){
    var deferred = $q.defer();
    $scope.numCampionamentiChanged(20);
    $scope.minHdopChanged(1.0);
    $scope.avgSamplingNumberChanged(20);
    $scope.minFixChanged(3);
    $scope.minNSatChanged(10);
    $scope.minSnrChanged(35);
    deferred.resolve("");
    return deferred.promise;
  };

  $scope.toggleFilterBtn = function(){
    var deferred = $q.defer();
    $rootScope.filterActive = !$rootScope.filterActive;
    deferred.resolve("");
    return deferred.promise;
  };

  $scope.reloadMetadata = function(){
    sqliteService.getMetadata(
      function(resmetadata){

        appService.safeApply($rootScope, function(){
          $scope.settings['metadata'] = resmetadata;
          $scope.settings['metadata']['use_gps'] = $scope.settings['metadata']['use_gps'] === 1;
          $scope.settings['metadata']['use_glonass'] = $scope.settings['metadata']['use_glonass'] === 1;
          $scope.settings['metadata']['use_galileo'] = $scope.settings['metadata']['use_galileo'] === 1;
        });

        appService.safeApply($rootScope, function(){
          $scope.settings['layers'] = baseLayersTypes;
        });
      },
      function(err){
        console.log(err);
      }
    );

  };

  $scope.numCampionamentiChanged = function(value){
    $scope.settings['metadata']['n_campionamenti'] = value;
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

  $scope.avgSamplingNumberChanged = function(value){
    $scope.settings['metadata']['avg_sampling_number'] = value;
    if (!$scope.sto_scaricando){
      sqliteService.updateMetadata( $scope.settings['metadata']['avg_sampling_number'], 'avg_sampling_number',
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
    $scope.settings['metadata']['min_fix'] = value;
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

  $rootScope.$on('settingsChanged', function(events, args){
    $scope.reloadMetadata();
  });

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
};

opzioniGpsAvanzateCtrl.$inject = ['$scope', '$stateParams', '$state', 'sqliteService', '$q',
  '$ionicPopup', '$rootScope', '$ionicPlatform', 'appService', 'httpService',
  'baseLayersTypes', '$translate'];
angular.module('opzioni_gps_avanzate.module').controller('opzioniGpsAvanzateCtrl', opzioniGpsAvanzateCtrl);
