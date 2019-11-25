var aboutCtrl = function ($scope, $ionicLoading, $stateParams, $state, $q, sessionService, appService, $filter, sqliteService, $rootScope, gpsDiagnosticService) {
  $scope.$on('$ionicView.enter', function() {
    $scope.title = "";
    $scope.body_text = "";
  });

  $scope.CONST_GALILEO = 6;
  $scope.CONST_SBAS = 2;
  $scope.GREEN_BALL_PATH = "img/green_ball.png";
  $scope.version_number = "";

  $scope.$on('$ionicView.beforeLeave', function() {
    window.gnss_status.stopGnssStatus(
      function success(data){
        console.log("SUCCESS STOP");
      },
      function error(err){
        console.log("ERROR STOPPING: "+err);
      }
    );
  });

  $scope.$on('$ionicView.afterEnter', function() {
    console.log("-----------> $ionicView.afterEnter");

    $scope.reloadMetadata();

    gpsDiagnosticService.checkAvailability(
      function(){
        window.gnss_status.registerReadCallback(
          function success(data) {
            // console.log("SUCCESS");
            // console.log(data);
            $scope.readInternalGnssInfo(data);
          },
          // error attaching the callback
          function error(err) {
            console.log("ERROR ATTACHING CALLBACK - "+err);
            // $rootScope.$broadcast('deviceError', []);
          }
        );
      },
      function(){console.log("GPS UNAVAILABLE!")});

      cordova.getAppVersion.getVersionNumber(function(versionNumber){
          // 1.0.0
          console.log("version number", versionNumber);
          $scope.version_number = versionNumber;
      });


    console.log(device.manufacturer);
    console.log(device.model);

    document.getElementById("ab_space_manufact").innerHTML = device.manufacturer;
    document.getElementById("ab_space_model").innerHTML = device.model;

  });

  $scope.checkKnownInfo = function(){
    if($scope.settings['support']['support_galileo'] === "true"){
      document.getElementById("ab_ball_galileo").src = $scope.GREEN_BALL_PATH;
    }

    if($scope.settings['support']['support_dual_freq'] === "true"){
      document.getElementById("ab_ball_dual_freq").src = $scope.GREEN_BALL_PATH;
    }

    if($scope.settings['support']['support_osnma'] === "true"){
      document.getElementById("ab_ball_osnma").src = $scope.GREEN_BALL_PATH;
    }

    if($scope.settings['support']['support_egnos'] === "true"){
      document.getElementById("ab_ball_egnos").src = $scope.GREEN_BALL_PATH;
    }
  };


  $scope.checkGalileo = function(net){
    return net === $scope.CONST_GALILEO;
  };

  $scope.checkEgnos = function(net){
    return net === $scope.CONST_SBAS;
  };

  $scope.str = "";
  $scope.readInternalGnssInfo = function(data, coordinatesNeedConversion){
    var view = new Uint8Array(data);
    if(view.length >= 1) {
      for (var i = 0; i < view.length; i++) {
        if (view[i] === 13) {
          var obj = JSON.parse($scope.str);
          try{
            if(obj['type'] === 'gnss_status'){
              var netws = JSON.parse(obj['constellations']);
              if(netws.find($scope.checkGalileo) !== undefined){
                if($scope.settings['support']['support_galileo'] !== "true"){
                  document.getElementById("ball_galileo").src = $scope.GREEN_BALL_PATH;
                  $scope.setSupportValue("support_galileo");
                }
              }
              if(netws.find($scope.checkEgnos) !== undefined){
                if($scope.settings['support']['support_egnos'] !== "true") {
                  document.getElementById("ball_egnos").src = $scope.GREEN_BALL_PATH;
                  $scope.setSupportValue("support_egnos");
                }
              }
            }
            if(obj['type'] === 'dual_freq'){
              var dual_freq = JSON.parse(obj['value']);
              if(dual_freq){
                if($scope.settings['support']['support_dual_freq'] !== "true") {
                  document.getElementById("ball_dual_freq").src = $scope.GREEN_BALL_PATH;
                  $scope.setSupportValue("support_dual_freq");
                }
              }
            }
            if(obj['type'] === 'osnma'){
              var osnma = JSON.parse(obj['value']);
              if(osnma){
                if($scope.settings['support']['support_osnma'] !== "true") {
                  document.getElementById("ball_osnma").src = $scope.GREEN_BALL_PATH;
                  $scope.setSupportValue("support_osnma");
                }
              }
            }
          }catch(e){
            console.log("ERR: "+e);
          }
          $scope.str = "";
        }
        else {
          var temp_str = String.fromCharCode(view[i]);
          var str_esc = escape(temp_str);
          $scope.str += unescape(str_esc);
        }
      }
    }else{
    }
  };

  $scope.setSupportValue = function(key){
    var deferred = $q.defer();
    sqliteService.updateMetadata( true, key,
      function(res){
        deferred.resolve("");
      },
      function(err){
        console.log(err);
        deferred.resolve("");
      });
    console.log("RITORNO DEFERRED PROMISE");
    return deferred.promise;
  };

  $scope.settings = {};
  $scope.reloadMetadata = function(){
    sqliteService.getMetadata(
      function(resmetadata){
        appService.safeApply($rootScope, function(){
          $scope.settings['support'] = resmetadata;
          $scope.checkKnownInfo();
        });
      },
      function(err){
        console.log(err);
      }
    );
  };
};

aboutCtrl.$inject = ['$scope', '$ionicLoading', '$stateParams', '$state', '$q', 'sessionService', 'appService', '$filter', 'sqliteService', '$rootScope', 'gpsDiagnosticService'];
angular.module('about.module').controller('aboutCtrl', aboutCtrl);
