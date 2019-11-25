var coordinatesService = function() {
  	var self = this;

	self.deg2num = function(lat_deg, lon_deg, zoom) {
	      lat_rad = this.radians(lat_deg);
	      n = Math.pow(2.0, zoom);
	      xtile = Math.floor((lon_deg + 180.0) / 360.0 * n);
	      ytile = Math.floor((1.0 - Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI) / 2.0 * n);
	      return [xtile, ytile];
	};

	self.num2deg = function(xtile, ytile, zoom) {
	  n = Math.pow(2.0, zoom);
	  lon_deg = xtile / n * 360.0 - 180.0;
	  lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * ytile / n)));
	  lat_deg = this.degrees(lat_rad);
	  return [lat_deg, lon_deg];
	};

	self.radians = function(degrees) {
	    return degrees * (Math.PI/180);
	};

	self.degrees = function(radians) {
	    return radians * (180/Math.PI);
	};

	self.from3857to30033004 = function(point, selected_authid){
        Proj4js.defs["EPSG:3003"] = "+proj=tmerc +lat_0=0 +lon_0=9 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs";
        Proj4js.defs["EPSG:3004"] = "+proj=tmerc +lat_0=0 +lon_0=15 +k=0.9996 +x_0=2520000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs";

        var dest;
        if(selected_authid  == "3003"){
            dest = new Proj4js.Proj('EPSG:3003');
        }else{
            dest = new Proj4js.Proj('EPSG:3004');
        }
        var source = new Proj4js.Proj('WGS84');
        var testPt = new Proj4js.Point(point.longitude, point.latitude);

        var test =  Proj4js.transform(source, dest, testPt);

        return test;

    };

	self.from30033004to3857 = function(point, selected_authid){
        Proj4js.defs["EPSG:3003"] = "+proj=tmerc +lat_0=0 +lon_0=9 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs";
        Proj4js.defs["EPSG:3004"] = "+proj=tmerc +lat_0=0 +lon_0=15 +k=0.9996 +x_0=2520000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs";

        var source;
        if(selected_authid  == "3003"){
            source = new Proj4js.Proj('EPSG:3003');
        }else{
            source = new Proj4js.Proj('EPSG:3004');
        }
        var dest = new Proj4js.Proj('WGS84');
        var testPt = new Proj4js.Point(point);

        var test =  Proj4js.transform(source, dest, testPt);

        return test;

    };

	self.fromWGS84to3857 = function(point){
    var parsed_point = [parseFloat(point.longitude), parseFloat(point.latitude)];
    var source = new Proj4js.Proj('WGS84');
    var dest = new Proj4js.Proj('GOOGLE');
    var testPt = new Proj4js.Point(parsed_point[0], parsed_point[1]);
    var test =  Proj4js.transform(source, dest, testPt);
	  return test;
  };

	self.from3857toWGS84 = function(point){
    var parsed_point = [parseFloat(point.longitude), parseFloat(point.latitude)];
    var source = new Proj4js.Proj('GOOGLE');
    var dest = new Proj4js.Proj('WGS84');
    var testPt = new Proj4js.Point(parsed_point[0], parsed_point[1]);
    var test =  Proj4js.transform(source, dest, testPt);
    return test;
  };

	self.transformGeom = function(selected_authid, geom, olgeom){
        Proj4js.defs["EPSG:3003"] = "+proj=tmerc +lat_0=0 +lon_0=9 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs";
        Proj4js.defs["EPSG:3004"] = "+proj=tmerc +lat_0=0 +lon_0=15 +k=0.9996 +x_0=2520000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs"


        var source;
        if(selected_authid  == "3003"){
            source = new Proj4js.Proj('EPSG:3003');
        }else{
            source = new Proj4js.Proj('EPSG:3004');
        }

        var dest = new Proj4js.Proj('WGS84');

        var poly = [];
        var tmp;
        for(var i=0; i < geom.exteriorRing.length; i++){
            tmp = geom.exteriorRing[i];

            var testPt = new Proj4js.Point(tmp.x, tmp.y);

            var test = Proj4js.transform(source, dest, testPt);

            if (olgeom){
                poly.push(ol.proj.fromLonLat([test.x, test.y]));
                //poly.push(ol.proj.fromLonLat([testPt.x, testPt.y]));
                //poly.push([tmp.x, tmp.y]);
            } else {
                poly.push(new plugin.google.maps.LatLng(testPt.y, testPt.x));
            }
        }

        if (olgeom){
            poly.push(poly[0]);
        }

        return poly;
	};

	self.wkbToGeom = function(wkb, wgs84, authId){
        var wkx = require('wkx');
        var buffer = require('buffer');

        var binary = atob(wkb);

        var array = new Uint8Array(binary.length);

        for( var i = 0; i < binary.length; i++ ) {
            array[i] = binary.charCodeAt(i);
        }

        var buf = new buffer.Buffer(array);

        var geom = wkx.Geometry.parse(buf);
        var real_geom;
        var polyCoords;
        if(geom.constructor.name == 'MultiPolygon'){
        	polyCoords = [];
        	for(var i = 0; i < geom.polygons.length; i++){
        		polyCoords.push(self.constructCoords(geom.polygons[i], wgs84, authId));
        	}
        	real_geom = new ol.geom.MultiPolygon(polyCoords);
        }else{
        	polyCoords = self.constructCoords(geom, wgs84, authId);
        	real_geom = new ol.geom.Polygon(polyCoords);
        }

        return real_geom;

    };

    self.geom30033004toWgs = function(geom, authId){
    	 var real_geom;
         var polyCoords;
         var parser = new jsts.io.OL3Parser();
         var jstsGeom = parser.read(geom);

         if(jstsGeom.constructor.name == 'MultiPolygon'){
         	polyCoords = [];
         	for(var i = 0; i < jstsGeom.polygons.length; i++){
         		polyCoords.push(self.constructCoords(jstsGeom.polygons[i], wgs84, authId));
         	}
         	real_geom = new ol.geom.MultiPolygon(polyCoords);
         }else{
         	polyCoords = self.constructCoords(jstsGeom, true, authId);
         	real_geom = new ol.geom.Polygon(polyCoords);
         }

         return real_geom;
    }

    self.geomWGSto30033004 = function(geom, authId){
    	var real_geom;
        var polyCoords;
        var parser = new jsts.io.OL3Parser();
        var jstsGeom = parser.read(geom);

        if(jstsGeom.constructor.name == 'MultiPolygon'){
        	polyCoords = [];
        	for(var i = 0; i < jstsGeom.polygons.length; i++){
        		polyCoords.push(self.constructCoords(jstsGeom.polygons[i], false, authId, true));
        	}
        	real_geom = new ol.geom.MultiPolygon(polyCoords);
        }else{
        	polyCoords = self.constructCoords(jstsGeom, true, authId, true);
        	real_geom = new ol.geom.Polygon(polyCoords);
        }

        return real_geom;
    }

    self.wkbToWkt =  function(wkb){
        var wkx = require('wkx');
        var buffer = require('buffer');

        var binary = atob(wkb);

        var array = new Uint8Array(binary.length);

        for( var i = 0; i < binary.length; i++ ) {
            array[i] = binary.charCodeAt(i);
        }

        var buf = new buffer.Buffer(array);

        var geom = wkx.Geometry.parse(buf);

        return geom.toWkt();

    };


    self.constructCoords = function(geom, wgs84, authId, reverse){
    	reverse = reverse || false;

    	Proj4js.defs["EPSG:3003"] = "+proj=tmerc +lat_0=0 +lon_0=9 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs";
        Proj4js.defs["EPSG:3004"] = "+proj=tmerc +lat_0=0 +lon_0=15 +k=0.9996 +x_0=2520000 +y_0=0 +ellps=intl +towgs84=-104.1,-49.1,-9.9,0.971,-2.917,0.714,-11.68 +units=m +no_defs"


    	var coords = [geom.exteriorRing].concat(geom.interiorRings);
        var realCoords = [];
        coords.forEach(function(ring){
            var ringToPush = [];
            ring.forEach(function(point){
            	if(wgs84){
            		var source;
            		var dest;
            		if(reverse){
            			if(authId  == "3003"){
                            dest = new Proj4js.Proj('EPSG:3003');
                        }else{
                            dest = new Proj4js.Proj('EPSG:3004');
                        }
                        source = new Proj4js.Proj('WGS84');
            		}else{
            			if(authId  == "3003"){
                            source = new Proj4js.Proj('EPSG:3003');
                        }else{
                            source = new Proj4js.Proj('EPSG:3004');
                        }
                        dest = new Proj4js.Proj('WGS84');
            		}

                    var test =  Proj4js.transform(source, dest, point);
                    ringToPush.push([test.x,test.y]);
            	}else{
            		ringToPush.push([point.x,point.y]);
            	}
            });
            realCoords.push(ringToPush);
        });

        return realCoords;
    }

	self.getAvaragedPoint = function(points){
        var latsum = 0;
        var lngsum = 0;
        for (var i = 0, len = points.length; i < len; i++) {
            latsum += points[i][0];
            lngsum += points[i][1];
        }
        return new ol.geom.Point([latsum/points.length, lngsum/points.length]);
    };

	self.GMapPolygonToWKT = function(poly) {
        var wkt = "POLYGON(";

            var path = poly.getPoints();
            wkt += "(";
            for(var j=0; j<path.length; j++)
            {
                wkt += path[j].lng.toString() +" "+ path[j].lat.toString() +",";
            }
            wkt += path[0].lng.toString() + " " + path[0].lat.toString() + "),";
        wkt = wkt.substring(0, wkt.length - 1) + ")";

        return wkt;
    };

	self.OLPolygonToWkt = function(poly){
        var format = new ol.format.WKT();
        return format.writeGeometry(poly);
    };

	self.WktToOLPolygon = function(wkt){
        var format = new ol.format.WKT();
        var geom_  = format.readGeometry(wkt);
        return geom_;
    };

	self.calcExtent = function(features){
        if (features.length > 0){
            var extent_trovata = features[0].getGeometry().getExtent();
            features.forEach(function(feat){
                var curr_ext = feat.getGeometry().getExtent();
                extent_trovata = [Math.min(extent_trovata[0], curr_ext[0]),
                    Math.min(extent_trovata[1], curr_ext[1]),
                    Math.max(extent_trovata[2], curr_ext[2]),
                    Math.max(extent_trovata[3], curr_ext[3])];
            });
            return extent_trovata;
        } else {
            return undefined;
        }
    }
};
coordinatesService.$inject=[];
angular.module('coordinates_services', []).service('coordinatesService', coordinatesService);
