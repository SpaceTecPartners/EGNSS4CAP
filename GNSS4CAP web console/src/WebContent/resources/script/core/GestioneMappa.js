cryptoFoto = cryptoFoto || {};

cryptoFoto.gestioneMappa = function() {

};

cryptoFoto.gestioneMappa.prototype.updateMapSize = function(newExtent) {

	$('#map').width($('.layout-east').width());
	map.updateSize();

	var nomeLayer = LayerName.LAYER_COMUNI;
	if (creaLayers.getLayerByName(LayerName.LAYER_PARTICELLE_GIS))
		nomeLayer = LayerName.LAYER_PARTICELLE_GIS;

	var extent = creaLayers.getLayerByName(nomeLayer).getSource().getExtent();
	if (!newExtent || ol.extent.containsExtent(newExtent, extent))
		newExtent = extent;

	map.getView().fit(newExtent, map.getSize());

};

cryptoFoto.gestioneMappa.prototype.riproporzionaMappa = function() {

	if (map) {
		var newExtent = map.getView().calculateExtent(map.getSize());
		this.updateMapSize(newExtent);
	}
};


cryptoFoto.gestioneMappa.prototype.fitMapToVectorLayers = function(targetMap){
	let extent;
	let mapToFit;
	if(targetMap){
		mapToFit = targetMap;
	}else{
		mapToFit = map;
	}
	for(let l of mapToFit.getLayers().getArray()){
		if(typeof l.getSource().getFeatures !== 'undefined'){
			for(let f of l.getSource().getFeatures()){
				let curr_ext = f.getGeometry().getExtent();
				if(!extent)
					extent = curr_ext;
					
				extent = [Math.min(extent[0], curr_ext[0]),
						Math.min(extent[1], curr_ext[1]),
						Math.max(extent[2], curr_ext[2]),
						Math.max(extent[3], curr_ext[3])];
			}
			mapToFit.getView().fit(extent, mapToFit.getSize());
		}
	}
	
}

cryptoFoto.gestioneMappa.prototype.fitMapToPhotos = function(targetMap){
	let extent;
	let mapToFit;
	if(targetMap){
		mapToFit = targetMap;
	}else{
		mapToFit = map;
	}
	//for(let f of mapToFit.getLayers().getArray()[2].getSource().getFeatures()){
	for(let f of creaLayers.getLayerByName(LayerName.LAYER_FOTO_POPUP,mapToFit).getSource().getFeatures()){
			let curr_ext = f.getGeometry().getExtent();
			if(!extent)
				extent = curr_ext;
				
			extent = [Math.min(extent[0], curr_ext[0]),
					Math.min(extent[1], curr_ext[1]),
					Math.max(extent[2], curr_ext[2]),
					Math.max(extent[3], curr_ext[3])];
	}
	mapToFit.getView().fit(extent);
}

cryptoFoto.gestioneMappa.prototype.visualizzaFoto = function(record, status, targetMap) {
	
	var coordinate = ol.proj.fromLonLat([ parseFloat(record.pointLng), parseFloat(record.pointLat) ],"EPSG:3857");

	var idx = record.id;
	
	let color;
	/*if(typeof status !== 'undefined'){
		if(!status.nmea){
			stroke = "rgba(128,0,0,1)";
			fill = "rgba(128,0,0,.8)";
		}else if(!status.location){
			stroke = "rgba(180,180,180,1)";
			fill = "rgba(180,180,180,.8)";
		}else{
			stroke ="rgba(0,128,0,1)";
			fill = "rgba(0,128,0,.8)";
		}
	}else{
		stroke = "rgba(0,0,0,.9)";
		fill   = "rgba(0,0,0,.7)";
	}*/
	
	if(status.compStatus){
		stroke ="rgba(0,128,0,1)";
		fill = "rgba(0,128,0,.8)";
	}else{
		stroke = "rgba(128,0,0,1)";
		fill = "rgba(128,0,0,.8)";
	}
	

	if(!isNaN(coordinate[0]) && !isNaN(coordinate[1])){
		this.getPhotoLayer(record.uri_photo, idx, coordinate, stroke, fill, targetMap);
		this.getConoLayer(record.heading, idx, coordinate, stroke, fill, targetMap);
	}

	removeOverlay();
};

