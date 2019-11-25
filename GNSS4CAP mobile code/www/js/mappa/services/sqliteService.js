var sqliteService = function(baseLayersTypes, $ionicPlatform, $translate) {
  	var self = this;

  	self.db = null;
	self.default_minZoom = 1;
    self.default_maxZoom = 9;
    self.default_distanceFilter = 5;
    self.default_fakephotogps = 1;
    self.default_fakeTracking = 0;
    self.default_GPSprovider = 2;
    self.default_attivo = 0;
    self.default_tempo = 0;
    self.default_aggiustamento = 30;

    self.default_n_campionamenti = 20;
    self.default_range_oscillazione = 50;
    self.default_min_hdop = 3.0;

    self.default_min_n_sat = 7;
    self.default_min_snr = 31;
    self.default_min_speed = 1.0;
    self.default_wait = 10;
    self.default_min_fix = 3;
    self.default_kalman_accuracy = 1;
    self.default_kalman_speed = 0;
    self.default_avg_sampling_number = 20;
    self.default_centroid_computation_time = 30;

    self.default_use_gps = 1;
    self.default_use_glonass = 1;
    self.default_use_galileo = 1;

    self.default_support_galileo = false;
    self.default_support_dual_freq = false;
    self.default_support_egnos = false;
    self.default_support_osnma = false;

  	self.dropMetadataQuery = "drop table if exists metadata;";
    self.dropTilesQuery = "drop table if exists tiles;";
    self.dropLayerTypeQuery = "drop table if exists layertype;";

    self.createMetadataQuery = "CREATE TABLE IF NOT EXISTS metadata ("+
        "id_metadata integer primary key autoincrement not null, "+
        "name text not null unique, "+
        "value double not null,"+
        "start_date date default current_timestamp,"+
        "end_date date default null);";

    self.createLayerTypeQuery = "CREATE TABLE IF NOT EXISTS layertype (" +
        " id_layer integer primary key autoincrement not null, "+
        " descrizione text, "+
        " numb_of_servers integer, "+
        " tipo_layer integer, "+
        " url_completo_layer text not null unique," +
        " url_layer text not null unique, "+
        " start_date date default current_timestamp, "+
        " end_date date default null);";

    self.createTilesQuery = "CREATE TABLE IF NOT EXISTS tiles ("+
      "id_tiles integer primary key autoincrement not null,"+
      "id_layer_type text not null,"+
      "zoom_level integer not null,"+
      "tile_column integer not null,"+
      "tile_row integer not null,"+
      "tile_data text not null,"+
      "start_date date default current_timestamp,"+
      "end_date date default null);";

    self.createIndexTilesQuery = "CREATE INDEX index_tiles ON tiles (zoom_level, id_layer_type, tile_column, tile_row);" ;


    self.insertMetadataQuery = "INSERT INTO metadata (name, value) values (?,?);";

    self.insertDefaultTileLayerQuery = "INSERT INTO metadata (name, value) values ('default_tile_id', ?);";

    self.insertLayerType = "INSERT INTO layertype (url_layer, url_completo_layer, descrizione, numb_of_servers, tipo_layer) values (?,?,?,?,?);";

    self.openConnection = function(database_name){
		  self.db = window.sqlitePlugin.openDatabase({name: database_name, location: 0});//'default'});
	  };

    self.getDefaultLang = function(){
        var lingue = ["en-EN","it-IT"];
        if (lingue.indexOf(window.navigator.language) != -1) {
            $translate.preferredLanguage(window.navigator.language);
            $translate.use(window.navigator.language);
            //$translate.refresh();
            self.default_lang = window.navigator.language;
        }else {
            $translate.preferredLanguage('en-EN');
            $translate.use('en-EN');
            //$translate.refresh();
            self.default_lang = 'en-EN';
        }
    };

	self.populateDatabase = function(successCallback, errorCallback){

        var onSuccess = function(){
          console.log("statement succeded")
        };
        var onError = function(err, msg){
          console.log("statement error ",msg);
        };

        self.db.transaction(function(tx){

            //Drops
            tx.executeSql(self.dropMetadataQuery, null, onSuccess, onError);
            tx.executeSql(self.dropTilesQuery, null, onSuccess, onError);
            tx.executeSql(self.dropLayerTypeQuery, null, onSuccess, onError);

            //Creates
            tx.executeSql(self.createMetadataQuery, null, onSuccess, onError);
            tx.executeSql(self.createLayerTypeQuery, null, onSuccess, onError);

            tx.executeSql(self.createTilesQuery, null, onSuccess, onError);
            tx.executeSql(self.createIndexTilesQuery, null, onSuccess, onError);

            //Inserts
            tx.executeSql(self.insertMetadataQuery, ['minzoom', self.default_minZoom], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['maxzoom', self.default_maxZoom], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['distanceFilter', self.default_distanceFilter], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['fakeTracking', self.default_fakeTracking], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['fakephotogps', self.default_fakephotogps], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['GPSprovider', self.default_GPSprovider], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['language', self.default_lang], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['utilizzo_attivo', self.default_attivo], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['utilizzo_tempo', self.default_tempo], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['aggiustamento', self.default_aggiustamento], onSuccess, onError);

            tx.executeSql(self.insertMetadataQuery, ['n_campionamenti', self.default_n_campionamenti], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['oscillaz_cm', self.default_range_oscillazione], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['min_hdop', self.default_min_hdop], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['min_n_sat', self.default_min_n_sat], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['min_snr', self.default_min_snr], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['min_speed', self.default_min_speed], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['wait', self.default_wait], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['min_fix', self.default_min_fix], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['kalman_accuracy', self.default_kalman_accuracy], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['kalman_speed', self.default_kalman_speed], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['avg_sampling_number', self.default_avg_sampling_number], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['centroid_computation_time', self.default_centroid_computation_time], onSuccess, onError);

            tx.executeSql(self.insertMetadataQuery, ['use_gps', self.default_use_gps], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['use_glonass', self.default_use_glonass], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['use_galileo', self.default_use_galileo], onSuccess, onError);

            tx.executeSql(self.insertMetadataQuery, ['support_galileo', self.default_support_galileo], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['support_dual_freq', self.default_support_dual_freq], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['support_egnos', self.default_support_egnos], onSuccess, onError);
            tx.executeSql(self.insertMetadataQuery, ['support_osnma', self.default_support_osnma], onSuccess, onError);

            tx.executeSql(self.insertDefaultTileLayerQuery, [0], onSuccess, onError);

        }, function(error) {
          console.log('Transaction ERROR: ' + error.message);
          errorCallback();
        }, function() {
          console.log('Populated database OK');
          successCallback();
        });

	};

    self.doQuery = function (query, params, onSuccess, onError) {
        //$ionicPlatform.ready(function() {
            var result = [];
            self.db.transaction(function(tx) {
                tx.executeSql(query, params,
                    function(tx, res){
                        result = res;
                    },
                    function(tx, err){
                        result = [];
                        console.log("------------------------------------QUERY ERROR!",err);
                        onError(err);
                    });
            }, function(error) {
                console.log('Transaction ERROR: ' + error.message);
                onError(error);
            }, function() {
                console.log('TRANSACTION SUCCESS');
                onSuccess(result);
            });
        //});
    };

    self.deleteTilesQuery = "DELETE FROM tiles;";
    self.deleteTiles = function(successCallback, errorCallback) {

        self.db.transaction(function (tx) {
            tx.executeSql(self.deleteTilesQuery);
        }, function (error) {
            console.log('Transaction ERROR: ' + error.message);
            errorCallback();
        }, function () {
            console.log('Delete tiles success');
            successCallback();
        });
    };

    self.updateMetadataQuery = "UPDATE metadata set value = ? where name = ? and end_date is null;";
    self.updateMetadata = function(value, name, onSuccess, onError){
        self.doQuery(self.updateMetadataQuery, [value, name], onSuccess, onError);
    };

    self.getDefaultTileLayerIdQuery = "select value from metadata where name = 'default_tile_id';";
    self.getDefaultTileLayerId = function(onSuccess, onError){
		self.doQuery(self.getDefaultTileLayerIdQuery, null, onSuccess, onError);
	};

    /* deprecato! */
    self.getDefaultTileLayerQuery = "select * from layertype "+
        " where id_layer = (select value from metadata where name = 'default_tile_id') "+
        " and end_date is null;";
	self.getDefaultTileLayer = function(onSuccess, onError){
		self.doQuery(self.getDefaultTileLayerQuery, null, onSuccess, onError);
	};

    self.getTileQuery = "select tile_data  from tiles "+
        " where zoom_level = ? "+
        " and tile_column in (?) "+
        " and tile_row in (?) "+
        " and id_layer_type = (select value from metadata where name = 'default_tile_id') "+
        " and end_date is null;";
	self.existsTileData = function(zoom, x, y,  onSuccess, onError){
	    var success = function (res) {
	        onSuccess(res.rows.length);
	    };
	    self.doQuery(self.getTileQuery, [zoom, x, y], success, onError);
	};

	self.getTile = function (zoom, x, y, onSuccess, onError) {
	    var success = function (res) {
	        onSuccess(res);
	    };

	    self.doQuery(self.getTileQuery, [zoom, x, y], success, onError);
	};


    self.insertTileQuery = "INSERT INTO tiles (zoom_level, tile_row, tile_column, tile_data, id_layer_type) values "+
        "(?,?,?,?,(select value from metadata where name = 'default_tile_id'));";
	self.insertTileData = function(zoom, x, y,  tile, onSuccess, onError){
	    self.doQuery(self.insertTileQuery, [zoom,x,y,tile], onSuccess, onError);
	};

    self.countTilesScaricatiQuery = "select count(*) from tiles where zoom_level = ? and id_layer_type = ? "+
        " and tile_column >= ? and tile_column <= ?"+
        " and tile_row >= ? and tile_row <= ?";
    self.countTilesScaricati = function(nuova__v, zoom_level, layertype, min_col, max_col, min_row, max_row, onSuccess, onError){
        self.doQuery(self.countTilesScaricatiQuery, [zoom_level, layertype, min_col, max_col, min_row, max_row],
            function(res){onSuccess(res, nuova__v)}, onError);

    };

    /* deprecata */
    self.getLayersQuery = "SELECT id_layer, descrizione, url_layer, url_completo_layer, numb_of_servers FROM layertype"+
        " where end_date is null;";
	self.getLayers = function(onSuccess, onError) {
	    var success = function (res) {
	        var ret = [];
	        for (var i = 0; i < res.rows.length; i++) {
	            var row = res.rows.item(i);
	            ret.push({'id_layer': row['id_layer'],
	                      'descrizione': row['descrizione'],
	                      'url_layer': row['url_layer'],
	                      'url_completo_layer': row['url_completo_layer'],
	                      'numb_of_servers': row['numb_of_servers']});
	        }
	        onSuccess(ret);
	    };
	    self.doQuery(self.getLayersQuery, null, success, onError);
	};

    self.metadataQuery = "SELECT name, value FROM metadata where end_date is null;";
	self.getMetadata = function (onSuccess, onError) {
	    var success = function (res) {
	        var ret = {};
	        for (var i = 0; i < res.rows.length; i++) {
	            var row = res.rows.item(i);
	            ret[row['name']] = row['value'];
	        }
	        onSuccess(ret);
	    };
	    self.doQuery(self.metadataQuery, null, success, onError);
	};
};

sqliteService.$inject = ['baseLayersTypes', '$ionicPlatform', '$translate'];
angular.module('sqlite_services', []).service('sqliteService', sqliteService);
