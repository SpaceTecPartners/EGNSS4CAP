/**
 * Created by Marzio on 05/07/2017.
 */


var usbService = function($rootScope, $interval, coordinatesService, sessionService, sqliteService, appService, httpService) {

  var self = this;
  var str = "";
  var str_pack = "";
  var lastLat = -1;
  var lastLon = -1;
  var scheduler;

  var gpsActive = false;
  var campionamenti_lat = [];
  var campionamenti_lon = [];
  var ref_point = [];
  var num_sats_low = false;
  var snr_too_low = true;

  var getAltitudeFromService = false;

  self.setGetAltitudeFromServiceBoolean = function(val){
    getAltitudeFromService = val;
  };

  var constant_sat_info = [];

  self.settings = {'metadata': [],
    'layers': [],
    'selected_id_layer': ''};

  self.reloadMetadata = function(){
    sqliteService.getMetadata(
      function(resmetadata){
        appService.safeApply($rootScope, function(){
          self.settings['metadata'] = resmetadata;

          if(self.settings.metadata.min_n_sat === undefined || isNaN(self.settings.metadata.min_n_sat)){
            self.settings.metadata.min_n_sat = 7;
          }

          if(self.settings.metadata.min_hdop === undefined || isNaN(self.settings.metadata.min_hdop)){
            self.settings.metadata.min_hdop = 1.0;
          }

          if(self.settings.metadata.min_snr === undefined || isNaN(self.settings.metadata.min_snr)){
            self.settings.metadata.min_snr = 31;
          }

          if(self.settings.metadata.min_fix === undefined || isNaN(self.settings.metadata.min_fix)){
            self.settings.metadata.min_fix = 3;
          }

          if(self.settings.metadata.kalman_speed === undefined || isNaN(self.settings.metadata.kalman_speed)){
            self.settings.metadata.kalman_speed = 3;
          }

          if(self.settings.metadata.kalman_accuracy === undefined || isNaN(self.settings.metadata.kalman_accuracy)){
            self.settings.metadata.kalman_accuracy = 1;
          }

          if(self.settings.metadata.default_avg_sampling_number === undefined || isNaN(self.settings.metadata.default_avg_sampling_number)){
            self.settings.metadata.default_avg_sampling_number = 20;
          }

          if(self.settings.metadata.default_wait === undefined || isNaN(self.settings.metadata.default_wait)){
            self.settings.metadata.default_wait = 10;
          }

          if(self.settings.metadata.default_n_campionamenti === undefined || isNaN(self.settings.metadata.default_n_campionamenti)){
            self.settings.metadata.default_n_campionamenti = 20;
          }

          if(self.settings.metadata.default_range_oscillazione === undefined || isNaN(self.settings.metadata.default_range_oscillazione)){
            self.settings.metadata.default_range_oscillazione = 50;
          }

          if(self.settings.metadata.use_gps === undefined || isNaN(self.settings.metadata.use_gps)){
            self.settings.metadata.use_gps = 0;
          }

          if(self.settings.metadata.use_glonass === undefined || isNaN(self.settings.metadata.use_glonass)){
            self.settings.metadata.use_glonass = 0;
          }

          if(self.settings.metadata.use_galileo === undefined || isNaN(self.settings.metadata.use_galileo)){
            self.settings.metadata.use_galileo = 0;
          }

          if(self.settings.metadata.centroid_computation_time === undefined || isNaN(self.settings.metadata.centroid_computation_time)){
            self.settings.metadata.centroid_computation_time = 60;
          }
          if($rootScope.survey_active){
            self.attiva_interval_centroid();
          }
        });

        appService.safeApply($rootScope, function(){
          self.settings['layers'] = baseLayersTypes;
        });

      },
      function(err){
        console.log(err);
      }
    );

  };

  self.attiva_interval_centroid = function(){
    var interval_in_milli = self.settings.metadata.centroid_computation_time*1000;
    window.setInterval(self.check_dir_exists_and_writefile, interval_in_milli);
  };

  self.kalman_on_lat = function(lats){
    KalmanFilter.constructor({R: 0.01, Q: 3});
    var dataConstantKalman = lats.map(function(v) {
      return KalmanFilter.filter(v);
    });
  };

  self.kalman_on_lon = function(lons){
    KalmanFilter.constructor({R: 0.01, Q: 3});
    var dataConstantKalman = lons.map(function(v) {
      return KalmanFilter.filter(v);
    });
  };

  self.compute_mean_lat = function(t_campionamenti_lat){
    var tot = 0;
    for(var i=0; i<t_campionamenti_lat.length; i++){
      tot += t_campionamenti_lat[i];
    }
    return tot/t_campionamenti_lat.length;
  };

  self.compute_mean_lon = function(t_campionamenti_lon){
    var tot = 0;
    for(var i=0; i<t_campionamenti_lon.length; i++) {
      tot += t_campionamenti_lon[i];
    }
    return tot/t_campionamenti_lon.length;
  };

  self.distance = function(lat1, lon1, lat2, lon2){
    var p = 0.017453292519943295;    // Math.PI / 180
    var c = Math.cos;
    var a = 0.5 - c((lat2 - lat1) * p)/2 +
      c(lat1 * p) * c(lat2 * p) *
      (1 - c((lon2 - lon1) * p))/2;

    return 100000 * 12742 * Math.asin(Math.sqrt(a)); // 2 * R; R = 6371 km
  };

  self.isGpsActive = function(){
    return gpsActive;
  };

  self.setGpsStatus = function(status){
    gpsActive = status;
  };

  self.parseOthers = function (splitted) {

  };

  self.parseVTG = function(splitted){

    if(splitted.length < 11){
      for(var i = splitted.length-1; i<=11; i++){
        splitted.push(0);
      }
    }

    var data = {
      tr_deg_1:null,
      tr_deg_2:null,
      speed_knots:null,
      speed_kmh:null
    };
    data.tr_deg_1 = splitted[1];
    data.tr_deg_2 = splitted[3];
    data.speed_knots = splitted[5];
    data.speed_kmh = splitted[7];
    $rootScope.$broadcast('nmeaVTGReceived', [data]);
  };

  self.fromDegToDec = function(inDeg){
    if(inDeg.charAt(0) === '0'){
      inDeg = inDeg.substring(1,inDeg.length);
    }
    var deg = parseFloat(inDeg.substring(0,2));
    var min = parseFloat(inDeg.substring(2,inDeg.length));
    var inDec = deg + (min/60);
    var inDecStr = inDec.toString();
    if(inDecStr.length > 10){
      inDecStr = inDecStr.substring(0,10);
    }
    return inDecStr;
  };

  self.track_kalman_filter = function(camp_list){
    var output_list = [];
    var accuracy = self.settings.metadata.kalman_accuracy;
    var variance = -1;
    var lat = 0;
    var lon = 0;
    var timestamp = 0;
    var Q_metres_per_second = 3;
    for(var i=0; i<camp_list.length; i++){
      if(variance < 0){
        timestamp = camp_list[i][2];
        lat = camp_list[i][0];
        lon = camp_list[i][1];
        variance = accuracy*accuracy;
      }else{
        var time_millis = camp_list[i][2] - timestamp;
        if(time_millis > 0){
          variance += time_millis * Q_metres_per_second * Q_metres_per_second / 1000;
          timestamp = camp_list[i][2];
        }

        var K = variance / (variance + accuracy * accuracy);
        lat += K * (camp_list[i][0] - lat);
        lon += K * (camp_list[i][1] - lon);
        variance = (1 - K) * variance;
        output_list.push([lat, lon, camp_list[i][3], camp_list[i][4], camp_list[i][5], camp_list[i][6], Q_metres_per_second, camp_list[i][7]]);
      }
    }
    return output_list;
  };

  var campionamenti_lat_lng = [];
  var campionamenti_kalman_filtered = [];
  self.kalman_filter = function(camp_list){
    var accuracy = self.settings.metadata.kalman_accuracy;
    var variance = -1;
    var lat = 0;
    var lon = 0;
    var timestamp = 0;
    var Q_metres_per_second = self.settings.metadata.kalman_speed;
    for(var i=0; i<camp_list.length; i++){
      if(variance < 0){
        timestamp = camp_list[i][2];
        lat = camp_list[i][0];
        lon = camp_list[i][1];
        variance = accuracy*accuracy;
      }else{
        var time_millis = camp_list[i][2] - timestamp;
        if(time_millis > 0){
          variance += time_millis * Q_metres_per_second * Q_metres_per_second / 1000;
          timestamp = camp_list[i][2];
        }

        var K = variance / (variance + accuracy * accuracy);

        lat += K * (camp_list[i][0] - lat);
        lon += K * (camp_list[i][1] - lon);
        variance = (1 - K) * variance;
        campionamenti_kalman_filtered.push([lat, lon, camp_list[i][3], camp_list[i][4], camp_list[i][5], camp_list[i][6], camp_list[i][7], camp_list[i][8], camp_list[i][9]]);
      }
  }
    self.kalman_lat_lon = campionamenti_kalman_filtered;
  };

  self.last_valid_lat = null;
  self.last_valid_lon = null;
  self.last_valid_latwgs84 = null;
  self.last_valid_lonwgs84 = null;
  var to_jump = 0;

  self.pure_lat_lon = [];
  self.filtered_lat_lon = [];
  self.kalman_lat_lon = [];
  self.mean_lat_lon = [];
  self.gaussian_lat_lon = [];
  self.pure_lat = [];
  self.pure_lon = [];

  self.check_dir_exists_and_writefile = function(){
    var pathToFile = 'file:///storage/emulated/0/Panoramas';
    var pathToFile2 = 'file:///storage/emulated/0/';
    window.resolveLocalFileSystemURL(pathToFile,
      function(dirEntry){
        if($rootScope.survey_active){
          self.writeCoordsFilterToFile();
        }else{}
      }, function(){
        window.resolveLocalFileSystemURL(pathToFile2, function(rootDirEntry){
          self.createDirectory(rootDirEntry);
        }, function(){
          console.log("ERRORE IN CREAZIONE CARTELLA");
        })
      });
  };

  self.createDirectory = function(rootDirEntry){
    rootDirEntry.getDirectory('Panoramas', { create: true }, function (dirEntry) {
      if($rootScope.survey_active){
        self.writeCoordsFilterToFile();
      }else{}
    }, function(){
      console.log("ERROR CREATING DIR");
    });
  };

  self.writeCoordsFilterToFile = function(){

    var centroid_lats = [];
    var centroid_lons = [];

    for(var j=0; j<self.hull_matrix.length; j++){
      if(!isNaN(self.hull_matrix[j][0])){
        centroid_lats.push(self.hull_matrix[j][0]);
      }
      if(!isNaN(self.hull_matrix[j][1])) {
        centroid_lons.push(self.hull_matrix[j][1]);
      }
    }
    // var s_lat = adNormalityTest.check(centroid_lats).stdP;
    // var s_lon = adNormalityTest.check(centroid_lons).stdP;
    var s_lat = "";
    var s_lon ="";

    var mean_centroid_lat = self.compute_mean_lat(centroid_lats);
    var mean_centroid_lon = self.compute_mean_lon(centroid_lons);

    if(need_to_convert_centroids){
      var location_centroid = coordinatesService.from3857to30033004({'latitude': mean_centroid_lat,'longitude': mean_centroid_lon},sessionService.getAuthId());
    }else{
      var location_centroid = coordinatesService.fromWGS84to3857({'latitude': mean_centroid_lat,'longitude': mean_centroid_lon});
    }

    $rootScope.$broadcast('meanCentroidComputed', [mean_centroid_lat, mean_centroid_lon, location_centroid.x, location_centroid.y, s_lat, s_lon]);
    ref_point = [];
    campionamenti_lat = [];
    campionamenti_lon = [];

    self.hull_matrix = [];
    self.pure_lat_lon = [];
  };

  self.compute_centroid = function(points){
    var hull_points = hull(points, 50);
    var last_hull = [];
    while(hull_points.length > 0){
      last_hull = hull_points;
      for(var i = 0; i<hull_points.length; i++){
        points.splice(points.indexOf(hull_points[i]), 1);
      }
      hull_points = hull(points, 50);
    }

    var centroid_mean_lat = 0;
    var centroid_mean_lon = 0;
    if(points.length === 0){
      points = last_hull;
    }

    for(i=0; i<points.length; i++){
      centroid_mean_lat += points[i][0];
      centroid_mean_lon += points[i][1];
    }
    centroid_mean_lat = centroid_mean_lat/points.length;
    centroid_mean_lon = centroid_mean_lon/points.length;
    var distance = self.calculateDistance([centroid_mean_lat, centroid_mean_lon], points);
    $rootScope.$broadcast("precisionAcquired", [distance]);
    return [centroid_mean_lat, centroid_mean_lon];
  };

  self.calculateDistance = function(centroid, perimeter_points) {
    var tot_distance = 0;
    for(var i=0; i<perimeter_points.length; i++){
      // var distance = Math.sqrt(Math.pow((centroid[0]-perimeter_points[i][0]),2)+Math.pow((centroid[1]-perimeter_points[i][1]),2));
      var distance = self.distance_centroids(centroid[0], perimeter_points[i][0], centroid[1], perimeter_points[i][1]);
      tot_distance += distance;
    }
    // var mean_distance = Math.sqrt(tot_distance)/perimeter_points.length;
    var mean_distance = tot_distance/perimeter_points.length;

    return mean_distance;
  };

  self.distance_centroids = function(lat1, lat2, lon1, lon2) {

    var R = 6371; // Radius of the earth

    var latDistance = ol.math.toRadians(lat2 - lat1);
    var lonDistance = ol.math.toRadians(lon2 - lon1);
    var a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
      + Math.cos(ol.math.toRadians(lat1)) * Math.cos(ol.math.toRadians(lat2))
      * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    var distance = R * c * 1000; // convert to meters

    distance = Math.pow(distance, 2);
    return distance;
  };

  var list_per_kalm = [];
  var camp_tracking_lat = [];
  var camp_tracking_lon = [];
  self.track_filtered_lat_lon = [];
  self.track_gaussian_lat_lon = [];
  self.track_mean_lat_lon = [];
  var track_list_per_kalm = [];
  self.track_kalman_lat_lon = [];
  self.track_pure_lat_lon = [];
  self.hull_matrix = [];
  var curr_sats_in_view = 0;
  var curr_pdop = 99;
  var curr_vdop = 99;
  var sat_info_matrix = [];
  var sat_info_matrix_track = [];

  var mean_altitudes_from_service = [];
  var mean_altitudes_from_satellite = [];
  var need_to_convert_centroids = false;

  self.parseGGA = function(splitted, coordinatesNeedConversion){

    need_to_convert_centroids = coordinatesNeedConversion;

    if(splitted.length < 15){
      for(var i = splitted.length-1; i<=15; i++){
        splitted.push(0);
      }
    }

    var data = {
      latitude:null,
      longitude:null,
      altitude:null,
      n_s:null,
      e_w:null,
      n_sat:null,
      time:null,
      accuracy:null,
      fix_type:null,
      hdop:null
    };

    data.latitude = self.fromDegToDec(splitted[2]);
    data.longitude = self.fromDegToDec(splitted[4]);

    data.n_s = splitted[3];
    data.e_w = splitted[5];
    if(data.n_s === "S"){
      data.latitude = -data.latitude;
    }
    if(data.e_w === "W"){
      data.longitude = -data.longitude;
    }

    curr_sats_number = parseInt(splitted[7]);

    if(isNaN(curr_mean_snr)){
      curr_mean_snr = curr_mean_snr_stable;
    }else{
      curr_mean_snr_stable = curr_mean_snr;
    }

    if(data.latitude !== "" && !isNaN(data.latitude)){
      if(getAltitudeFromService){
        mean_altitudes_from_satellite.push(splitted[9]);
        httpService.getAltitudeFromLatLon(data.latitude,data.longitude).then(function (res) {
          mean_altitudes_from_service.push(res[0]['results'][0]);
        });
      }

      if($rootScope.survey_active){
        self.pure_lat_lon.push([data.latitude, data.longitude, curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop, splitted[1], current_full_timestamp]);
        var liste_sat = [[self.sats_with_snr['gp'], 'gps_sat_list', 'gp'],[self.sats_with_snr['gl'], 'glonass_sat_list', 'gl'],[self.sats_with_snr['ga'], 'galileo_sat_list', 'ga'],[self.sats_with_snr['bd'], 'beidou_sat_list', 'bd']];
        if(Object.keys(liste_sat[0][0]).length < 1){
          liste_sat = constant_sat_info;
        }
        var lista_sat_tot = [];
        for(var i=0; i<liste_sat.length; i++){
          for(var x=0; x<Object.keys(liste_sat[i][0]).length; x++){
            var k = Object.keys(liste_sat[i][0])[x];
            lista_sat_tot.push({'prn':k,'snr':liste_sat[i][0][k][0], 'elevation':liste_sat[i][0][k][1], 'azimuth':liste_sat[i][0][k][2]});
          }
        }
        sat_info_matrix.push(lista_sat_tot);
        $rootScope.curr_sats_info = lista_sat_tot;

        //SALVO PUNTO PER ANALISI SOLO SE CONDIZIONI FILTRO SONO RISPETTATE
        if(!($rootScope.filterActive)  || (curr_sats_number >= self.settings.metadata.min_n_sat &&
          parseFloat(splitted[8]) <= self.settings.metadata.min_hdop &&
          curr_mean_snr >= self.settings.metadata.min_snr &&
          parseInt(fix_value) >= self.settings.metadata.min_fix)){

          self.pure_lat.push(parseFloat(data.latitude));
          self.pure_lon.push(parseFloat(data.longitude));

          list_per_kalm.push([parseFloat(data.latitude), parseFloat(data.longitude), parseInt(splitted[1]), curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop]);

        }

        if(self.pure_lat.length >= self.settings.metadata.n_campionamenti && self.pure_lon.length >= self.settings.metadata.n_campionamenti){
          var normalDistLon1 = adNormalityTest.check(self.pure_lon);
          var normalDistLat1 = adNormalityTest.check(self.pure_lat);

          self.filtered_lat_lon.push([normalDistLat1.mean, normalDistLon1.mean]);
          self.gaussian_lat_lon.push([normalDistLat1.mean, normalDistLon1.mean, curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop]);

          campionamenti_lat = self.pure_lat;
          campionamenti_lon = self.pure_lon;
          self.mean_lat_lon.push([self.compute_mean_lat(campionamenti_lat), self.compute_mean_lon(campionamenti_lon), curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop]);

          var points = [];
          for(var i=0; i<self.pure_lat.length; i++){
            if(i<self.pure_lon.length){
              points.push([self.pure_lat[i],self.pure_lon[i]]);
            }
          }

          var res = self.compute_centroid(points);

          if(res !== -1){
            self.hull_matrix.push([res[0], res[1], curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop]);
          }

          self.pure_lat = [];
          self.pure_lon = [];
        }
      }else{
        self.pure_lat_lon = [];
        self.pure_lat = [];
        self.pure_lon = [];
        list_per_kalm = [];
        self.filtered_lat_lon = [];
        self.gaussian_lat_lon = [];
        campionamenti_lat = self.pure_lat;
        campionamenti_lon = self.pure_lon;
        self.mean_lat_lon = [];
      }
    }

    var location;
    if(coordinatesNeedConversion){
    	location = coordinatesService.from3857to30033004({'latitude': data.latitude,'longitude': data.longitude},"3004");
    }else{
      var location_sphere = coordinatesService.fromWGS84to3857({'latitude': data.latitude,'longitude': data.longitude});
      location = location_sphere;
    }

    data.time = splitted[1];
    data.x = location.x;
    data.y = location.y;
    if(location.z === null){
      location.z = splitted[9];
    }
    data.z = location.z;
    data.altitude = splitted[9];
    data.n_sat = splitted[7];
    data.hdop = splitted[8];
    data.fix_type = splitted[6];
    data.normality_lat = {'mean':'', 'normal':'', 'pValue':'', 'stdP':'', 'stdS':''};
    data.normality_lon = {'mean':'', 'normal':'', 'pValue':'', 'stdP':'', 'stdS':''};

    var d = new Date();
    var format_date = d.getUTCDate() + '-' + (d.getUTCMonth()+1) + '-' + d.getUTCFullYear();
    var format_time = data.time.charAt(0)+data.time.charAt(1)+':'+data.time.charAt(2)+data.time.charAt(3)+':'+data.time.charAt(4)+data.time.charAt(5);
    var utc_timestamp = format_date + ' ' + format_time;
    var low_hdop = true;

    if(parseFloat(data.hdop) > self.settings.metadata.min_hdop){
      low_hdop = false;
    }else{
      low_hdop = true;
    }

    if(!$rootScope.survey_active) {

      campionamenti_lat = [];
      campionamenti_lon = [];
      ref_point = [];

      self.last_valid_lat = data.y;
      self.last_valid_lon = data.x;
      self.last_valid_latwgs84 = data.latitude;
      self.last_valid_lonwgs84 = data.longitude;

      $rootScope.$broadcast('nmeaGGAReceived', [data]);
      $rootScope.$broadcast('locationAcquired', ['device', data, utc_timestamp]);

      if(data.latitude !== "" && !isNaN(data.latitude)){

          camp_tracking_lat.push(parseFloat(data.latitude));
          camp_tracking_lon.push(parseFloat(data.longitude));
          track_list_per_kalm.push([parseFloat(data.latitude), parseFloat(data.longitude), parseInt(splitted[1]), curr_sats_number, splitted[8], curr_mean_snr, fix_value, current_sped_kmh, curr_sats_in_view, curr_pdop, curr_vdop, data.altitude, current_track_mg]);

          self.track_pure_lat_lon.push([data.latitude, data.longitude, curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop, splitted[1], data.altitude, current_track_mg, current_sped_kmh, current_full_timestamp]);

        var liste_sat = [[self.sats_with_snr['gp'], 'gps_sat_list', 'gp'],[self.sats_with_snr['gl'], 'glonass_sat_list', 'gl'],[self.sats_with_snr['ga'], 'galileo_sat_list', 'ga'],[self.sats_with_snr['bd'], 'beidou_sat_list', 'bd']];

        if(Object.keys(liste_sat[0][0]).length < 1){
          liste_sat = constant_sat_info;
        }
        var lista_sat_tot = [];

        for(var i=0; i<liste_sat.length; i++){
          for(var x=0; x<Object.keys(liste_sat[i][0]).length; x++){
            var k = Object.keys(liste_sat[i][0])[x];
            lista_sat_tot.push({'prn':k,'snr':liste_sat[i][0][k][0], 'elevation':liste_sat[i][0][k][1], 'azimuth':liste_sat[i][0][k][2]});
          }
        }
        sat_info_matrix_track.push(lista_sat_tot);
        $rootScope.curr_sats_info = lista_sat_tot;
        if(camp_tracking_lat.length >= self.settings.metadata.avg_sampling_number){
            var track_normalDistLon1 = adNormalityTest.check(camp_tracking_lon);
            var track_normalDistLat1 = adNormalityTest.check(camp_tracking_lat);

            self.track_filtered_lat_lon.push([track_normalDistLat1.mean, track_normalDistLon1.mean]);
            self.track_gaussian_lat_lon.push([track_normalDistLat1.mean, track_normalDistLon1.mean, curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop]);

            self.track_mean_lat_lon.push([self.compute_mean_lat(camp_tracking_lat), self.compute_mean_lon(camp_tracking_lon), curr_sats_number, splitted[8], curr_mean_snr, fix_value, curr_sats_in_view, curr_pdop, curr_vdop]);

            camp_tracking_lat.splice(0,1);
            camp_tracking_lon.splice(0,1);
          }
      }
    }else{
      if (ref_point.length === 0) {

        if (campionamenti_lon.length < self.settings.metadata.n_campionamenti) {
          var num = self.settings.metadata.n_campionamenti-campionamenti_lon.length;
          $rootScope.$broadcast('surveyCountdown', [self.settings.metadata.n_campionamenti-campionamenti_lon.length]);
          if(!$rootScope.filterActive){
            campionamenti_lon.push(parseFloat(data.x));
            campionamenti_lat.push(parseFloat(data.y));
            campionamenti_lat_lng.push([parseFloat(data.y), parseFloat(data.x), parseInt(data.time)]);
          }else{
            if(low_hdop && !snr_too_low && !num_sats_low && fix3d){
              campionamenti_lon.push(parseFloat(data.x));
              campionamenti_lat.push(parseFloat(data.y));
              campionamenti_lat_lng.push([parseFloat(data.y), parseFloat(data.x), parseInt(data.time)]);
            }else{

            }
          }
        }else{
            $rootScope.$broadcast('surveyCountdown', [0]);
            self.kalman_on_lat(campionamenti_lat);
            self.kalman_on_lon(campionamenti_lon);
            data.x = self.compute_mean_lon(campionamenti_lon);
            data.y = self.compute_mean_lat(campionamenti_lat);
            self.last_valid_lat = data.y;
            self.last_valid_lon = data.x;
            self.last_valid_latwgs84 = data.latitude;
            self.last_valid_lonwgs84 = data.longitude;
            ref_point = [data.longitude, data.latitude];
            $rootScope.$broadcast('nmeaGGAReceived', [data]);
            $rootScope.$broadcast('locationAcquired', ['device', data, utc_timestamp]);
            $rootScope.$broadcast('photoCentroidComputed', [data.y, data.x]);
            campionamenti_lat = [];
            campionamenti_lon = [];
            to_jump = 0;
        }
      }else{
        var dist = self.distance(data.longitude, data.latitude, ref_point[0], ref_point[1]);
        $rootScope.$broadcast('distFromRef',[dist.toFixed(3)]);
        if (!($rootScope.filterActive)  || ($rootScope.filterActive && low_hdop && !snr_too_low && !num_sats_low && fix3d)) {
          $rootScope.$broadcast('nmeaGGAReceived', [data]);
          $rootScope.$broadcast('locationAcquired', ['device', data, utc_timestamp]);
        }else{
          if(self.last_valid_lat != null && self.last_valid_lon != null){
            data.x = self.last_valid_lon;
            data.y = self.last_valid_lat;
            data.latitude = self.last_valid_latwgs84;
            data.longitude = self.last_valid_lonwgs84;
          }else{
            data.x = NaN;
            data.y = NaN;
            data.latitude = NaN;
            data.longitude = NaN;
          }
          $rootScope.$broadcast('nmeaGGAReceived', [data]);
          $rootScope.$broadcast('locationAcquired',['device', data, utc_timestamp]);
        }
      }
    }
  };

  var current_track_mg = "0.0";
  var current_sped_kmh = "";
  var current_full_timestamp = "";
  self.parseRMC = function(splitted, coordinatesNeedConversion){

    if(splitted.length < 13){
      for(var i = splitted.length-1; i<=13; i++){
        splitted.push(0);
      }
    }

    var data = {
      lat:null,
      lon:null,
      n_s:null,
      e_w:null,
      speed:null,
      magn_var:null,
      track_mg:null,
      e_w_magn:null
    };
    data.time = splitted[1];
    data.date = splitted[9];
    var format_time = data.time.charAt(0)+data.time.charAt(1)+':'+data.time.charAt(2)+data.time.charAt(3)+':'+data.time.charAt(4)+data.time.charAt(5);
    var format_date = data.date.charAt(0)+data.date.charAt(1)+'-'+data.date.charAt(2)+data.date.charAt(3)+'-'+data.date.charAt(4)+data.date.charAt(5);
    var utc_timestamp = format_date + ' ' + format_time;
    current_full_timestamp = format_date + '_' + format_time;
    data.lat = self.fromDegToDec(splitted[3]);
    data.lon = self.fromDegToDec(splitted[5]);
    data.n_s = splitted[4];
    data.e_w = splitted[6];
    data.speed = splitted[7];
    data.magn_var = splitted[10];
    data.track_mg = splitted[8];
    data.e_w_magn = splitted[11];

    if(!isNaN(data.track_mg) && data.track_mg !== null && data.track_mg !== ""){
      current_track_mg = data.track_mg;
    }

    current_sped_kmh = data.speed;

    var location;
    if(coordinatesNeedConversion){
      location = coordinatesService.from3857to30033004({'latitude': data.lat,'longitude': data.lon},sessionService.getAuthId());
    }else{
      location = {'x': data.lon, 'y': data.lat, 'z': 0};
    }

    $rootScope.$broadcast('nmeaRMCReceived', [data]);
    $rootScope.$broadcast('locationAcquiredRMC',['device', data, utc_timestamp])
  };

  self.interval_time = 1000;
  self.currGalileos = [];

  self.gps_ids_snr = [];
  self.glonass_ids_snr = [];
  self.galileo_ids_snr = [];
  self.beidou_ids_snr = [];

  self.gpsSats = [];
  self.glonassSats = [];
  self.beidouSats = [];
  self.right_list = {'gp':self.gps_ids_snr, 'ga':self.galileo_ids_snr, 'gl':self.glonass_ids_snr, 'bd':self.beidou_ids_snr};
  self.right_list_sat_only = {'gp':self.gpsSats, 'ga':self.currGalileos, 'gl':self.glonassSats, 'bd':self.beidouSats};

  var fix3d = false;
  var fix_value = 0;
  self.parseGSA = function (splitted, netw) {
    if(splitted.length < 18){
      for(var i = splitted.length-1; i<=18; i++){
        splitted.push(0);
      }
    }
    var data = {
      fix_mode:null,
      s1:null,
      s2:null,
      s3:null,
      s4:null,
      s5:null,
      s6:null,
      s7:null,
      s8:null,
      s9:null,
      s10:null,
      s11:null,
      s12:null,
      hdop:null,
      pdop:null,
      vdop:null
    };
    data.fix_mode = splitted[2];
    data.s1 = splitted[3];
    data.s2 = splitted[4];
    data.s3 = splitted[5];
    data.s4 = splitted[6];
    data.s5 = splitted[7];
    data.s6 = splitted[8];
    data.s7 = splitted[9];
    data.s8 = splitted[10];
    data.s9 = splitted[11];
    data.s10 = splitted[12];
    data.s11 = splitted[13];
    data.s12 = splitted[14];
    data.hdop = splitted[16];
    data.pdop = splitted[15];
    if(splitted.length === 18){
      data.vdop = splitted[17].split('*')[0];
    }else{
      data.vdop = splitted[17];
    }

    curr_pdop = data.pdop;
    curr_vdop = data.vdop;

    fix_value = data.fix_mode;
    if(parseInt(data.fix_mode)>=self.settings.metadata.min_fix){
      fix3d = true;
    }else{
      fix3d = false;
    }

    if(netw === 'gn'){
      for(var i=1; i<13; i++) {
        if (parseInt(data['s' + i]) < 33 && self.gpsSats.indexOf(data['s' + i]) === -1) {
          self.gpsSats.push(data['s' + i]);
          self.right_list['gp'].push([data['s' + i],null]);
        }
        if (parseInt(data['s' + i]) > 66 && parseInt(data['s' + i]) < 88 && self.glonassSats.indexOf(data['s' + i]) === -1) {
          self.glonassSats.push(data['s' + i]);
          self.right_list['gl'].push([data['s' + i],null]);
        }
      }
    }

    $rootScope.$broadcast('nmeaGSAReceived', [data, netw]);
  };

  self.curr_snr = [];
  self.list_of_mean_snr = [];
  self.compute_mean_snr = function (snr_list) {
    var min_snr = 99999;
    var max_snr = 0;
    var min_snr_i = 0;
    var max_snr_i = 0;

    Object.keys(self.sats_with_snr).forEach(function(key,index){
      Object.keys(self.sats_with_snr[key]).forEach(function(key2,index2){
        if(self.sats_with_snr[key][key2] < 100){
          snr_list.push(self.sats_with_snr[key][key2]);
        }
      });
    });

    for(var i = 0; i<snr_list.length; i++){
      if(snr_list[i] > max_snr){
        max_snr = snr_list[i];
        max_snr_i = i;
      }
    }
    snr_list.splice(max_snr_i,1);

    for(var j = 0; j<snr_list.length; j++){
      if(snr_list[j] < min_snr){
        min_snr = snr_list[j];
        min_snr_i = j;
      }
    }
    snr_list.splice(min_snr_i,1);

    var mean_snr = 0;

    for(var x = 0; x<snr_list.length; x++){
      mean_snr += snr_list[x];
    }
    mean_snr = Math.ceil(mean_snr/snr_list.length);

    snr_list = [];

    return mean_snr;
  };

  self.sats_with_snr = {'gp':{}, 'gl':{}, 'ga':{}, 'bd':{}};
  var list_data = [];
  self.parseGSV = function (splitted, netw) {

    var freq = "E1";
    splitted[splitted.length-1] = splitted[splitted.length-1].split('*')[0];

    if(netw === "ga"){
      if(splitted[splitted.length-1] === "1"){
        freq = "E5";
      }
    }

    if(netw === "gp"){
      if(splitted[splitted.length-1] === "8"){
        freq = "L5";
      }else{
        freq = "L1";
      }
    }

    list_data = [];

    var num_blocks = (splitted.length - 4)/4;

    for(var i=1; i<=num_blocks; i++){
      if(splitted.length >= (4*i)+3){
        var data = {
          prn : null,
          snr : null,
          elevation : null,
          azimuth : null
        };

          data.prn = splitted[4*i];
          data.elevation = splitted[(4*i)+1];
          data.azimuth = splitted[(4*i)+2];
          data.snr = splitted[(4*i)+3];
          if(netw === 'ga' || netw === 'gp'){
            data.band = freq;
            data.prn = data.prn+"_"+data.band;
          }else{
            data.band = "n/d";
          }
          if(data.snr === "" || data.snr === " "){
            continue;
          }
          self.sats_with_snr[netw][data.prn] = [parseInt(data.snr), parseInt(data.elevation), parseInt(data.azimuth)];
          list_data.push(data);

          if(netw == 'ga' && self.currGalileos.indexOf(data.prn) === -1){
            if(data.prn !== '0*74'){
              self.currGalileos.push(data.prn);
              self.right_list[netw].push([data.prn, data.snr]);
            }
          }

          if(self.right_list_sat_only[netw].indexOf(data.prn) === -1){
            // self.right_list[network].push([data.prn, data.snr]);
          }
          else{
            for(var j=0; j<self.right_list[netw].length; j++){
              if(self.right_list[netw][j][0] === data.prn){
                self.right_list[netw][j][1] = data.snr;
              }
            }
          }
      }
    }

    var temp = [];
    Object.keys(self.sats_with_snr).forEach(function(key1,index1){
      Object.keys(self.sats_with_snr[key1]).forEach(function(key2,index2){
        if(!isNaN(self.sats_with_snr[key1][key2][0])){
          temp.push(self.sats_with_snr[key1][key2][0]);
        }
      });
    });

    var mean_snr = self.compute_mean_snr(temp);
    if(!isNaN(mean_snr)){
      self.curr_snr.push(mean_snr);
    }

    $rootScope.$broadcast('nmeaGSVReceived', [list_data, netw]);
  };

  self.start_scheduler_satlist = function(){
    self.scheduler = $interval(self.update_sats_lista_and_number, self.interval_time);
  };

  self.stop_scheduler_satlist = function(){
    $interval.cancel(self.scheduler);
  };

  var curr_sats_number = 0;
  var curr_mean_snr = 0;
  var curr_mean_snr_stable = 0;
  self.update_sats_lista_and_number = function(){
    var liste_sat = [[self.sats_with_snr['gp'], 'gps_sat_list', 'gp'],[self.sats_with_snr['gl'], 'glonass_sat_list', 'gl'],[self.sats_with_snr['ga'], 'galileo_sat_list', 'ga'], [self.sats_with_snr['bd'], 'beidou_sat_list', 'bd']];
    constant_sat_info = liste_sat;
    var tot_sat = 0;
    for(var i=0; i<liste_sat.length; i++){
      tot_sat += Object.keys(liste_sat[i][0]).length;
    }

    curr_sats_in_view = tot_sat;

    if(tot_sat < self.settings.metadata.min_n_sat){
      num_sats_low = true;
    }else{
      num_sats_low = false;
    }

    var snr = self.compute_mean_snr(self.curr_snr);
    curr_mean_snr = snr;
    if(!isNaN(snr)){
      $rootScope.$broadcast('meansnrMessage',[snr]);
      if(snr < self.settings.metadata.min_snr){
        snr_too_low = true;
      }else{
        snr_too_low = false;
      }
    }

    self.curr_snr = [];
    self.gpsSats = [];
    self.glonassSats = [];
    self.currGalileos = [];
    self.sats_with_snr = {'gp':{}, 'gl':{}, 'ga':{}, 'bd':{}};
  };

  self.broadcastNMEAMessage = function(str){
    $rootScope.$broadcast('nmeaRawMessage', [str]);
  };

  self.requestNmeaMessages = function(coordinatesNeedConversion){

      self.reloadMetadata();

      if ($rootScope.externalData) {

      } else {
          try {
            window.internalnmea.registerReadCallback(
              function success(data) {
                self.readInternalNmeaStream(data, coordinatesNeedConversion);
              },
              function (err) {

              }
            );
            self.start_scheduler_satlist();
          } catch (err) {
          }
      }
  };

  self.stopNmeaMessages = function(){

    ref_point = [];
    campionamenti_lat = [];
    campionamenti_lon = [];
    campionamenti_lat_lng = [];

    self.check_dir_exists_and_writefile();

    self.stop_scheduler_satlist();
    window.internalnmea.stopNmea(
      function success(data){
      },
      function(err){
        console.log("ERROR STOPPING NMEA - "+err);
      }
    );
  };

  self.lm_lats = [];
  self.lm_lons = [];
  self.lm_hull_matrix = [];
  self.lm_alts = [];
  self.checkLengthAndComputeCentroids = function(lat, lon, alt, ts, sampling_number){
    self.lm_lats.push(lat);
    self.lm_lons.push(lon);
    self.lm_alts.push(alt);
    if(self.lm_lats.length >= sampling_number && self.lm_lons.length >= sampling_number){

      //calcolo media
      var tot_alt = 0;
      for(var i=0; i<self.lm_alts.length; i++){
        tot_alt += self.lm_alts[i];
      }
      if(self.lm_alts.length > 0){
        var mean_alt = tot_alt/self.lm_alts.length;
      }else{
        var mean_alt = 0;
      }

      var points = [];
      for(var i=0; i<self.lm_lats.length; i++){
        if(i<self.lm_lons.length){
          points.push([self.lm_lats[i],self.lm_lons[i]]);
        }
      }

      var res = self.compute_centroid(points);
      if(res !== -1){
        self.lm_hull_matrix.push([res[0], res[1], mean_alt, ts]);
      }

      self.lm_lats = [];
      self.lm_lons = [];
    }
  };

  $rootScope.extra_sat_number = -1;
  self.readInternalNmeaStream = function(data, coordinatesNeedConversion){
    var view = new Uint8Array(data);
    if(view.length >= 1) {
      for (var i = 0; i < view.length; i++) {
        if (view[i] === 13) {
          var obj = JSON.parse(str);
          if(obj['provider'] === "ExtraProvider"){
            $rootScope.extra_sat_number = obj["satellites"];
          }
          if(obj['provider'] === "Nmea"){
            $rootScope.from_mock = false;
            $rootScope.no_extras = false;

            str = obj["nmea_message"].replace("\r", "");

            if(str === "ERROR"){
              $rootScope.$broadcast('deviceError', []);
            }

            self.broadcastNMEAMessage(str);
            var elements = str.split(',');
            var prefix = elements[0].substring(1,elements[0].length);

            switch (prefix){

              case 'GNGGA':
                self.parseGGA(elements, coordinatesNeedConversion);
                break;
              case 'GPGGA':
                self.parseGGA(elements, coordinatesNeedConversion);
                break;
              case 'GLGGA':
                self.parseGGA(elements, coordinatesNeedConversion);
                break;
              case 'GAGGA':
                self.parseGGA(elements, coordinatesNeedConversion);
                break;
              case 'GNRMC':
                self.parseRMC(elements, coordinatesNeedConversion);
                break;
              case 'GPRMC':
                self.parseRMC(elements, coordinatesNeedConversion);
                break;
              case 'GLRMC':
                self.parseRMC(elements, coordinatesNeedConversion);
                break;
              case 'GARMC':
                self.parseRMC(elements, coordinatesNeedConversion);
                break;
              case 'GPGSA':
                self.parseGSA(elements, 'gn');
                break;
              case 'GLGSA':
                self.parseGSA(elements, 'gn');
                break;
              case 'GNGSA':
                self.parseGSA(elements, 'gn');
                break;
              case 'GNVTG':
                self.parseVTG(elements);
                break;
              case 'GAGSV':
                if(self.settings.metadata.use_galileo) {
                  self.parseGSV(elements, 'ga');
                }
                break;
              case 'GPGSV':
                if(self.settings.metadata.use_gps) {
                  self.parseGSV(elements, 'gp');
                }
                break;
              case 'GLGSV':
                if(self.settings.metadata.use_glonass) {
                  self.parseGSV(elements, 'gl');
                }
                break;
              case 'BDGSV':
                self.parseGSV(elements, 'bd');
                break;
              default:
                self.parseOthers(elements);
                break;
            }
          }
          $rootScope.$broadcast('nmeaMessageReceived', [str]);

          if(obj['provider'] === "LocationManager"){
            var lat = obj["latitude"];
            var lon = obj["longitude"];
            var alt = obj["altitude"];
            var ts = obj["timestamp"];
          }
          if(obj['provider'] === "ErrorProvider"){
            var err = obj["message"];
            console.log("ERROR: "+err);
            if(err==="from_mock")
            {
              $rootScope.from_mock = true;
              $rootScope.$broadcast('mockupProvider',[]);
            }
            if(err==="no_extras"){
              $rootScope.no_extras = true;
              $rootScope.extra_sat_number = -1;
            }
          }
          str = "";
        }
        else {
          var temp_str = String.fromCharCode(view[i]);
          var str_esc = escape(temp_str);
          str += unescape(str_esc);
        }
      }
    }else{
    }
  };

  $rootScope.from_mock = false;
  $rootScope.no_extras = false;
  self.pluginKalmanPoints = [];
  self.pluginKalmanUnfilteredPoints = [];
  self.startNmeaCommunication = function(coordinatesNeedConversion){
    try {
      var platform = window.device.platform;
      var version = window.device.version.split('.')[0]+'.'+window.device.version.split('.')[1];

      try{
        if((platform === "Android" && parseFloat(version) < 7.0) || platform !== "Android"){
          $rootScope.noInternalNmea = true;
        }
        if(platform === "Android" && parseFloat(version) >= 7.0){
          $rootScope.noInternalNmea = false;
        }

        self.reloadMetadata();

      }catch(err){
        console.log(err);
      }

      $rootScope.externalData = false;

      if(!$rootScope.externalData){
        try{
          window.internalnmea.registerReadCallback(
            function success(data){
              self.readInternalNmeaStream(data, coordinatesNeedConversion);
            },
            function(err){
              console.log("ERROR ATTACHING CALLBACK - "+err);
            }
          );
          self.start_scheduler_satlist();
        }catch(err){
          console.log(err);
        }
      }
    }
    catch (err){
      $rootScope.$broadcast('deviceError', []);
    }
  };

  self.stopCommunication = function(){
    $interval.cancel(scheduler);
  };

  var adNormalityTest = (function () {

    var mean = function (arr) {

      var sum = 0;
      var length = arr.length;
      for (var i = 0; i < length; i++) {

        sum = sum + arr[i]
      }

      var mean = sum / length;
      return mean;

    };

    var variance = function (arr, std_type) {

      var sum = 0;
      var v = 0;
      var arr_length = arr.length;
      var this_mean = mean(arr);

      if (arr_length > 1) {
        for (var i = 0; i < arr_length; i++) {

          v = v + (arr[i] - this_mean) * (arr[i] - this_mean);
        }

        if (std_type == 's') {

          return v / (arr_length - 1);
        }
        else {

          return v / arr_length;
        }
      }
      else {

        return 0;
      }
    };

    var zFact = function (arr, mean, std) {

      var z = arr.map(function (d) {

        return ((d - mean) / std);
      });

      return z;
    };

    var pVal = function (norm, normSort) {

      var length = norm.length;
      var si = 0;
      var ad1 = 0;
      var ad2 = 0;
      var p1, p2, p3, p4, pval, test;

      for (var i = 0; i < length; i++) {

        si = si + (2 * (i + 1) - 1) * (Math.log(norm[i]) + Math.log(normSort[i]))
      }

      ad1 = -si / length - length;
      ad2 = ad1 * (1 + 0.75 / length + 2.25 / Math.pow(length, 2));


      if (ad2 < 13 && ad2 >= 0.6) {

        p1 = Math.exp(1.2937 - 5.709 * ad2 + 0.0186 * Math.pow(ad2, 2));
      }
      else {
        p1 = 0;
      }

      if (ad2 < 0.6 && ad2 >= 0.34) {

        p2 = Math.exp(0.9177 - 4.279 * ad2 - 1.38 * Math.pow(ad2, 2));
      }
      else {
        p2 = 0;
      }

      if (ad2 < 0.34 && ad2 >= 0.2) {

        p3 = 1 - Math.exp(-8.318 + 42.796 * ad2 - 59.938 * Math.pow(ad2, 2));
      }
      else {

        p3 = 0;
      }

      if (ad2 < 0.2 && ad2 >= 0.2) {

        p4 = 1 - Math.exp(-13.436 + 101.14 * ad2 - 223.73 * Math.pow(ad2, 2));
      }
      else {

        p4 = 0;
      }

      pval = Math.max(p1, p2, p3, p4);

      if (pval < 0.0005) {

        test = false
      }
      else {

        test = true
      }

      var normTest = { pValue: pval, test: test }
      return normTest
    };

    var normDist = function (arr) {

      var B1 = 0.319381530;
      var B2 = -0.356563782;
      var B3 = 1.781477937;
      var B4 = -1.821255978;
      var B5 = 1.330274429;
      var P = 0.2316419;
      var C = 0.39894228;

      var norm = arr.map(function (d) {

        if (d >= 0) {

          var t = (1.0 / (1.0 + P * d));
          return (1.0 - C * Math.exp(-d * d / 2.0) * t * (t * (t * (t * (t * B5 + B4) + B3) + B2) + B1));
        }
        else if (d < 0) {
          var t = (1.0 / (1.0 - P * d))

          return (C * Math.exp(-d * d / 2.0) * t * (t * (t * (t * (t * B5 + B4) + B3) + B2) + B1))
        }

      });

      return norm;
    };

    var stdSample = function (arr) {

      return Math.sqrt(variance(arr, 's'));

    };

    var stdPopulation = function (arr) {
      return Math.sqrt(variance(arr, 'p'));
    };

    var test = 'test change to master branch'
    var test2 = 'test2'

    var check = function (arr) {


      if (arr && arr.length >= 1) {

        var data = {};
        data.data = arr.sort(function (a, b) {
          return a - b;
        });
        data.mean = mean(data.data);
        data.stdP = stdPopulation(data.data, data.mean)
        data.stdS = stdSample(data.data, data.mean)
        data.zFact = zFact(data.data, data.mean, data.stdS);
        data.normDist = normDist(data.zFact);
        data.normDistInv = data.normDist.map(function (d) {
          return 1 - d;
        });
        data.normSort = data.normDistInv.sort(function (a, b) {
          return a - b;
        });
        data.normalTest = pVal(data.normDist, data.normSort)

        return {
          data: data.data,
          mean: data.mean,
          stdP: data.stdP,
          stdS: data.stdS,
          pValue: data.normalTest.pValue,
          normal: data.normalTest.test
        }

      }
      else {
        return;
      }

    };

    return {
      check: check
    }

  })();
};

usbService.$inject = ['$rootScope', '$interval', 'coordinatesService', 'sessionService', 'sqliteService', 'appService', 'httpService'];
angular.module('usb_module', []).service('usbService', usbService);