cryptoFoto.gestioneMappa.prototype.getPhotoLayer = function(uri_photo, idx,
		coordinate, stroke, fill, targetMap) {

	var _this = this;

	var canvas = document.createElement('canvas');
	canvas.height = 300;
	canvas.width = 150;
	var ctx = canvas.getContext("2d");
	var img = new Image();
	img.src = uri_photo;
	
	img.onload = function() {
		var factor = img.width / img.height;
		var imgnewwidth;
		var imgnewheight;
		var startonx;
		var startony;
		var strokewidth = 5;
		
		if(factor >= 1){ //orizzontale
			canvas.height = 150;
			canvas.width = 300;
			imgnewwidth = canvas.width/2; //(canvas.width / 2) * factor;
			imgnewheight = canvas.height/2; //(canvas.height / 2) * factor;
			startonx = ((canvas.width / 2) - (imgnewwidth / 2));
			startony = 10;
		}else{ //verticale
			imgnewwidth = (canvas.width / 2) * factor;
			imgnewheight = (canvas.height / 2) * factor;
			startonx = ((canvas.width / 2) - (imgnewwidth / 2));
			startony = ((canvas.height / 2) - (imgnewheight / 2)); //startony = 10;
		}
		
		ctx.fillStyle = fill;
		ctx.strokeStyle = stroke;
		ctx.fillRect(startonx - strokewidth, startony - strokewidth,
				imgnewwidth + (strokewidth * 2), imgnewheight
						+ (strokewidth * 2));
		ctx.drawImage(img, startonx, startony, imgnewwidth, imgnewheight);
		ctx.lineWidth = "" + strokewidth;
		ctx.rect(startonx - strokewidth, startony - strokewidth, imgnewwidth
				+ (strokewidth * 2), imgnewheight + (strokewidth * 2));
		ctx.stroke();
		ctx.fillRect(canvas.width / 2 - (strokewidth / 2), imgnewheight
				+ (startony + strokewidth), strokewidth, (canvas.height
				- imgnewheight - (startony + strokewidth)));
		
		_this.creaLayerInfoFoto(idx, coordinate, LayerName.LAYER_FOTO_POPUP,
				canvas.toDataURL(), targetMap);
	};

};

cryptoFoto.gestioneMappa.prototype.getConoLayer = function(heading, idx,
		coordinate, stroke, fill, targetMap) {

	var canvas = document.createElement("canvas");
	var ctx = canvas.getContext("2d");
	canvas.height = 300;
	canvas.width = 300;

	var radiansConst = 0.01745329252;

	var headInRadians = heading * radiansConst;
	var northDegrees = 270;
	var north = northDegrees * radiansConst;

	var arcStartDegrees = northDegrees + heading;

	if (arcStartDegrees > 360) {
		arcStartDegrees = arcStartDegrees - 360;
	}

	var coneStartD = arcStartDegrees - 28;
	var coneEndD = arcStartDegrees + 28;

	var coneStartR = coneStartD * radiansConst;
	var coneEndR = coneEndD * radiansConst;

	var arcEnd = (northDegrees + heading) * radiansConst;

	ctx.lineWidth = 4;

	ctx.strokeStyle = stroke;
	ctx.fillStyle = fill;

	ctx.beginPath();
	ctx.moveTo(150, 150);
	ctx.arc(150, 150, 145, coneStartR, coneEndR);
	ctx.lineTo(150, 150);
	ctx.stroke();
	ctx.fill();

	this.creaLayerInfoFoto(idx, coordinate, LayerName.LAYER_CONO, canvas
			.toDataURL(), targetMap);

};

