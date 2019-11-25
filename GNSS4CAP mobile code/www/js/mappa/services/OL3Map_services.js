var OL3Map_service = function () {

    var self = this;

    var OSMCenter = [0,0];

    self.getOSMView = function(zoom){
    	return new ol.View({
            center: OSMCenter,
            zoom: zoom,
            projection:  ol.proj.get("EPSG:3857"),
            maxZoom:19
        });
    };

    self.getGoogleMapsView = function(zoom){
      return new ol.View({
        center: OSMCenter,
        zoom: zoom,
        projection: ol.proj.get("EPSG:3857"),
        maxZoom:19
      });
    };

    self.createMap = function(id_widget, zoom, epsg, codiRegi, defaultTile){

        if(defaultTile.id == 'GOOGLE_MAPS'){
          return new ol.Map({
            controls:ol.control.defaults({
              attributionOptions:({}),
              attribution:false,
              rotate:false,
              zoom:false
            }).extend([
              new ol.control.ScaleLine()
            ]),
            view: self.getGoogleMapsView(zoom),
            target: id_widget
          });
        }

        if(defaultTile.id == 'OSM'){
        	return new ol.Map({
                controls:ol.control.defaults({
                            attributionOptions:({}),
                            attribution:false,
                            rotate:false,
                            zoom:false
                          }).extend([
                            new ol.control.ScaleLine()
                          ]),
                view: self.getOSMView(zoom),
                target: id_widget
            });
        }
    };

    self.addLayers = function(map, layers){
        layers.forEach(function(layer){
            map.addLayer(layer);
        });
    };

    self.removeLayers = function(map, layers){
        layers.forEach(function(layer){
            map.removeLayer(layer);
        });
    };

    self.removeAllLayers = function(map){
        map.getLayers().getArray().slice().forEach(function(layer){
            map.removeLayer(layer);
        });
    };

    self.zoomToBufferedExtent = function(map, extent, buffer){
        var extent_buffered = ol.extent.buffer(extent, buffer);
        map.updateSize();
        map.getView().fit(extent_buffered, map.getSize());
    };

    self.addOverlayPopup = function(map, container){
        var overlay = new ol.Overlay({
            element: container,
            autoPan: true,
            autoPanAnimation: {
                duration: 250
            }
        });

        map.addOverlay(overlay);


        return overlay;
    };

    self.getNewPKUID = function(layer){
        var pkuid = 0;
        layer.getSource().getFeatures().forEach(function(feature){
            var trovato = parseInt(feature.get("pkuid"));
            if (pkuid < trovato) {
                pkuid = trovato;
            }
        });
        return pkuid+1;
    };

};


angular.module('mappa.module').service('OL3Map_service', OL3Map_service);
