var sensoregpsCtrl = function ($scope, $rootScope, $ionicHistory, usbService, $ionicPopup, $ionicPlatform, $interval, $q, $ionicLoading, $stateParams, $state, sessionService, appService, $filter, sqliteService, gpsDiagnosticService, gpsService) {
          $scope.sat_tot = 0;

          $scope.altitude = "n/d";
          $scope.lat = "n/d";
          $scope.lon = "n/d";
          $scope.n_s = "n/d";
          $scope.w_e = "n/d";

          $scope.fix_mode = "n/d";
          $scope.hdop = "n/d";

          $scope.speed_kmh_rmc = "n/d";
          $scope.speed = "n/d";
          $scope.magn_var = "n/d";
          $scope.tr_deg_1 = "n/d";
          $scope.tr_deg_2 = "n/d";
          $scope.speed_knots = "n/d";
          $scope.speed_kmh = "n/d";
          $scope.pdop = "n/d";
          $scope.vdop = "n/d";
          $scope.track_mg = "n/d";
          $scope.e_w_magn = "n/d";

          $scope.n_sat = "n/d";

          $scope.connectionActive = false;
          $scope.firstConnection = true;

          $scope.galileoTot = 1;
          $scope.currGalileos = [];

          $scope.gpsSats = [];
          $scope.glonassSats = [];
          $scope.beidouSats = [];

          $scope.fix_type = "n/d";

          $scope.dummy_sat_list = '<div class="list"><label class="item item-input item-stacked-label" ><div>n/d</div></label><label class="item item-input item-stacked-label" ><div>n/d</div></label><label class="item item-input item-stacked-label" ><div>n/d</div></label></div>';

          $scope.interval_time = 5000;
          $scope.sg_scheduler = null;
          $scope.rawActive = false;
          $scope.rawBtnActive = false;

          $scope.nmeaRawText = "";
          $scope.pausedText = false;

          $scope.gps_ids_snr = [];
          $scope.glonass_ids_snr = [];
          $scope.galileo_ids_snr = [];
          $scope.beidou_ids_snr = [];

          $scope.right_list = {'gp':$scope.gps_ids_snr, 'ga':$scope.galileo_ids_snr, 'gl':$scope.glonass_ids_snr, 'bd':$scope.beidou_ids_snr};
          $scope.right_list_sat_only = {'gp':$scope.gpsSats, 'ga':$scope.currGalileos, 'gl':$scope.glonassSats, 'bd':$scope.beidouSats};

          $rootScope.filterActive = false;

          $rootScope.survey_active = false;

          $rootScope.externalData = false;

          $scope.accuracy = "n/d";
          $scope.precision = "n/d";

          $scope.toggleSamplingModeBtn = function(){
            var deferred = $q.defer();
            $rootScope.survey_active = !$rootScope.survey_active;
            deferred.resolve("");
            return deferred.promise;
          };

          $scope.toggleFilterBtn = function(){
            var deferred = $q.defer();
            $rootScope.filterActive = !$rootScope.filterActive;
            deferred.resolve("");
            return deferred.promise;
          };

          $scope.$on('$ionicView.afterEnter', function() {
            $scope.connectionActive = usbService.isGpsActive();
            $scope.rawActive = false;
            $scope.rawBtnActive = false;
            document.getElementById("sens_space_manufact").innerHTML = device.manufacturer;
            document.getElementById("sens_space_model").innerHTML = device.model;
            gpsDiagnosticService.checkAvailability(
              function(){
                gpsService.startBackgroundTracking(sessionService.getAuthId(),  false);
              },
              function(){console.log("GPS UNAVAILABLE!")});
            $rootScope.survey_active = true;
            $scope.getStartingPosition();
            $scope.start_scheduler_satlist();
            $scope.reloadMetadata();
          });

          $scope.getStartingPosition = function(){
            if(!usbService.isGpsActive()){
              usbService.startNmeaCommunication($scope.coordinatesNeedConversion);
              usbService.setGpsStatus(true);
            }
            gpsService.startForegroundTracking(false);
          };

          $scope.start_scheduler_satlist = function(){
            if($scope.sg_scheduler === null){
              $scope.sg_scheduler = $interval($scope.update_sats_lista_and_number, $scope.interval_time);
            }
          };

          $scope.stop_scheduler_satlist = function(){
            $interval.cancel($scope.sg_scheduler);
          };

          $scope.update_sats_lista_and_number = function(){
            var liste_sat = [[$scope.sats_with_snr['gp'], 'gps_sat_list', 'gp'],[$scope.sats_with_snr['gl'], 'glonass_sat_list', 'gl'],[$scope.sats_with_snr['ga'], 'galileo_sat_list', 'ga'],[$scope.sats_with_snr['bd'], 'beidou_sat_list', 'bd']];
            var tot_sat = 0;
            for(var i=0; i<liste_sat.length; i++){
              var lista = $scope.right_list[liste_sat[i][2]];
              var id = liste_sat[i][1];
              $scope.createSatListFromDic(liste_sat[i][0], id);
              tot_sat += Object.keys(liste_sat[i][0]).length;
            }
            document.getElementById('t_n_sat').innerHTML = tot_sat.toString();
            $scope.gpsSats = [];
            $scope.glonassSats = [];
            $scope.glonassSats = [];
            $scope.currGalileos = [];
            $scope.sats_with_snr = {'gp':{}, 'gl':{}, 'ga':{}, 'bd':{}};
          };

          $scope.toggleDevice = function(){
            var deferred = $q.defer();
            if($scope.firstConnection){
              $scope.firstConnection = false;
              $scope.connectionActive = true;
              usbService.startNmeaCommunication();
              $scope.start_scheduler_satlist();
            }
            else{
              if(!$scope.connectionActive){
                $scope.connectionActive = true;
                usbService.requestNmeaMessages();
                $scope.start_scheduler_satlist();
              }
              else{
                $scope.connectionActive = false;
                usbService.stopNmeaMessages();
                $scope.stop_scheduler_satlist();
              }
            }
            usbService.setGpsStatus($scope.connectionActive);
            deferred.resolve("");
            return deferred.promise;
          };

          $scope.createSatListFromDic = function(sats, list_id){
            var entry_1 = '<label class="item item-input item-stacked-label" ><div>';
            var entry_2 = '</div></label>';
            var start = '<div class="list">';
            var end = '</div>';
            var to_append = '';

            Object.keys(sats).forEach(function(key,index) {
              if(list_id === "galileo_sat_list" || list_id === "gps_sat_list"){
                // to_append += entry_1 + key +' - snr: ' + sats[key][0] + 'dB f: '+ sats[key][1] + entry_2;
                to_append += entry_1 + key +' - snr: ' + sats[key][0] + 'dB'+ entry_2;
              }else{
                to_append += entry_1 + key +' - snr: ' + sats[key][0] + 'dB' + entry_2;
              }
            });
            to_append = start + to_append + end;
            document.getElementById(list_id).innerHTML = to_append;
          };

          $scope.createNewSatList = function(sats, list_id, k){
            var entry_1 = '<label class="item item-input item-stacked-label" ><div>';
            var entry_2 = '</div></label>';
            var start = '<div class="list">';
            var end = '</div>';
            var to_append = '';
            for(var i=0; i<sats.length; i++){
              if(sats[i][1] !== null){
                to_append += entry_1 + sats[i][0] +' - snr:' + sats[i][1] + 'dB' + entry_2;
              }else{
                to_append += entry_1 + sats[i][0] +' - snr: n/d' + entry_2;
              }
            }
            to_append = start + to_append + end;
            $scope.right_list[k] = [];
            document.getElementById(list_id).innerHTML = to_append;
          };

          $ionicPlatform.ready(function () {

          $rootScope.$on('deviceError', function(events, args){
            $scope.connectionActive = false;
            $scope.firstConnection = true;
            usbService.setGpsStatus($scope.connectionActive);
            $scope.stop_scheduler_satlist();
            $ionicPopup.show({
                title: "ATTENTION",
                subTitle: "device communication error",
                scope: $scope,
                buttons: [
                  {
                    text: '<b>ok</b>',
                    type: 'button-positive',
                    onTap: function (e) {
                    }
                  }
                ]
              });
          });

            $rootScope.$on('meansnrMessage', function(events, args){
              document.getElementById("mean_snr").innerHTML = args[0];
            });

          $rootScope.$on('nmeaVTGReceived', function(events, args){
            var data = args[0];
            $scope.tr_deg_1 = data.tr_deg_1;
            $scope.tr_deg_2 = data.tr_deg_2;
            $scope.speed_knots = data.speed_knots;
            $scope.speed_kmh = data.speed_kmh;

            document.getElementById("t_tr_deg_1").innerHTML = $scope.tr_deg_1;
            document.getElementById("t_tr_deg_2").innerHTML = $scope.tr_deg_2;
            document.getElementById("t_speed_knots").innerHTML = $scope.speed_knots;
            document.getElementById("t_speed_kmh").innerHTML = $scope.speed_kmh;
          });

          $rootScope.$on('locationAcquired', function(events, args){
            if(args[0] === "foreground" || args[0] === "background"){
              var accuracy = args[3];
              $scope.accuracy = accuracy.toFixed(2) + " m";
              document.getElementById("t_accuracy").innerHTML = $scope.accuracy;
            }
          });

          $rootScope.$on('precisionAcquired', function(events, args){
              var precision = args[0];
              $scope.precision = precision.toFixed(2) + " m";
              document.getElementById("t_precision").innerHTML = $scope.precision;
          });

          $rootScope.$on('nmeaGGAReceived', function(events, args){
            var data = args[0];
            $scope.lat = data.latitude;
            $scope.lon = data.longitude;
            $scope.altitude = data.altitude;
            $scope.n_s = data.n_s;
            $scope.w_e = data.e_w;
            $scope.n_sat = data.n_sat;
            $scope.fix_type = data.fix_type;

            console.log("STAMPO INFO NORMALITA'");
            console.log(data.normality_lat);
            console.log(data.normality_lon);

            if(data.normality_lat.mean !== ''){
              document.getElementById("norm_mean_lat").innerHTML = data.normality_lat.mean.toFixed(4);
              document.getElementById("norm_lat").innerHTML = data.normality_lat.normal;
              document.getElementById("norm_p_lat").innerHTML = data.normality_lat.pValue.toFixed(4);
              document.getElementById("norm_stdP_lat").innerHTML = data.normality_lat.stdP.toFixed(4);
              document.getElementById("norm_stdS_lat").innerHTML = data.normality_lat.stdS.toFixed(4);
            }

            if(data.normality_lon.mean !== ''){
              document.getElementById("norm_mean_lon").innerHTML = data.normality_lon.mean.toFixed(4);
              document.getElementById("norm_lon").innerHTML = data.normality_lon.normal;
              document.getElementById("norm_p_lon").innerHTML = data.normality_lon.pValue.toFixed(4);
              document.getElementById("norm_stdP_lon").innerHTML = data.normality_lon.stdP.toFixed(4);
              document.getElementById("norm_stdS_lon").innerHTML = data.normality_lon.stdS.toFixed(4);
            }


            if($scope.fix_type === '1'){
              document.getElementById("t_fix_type").innerHTML = "no"
            }

            if($scope.fix_type === '2'){
              document.getElementById("t_fix_type").innerHTML = "yes"
            }

            if($scope.lat !== undefined && $scope.lon !== undefined && !isNaN($scope.lat) && !isNaN($scope.lon) && $scope.lat !== 'NaN' && $scope.lon !== 'NaN'){
              document.getElementById("t_lat").innerHTML = parseFloat($scope.lat).toFixed(8);
              document.getElementById("t_lon").innerHTML = parseFloat($scope.lon).toFixed(8);
              document.getElementById("t_alt").innerHTML = $scope.altitude;
            }else{
              document.getElementById("t_lat").innerHTML = 'n/d';
              document.getElementById("t_lon").innerHTML = 'n/d';
              document.getElementById("t_alt").innerHTML = 'n/d';
            }

            document.getElementById("n_sat_used").innerHTML = $scope.n_sat;
            document.getElementById("t_n_s").innerHTML = $scope.n_s;
            document.getElementById("t_w_e").innerHTML = $scope.w_e;
          });

          $rootScope.$on('nmeaRMCReceived', function(events, args){
            var data = args[0];
            $scope.lat = data.latitude;
            $scope.lon = data.longitude;
            $scope.n_s = data.n_s;
            $scope.w_e = data.e_w;
            $scope.speed = data.speed;
            $scope.magn_var = data.magn_var;
            $scope.speed_kmh_rmc = parseFloat($scope.speed)*1.825;
            $scope.speed_kmh_rmc = $scope.speed_kmh_rmc.toFixed(1);

            $scope.track_mg = data.track_mg;
            $scope.e_w_magn = data.e_w_magn;

            if($scope.lat != undefined && $scope.lon != undefined) {
              document.getElementById("t_lat").innerHTML = $scope.lat;
              document.getElementById("t_lon").innerHTML = $scope.lon;
            }
            document.getElementById("t_n_s").innerHTML = $scope.n_s;
            document.getElementById("t_w_e").innerHTML = $scope.w_e;
            document.getElementById("t_speed").innerHTML = $scope.speed;
            document.getElementById("t_speed_kmh_rmc").innerHTML = $scope.speed_kmh_rmc;
            document.getElementById("t_magn_var").innerHTML = $scope.magn_var;
            document.getElementById("track_mg").innerHTML = $scope.track_mg;
            document.getElementById("e_w_magn").innerHTML = $scope.e_w_magn;
          });

          $scope.sats_with_snr = {'gp':{}, 'gl':{}, 'ga':{}, 'bd':{}};
          $rootScope.$on('nmeaGSVReceived', function(events, args){
            var network = args[1];
            var data = [];

            for(var j=0; j<args[0].length; j++){
              data = args[0][j];
              if(network == 'ga' && $scope.currGalileos.indexOf(data.prn) === -1){
                if(data.prn !== '0*74'){
                  $scope.currGalileos.push(data.prn);
                  $scope.right_list[network].push([data.prn, data.snr]);
                }
              }

              if(data.prn !== '0*74' && data.prn !== '0*79' && data.prn !== '0*65'){
                $scope.sats_with_snr[network][data.prn] = [data.snr, data.band];
              }

              if($scope.right_list_sat_only[network].indexOf(data.prn) === -1){
                $scope.right_list[network].push([data.prn, data.snr]);
                $scope.right_list_sat_only[network].push(data.prn);
              }
              else{
                for(var i=0; i<$scope.right_list[network].length; i++){
                  if($scope.right_list[network][i][0] === data.prn){
                    $scope.right_list[network][i][1] = data.snr;
                  }
                }
              }
            }
          });

          $rootScope.$on('nmeaGSAReceived', function(events, args){
            var data = args[0];
            var network = args[1];

            if(network == 'gn'){
              $scope.fix_mode = data.fix_mode;
              $scope.hdop = data.hdop;
              $scope.pdop = data.pdop;
              $scope.vdop = data.vdop;
              document.getElementById("t_hdop").innerHTML = $scope.hdop;
              document.getElementById("t_vdop").innerHTML = $scope.vdop;
              document.getElementById("t_pdop").innerHTML = $scope.pdop;
            }
          });
        });

  $scope.CONST_GALILEO = 6;
  $scope.CONST_SBAS = 2;
  $scope.GREEN_BALL_PATH = "img/green_ball.png";

  $scope.$on('$ionicView.beforeLeave', function() {
    clearInterval($scope.scheduler);
    window.gnss_status.stopGnssStatus(
      function success(data){
      },
      function error(err){
        console.log("ERROR STOPPING: "+err);
      }
    );
  });

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
    return deferred.promise;
  };

  $scope.settings = {};
  $scope.reloadMetadata = function(){
    sqliteService.getMetadata(
      function(resmetadata){
        appService.safeApply($rootScope, function(){
          $scope.settings['support'] = resmetadata;
        });
      },
      function(err){
        console.log(err);
      }
    );
  };
};

sensoregpsCtrl.$inject = ['$scope', '$rootScope', '$ionicHistory', 'usbService', '$ionicPopup', '$ionicPlatform', '$interval', '$q', '$ionicLoading', '$stateParams', '$state', 'sessionService', 'appService', '$filter', 'sqliteService', 'gpsDiagnosticService', 'gpsService'];
angular.module('sensoregps.module').controller('sensoregpsCtrl', sensoregpsCtrl);