cryptoFoto.gestioneMappa.prototype.creaLayerInfoFoto = function(idx,
		coordinate, nomeLayer, imgSrc, targetMap) {

	var feature = new ol.Feature(new ol.geom.Point(coordinate));
	feature.idx = idx;
	feature.imgSrc = imgSrc;
	feature.nomeLayer = nomeLayer;
	feature.set("id",idx);
	var features = [feature];
	if(nomeLayer == LayerName.LAYER_FOTO_POPUP){
		var buffer = new ol.Feature(new ol.geom.Circle(coordinate, 150)); //150m buffer on photo marker
		features.push(buffer);
	}
	if ((targetMap &&  !creaLayers.getLayerByName(nomeLayer, targetMap)) || (map && !creaLayers.getLayerByName(nomeLayer))) {

		var layer = new ol.layer.Vector({
			source : new ol.source.Vector({
				features : features
			})
		});
		layer.set("nomeLayer",nomeLayer);
		layer
				.setStyle(function style(feature, resolution) {

					if (feature.nomeLayer == LayerName.LAYER_CONO) {
						return new ol.style.Style(
								{
									image : new ol.style.Icon(
											{
												src : feature.imgSrc,
												anchor : [ 0.5, 0.5 ],
												anchorXUnits : 'fraction',
												anchorYUnits : 'fraction',
												opacity : 1,
												rotateWithView : true,
												scale : (resolution > 0
														&& resolution < 0.5 ? 0.6
														: 0.2)
											})
								});
					} else if (feature.nomeLayer == LayerName.LAYER_FOTO_POPUP)  {

						return new ol.style.Style(
								{
									image : new ol.style.Icon(
											{
												src : feature.imgSrc,
												anchor : [ 0.5, 1 ],
												anchorXUnits : 'fraction',
												anchorYUnits : 'fraction',
												opacity : 1,
												scale : (resolution > 0
														&& resolution < 0.5 ? 0.8
														: 0.4)
											})
								});
					}
				});

		creaLayers.addLayer(nomeLayer, layer, true, targetMap);
	} else {
		creaLayers.getLayerByName(nomeLayer, (targetMap ? targetMap : map)).getSource().addFeature(feature);
	}
};

cryptoFoto.gestioneMappa.prototype.clearPhotoLayer = function(){
	
	let coneLayer = creaLayers.getLayerByName(LayerName.LAYER_CONO);
	if(typeof coneLayer !== 'undefined' && coneLayer != null){
		coneLayer.getSource().clear();
	}
	let fotoLayer = creaLayers.getLayerByName(LayerName.LAYER_FOTO_POPUP);
	if(typeof fotoLayer !== 'undefined' && fotoLayer != null){
		fotoLayer.getSource().clear();
	}
}

cryptoFoto.gestioneMappa.prototype.creaMappa = function(arrayPoligoni, id, controlliAttivi) {
	this.creaDivMappa();

	var overlay = null;

	if (arrayPoligoni != null) {
		var controls = ol.control.defaults({
			zoom : controlliAttivi,
			rotate : controlliAttivi,
			attribution : controlliAttivi
		});

		var interactions = ol.interaction.defaults({

			doubleClickZoom : false,
			keyboard : controlliAttivi,
			pinchRotate : controlliAttivi,
			dragAndDrop : controlliAttivi,
			dragAndDropEvent : controlliAttivi,
			dragPan : controlliAttivi,
			mouseWheelZoom : controlliAttivi
		});

		if (controlliAttivi) {

			var container = document.getElementById('popup');
			var closer = document.getElementById('popup-closer');

			closer.onclick = function() {

				$('#popup').fadeOut("slow");
				closer.blur();
				popupAperto = false;
				return false;
			};
			/**
			 * Create an overlay to anchor the popup to the map.
			 */
			overlay = new ol.Overlay({
				element : container,
				id : 1
			});

			if (map != null) {

				map.removeOverlay(map.getOverlayById(1));
				map.addOverlay(overlay);

				// if(map.getSize()[0]==0)
				// this.updateMapSize();
				//				
				map.removeOverlay(map.getOverlayById(1));
				map.addOverlay(overlay);
				eventHandler.removeListener(eventHandler.get('mapClick'));
				eventHandler.set('mapClick', eventHandler.addListener(map,
						'click', function(evt) {

							if (showInfoPopup)
								mapClick(null, overlay, evt.coordinate, evt);
						}, false));
				eventHandler.set('mapDblClick', eventHandler.addListener(map,
						'dblclick', function(evt) {

							if (showInfoPopup)
								mapDblClick(null, overlay, evt.coordinate, evt);
						}, false));
			}
		}

		var projection = ol.proj.get("EPSG:3857"); 

		var ortofoto = new ol.layer.Tile({
			zIndex : 0,
			title:"ortofoto",
			nomeLayer: LayerName.LAYER_ORTOFOTO,
			source: new ol.source.OSM({attributions:false})
		});
			
		if (controlliAttivi && map == null) { 

			map = new ol.Map({

				logo : false,
				controls : controls,
				interactions : interactions,
				layers : [ ortofoto ],
				target : document.getElementById('map'),
				overlays : [ overlay ],
				view : new ol.View({
					projection : projection,
					zoom: 6,
					center : [995585, 6247045]//extentPoligoni
					//center: [12 , 41]
				})
			});

			map.addControl(new ol.control.OverviewMap());

			this.setStyleMap(map);


			eventHandler.set('mapClick', eventHandler.addListener(map, 'click',
					function(evt) {

						if (showInfoPopup)
							mapClick(null, overlay, evt.coordinate, evt);
					}, false));

		}
	}
};

