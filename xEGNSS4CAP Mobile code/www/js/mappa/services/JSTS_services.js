var JSTS_service = function () {

        var self = this;

        self.jsts_parser = new jsts.io.OL3Parser();
        self.jsts_geojson_reader = new jsts.io.GeoJSONReader();

        self.readListOfGeoms = function(list_of_geoms){
            var new_list = [];
            list_of_geoms.forEach(function(geom){
                new_list.push(self.jsts_parser.read(geom));
            });
            return new_list;
        };

        self.readGeom = function(geom){
            return self.jsts_parser.read(geom);
        };

        self.isGeomValid = function(geom){
            return self.readGeom(geom).isValid();
        };

        self.featContainsPoint = function(feature, coords){
            var geom = self.jsts_parser.read(feature.getGeometry());

            var point_xy = ol.proj.fromLonLat([ parseFloat(coords.longitude), parseFloat(coords.latitude)]);
            var curr_point = new ol.geom.Point(point_xy);
            var current_jsts_pos = self.jsts_parser.read(curr_point);

            return geom.contains(current_jsts_pos);
        };

        self.bufferFeature = function(feature, buffer){
            var geom_jsts = self.jsts_parser.read(feature.getGeometry()).buffer(buffer);
            var geom = self.jsts_parser.write(geom_jsts);
            feature.setGeometry(geom);
        };

        self.bufferGeom = function(geom, buffer, removeInitial){
            var geomJSTS = self.jsts_parser.read(geom);
            var geomBufferedJSTS = geomJSTS.buffer(buffer);
            if (removeInitial){
                geomBufferedJSTS = geomBufferedJSTS.difference(geomJSTS);
            }
            return self.jsts_parser.write(geomBufferedJSTS);
        };

        self.bufferLayerFeatures = function(layer, buffer){
            layer.getSource().getFeatures().forEach(function(feature){
                self.bufferFeature(feature, buffer);
            });
        };

        self.getInterscetingFeatures = function(feature, layer, minDistance){
            var percorse = [];

            var feature_jsts = self.jsts_parser.read(feature.getGeometry());

            layer.getSource().getFeatures().forEach(function(lf){

                var lf_jsts = self.jsts_parser.read(lf.getGeometry());

                if (self.intersectMinDist(feature_jsts, lf_jsts, minDistance)){
                    percorse.push({'nome': lf.get("Name")});
                }
            });
            return percorse;
        };

        self.intersectMinDist = function (sciata_jsts, pista_jsts, minDistance) {
            var toReturn = false;
            if (pista_jsts.intersects(sciata_jsts)) {
                var intersection = pista_jsts.intersection(sciata_jsts);
                toReturn = intersection.getLength() > minDistance;
            }
            return toReturn
        };

        self.getContainingFeatFromGeoJSON = function(json_features, point){
            var toReturn = null;

            var current_jsts_pos = self.readGeom(point);
            var result = self.jsts_geojson_reader.read(json_features);
            result.features.forEach(function(feature){
                if (feature.geometry.contains(current_jsts_pos)){
                    toReturn = feature;
                }
            });

            return toReturn;
        };

        self.getGeometryInfo = function(geometry){
            var geometryjsts = self.readGeom(geometry);
            return {
                'perimeter' : geometryjsts.getLength(),
                'area' : geometryjsts.getArea()
            }
        };

        self.cleanGeometry = function(f_to_c, features){
            var toSet = null;
            var world_jsts = self.readGeom(f_to_c.getGeometry());

            features.forEach(function(feat){
                var jsts_geom = self.readGeom(feat.getGeometry());
                if (toSet){

                    toSet = toSet.difference(jsts_geom);
                }
                else {
                    toSet = world_jsts.difference(jsts_geom);
                }
            });

            f_to_c.setGeometry(self.jsts_parser.write(toSet));

        }

        self.layerContainsGeom = function(layer, geom){
            var geom_jsts = self.readGeom(geom);
            var contiene = false;
            layer.getSource().getFeatures().forEach(function(feat){
                var l_feat = self.readGeom(feat.getGeometry())
                if (l_feat.contains(geom_jsts)) {
                    contiene = true;
                }
            });
            return contiene;

        }
    };


angular.module('mappa.module').service('JSTS_service', JSTS_service);