cryptoFoto.gestioneMappa.prototype.creaDivMappa = function() {

	var divPopup = '<div id="popup" class="ol-popup">';
	divPopup += '<a href="#" id="popup-closer" class="ol-popup-closer"></a>';
	divPopup += '<div id="popup-content"></div>';
	divPopup += '</div>';
	$('#popup').remove();
	$('#map').append(divPopup);

	$('#mapContainer').css('width', $('#ui-layout-center').css('width'));

	$('#mapContainerMappa').show();

};

cryptoFoto.gestioneMappa.prototype.setStyleMap = function(map) {

	featureOverlayCollection = new ol.Collection();
	featureOverlay = new ol.layer.Vector({
		map : map,
		source : new ol.source.Vector({
			features : collection,
			useSpatialIndex : false
		// optional, might improve performance
		}),
		style : styles,
		updateWhileAnimating : true, // optional, for instant visual feedback
		updateWhileInteracting : true
	// optional, for instant visual feedback
	});

	var highlight = null;
	var displayFeatureInfo = function(pixel) {

		var feature = map.forEachFeatureAtPixel(pixel,
				function(feature, layer) {
					if (!feature.noPopup)
						return feature;
				});

		if (feature !== highlight) {
			if (highlight) {
				featureOverlay.getSource().clear();
			}
			if (feature && !popupAperto) {
				featureOverlay.getSource().addFeature(feature);
			}
			highlight = feature;
		}
	};

	var _this = this;

	$(map.getViewport()).on('mousemove', function(evt) {

		var pixel = map.getEventPixel(evt.originalEvent);

		displayFeatureInfo(pixel);
	}).hover(function(event) {

		var pixel = map.getEventPixel(event.originalEvent);

		displayFeatureInfo(pixel);
	}, function(event) {

		if (highlight) {
			featureOverlay.getSource().clear();
			highlight = null;
		}
	});
	
};

function mapDblClick(feature, overlay, coordinate, evt) {
	console.log(evt);
}

function mapClick(feature, overlay, coordinate, evt) {

	var container = document.getElementById('popup');
	var content = document.getElementById('popup-content');
	var closer = document.getElementById('popup-closer');
	var pixel = null;
	closer.onclick = function() {

		$('#popup').fadeOut("slow");
		closer.blur();
		popupAperto = false;
		return false;
	};

	if (!overlay) {

		overlay = map.getOverlayById(1);
	}

	if (showInfoPopup) {

		pixel = map.getPixelFromCoordinate(coordinate);

		if (!feature) {

			feature = map
					.forEachFeatureAtPixel(
							pixel,
							function(feature, layer) {
								if (feature.nomeLayer == LayerName.LAYER_FOTO_POPUP
										|| feature.nomeLayer == "CONO") {
									return feature;
								} else {
									return null;
								}
							});
		}

		if (feature) {
			window.setTimeout(function() {

				let id = feature.get('id');
				gestioneFoto.showPhoto(id);
				
			}, 400);
		}
	}
}
