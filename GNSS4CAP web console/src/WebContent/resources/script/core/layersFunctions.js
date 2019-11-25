function highlightFeature2(feature){

	var nomeLayer = feature.nomeLayer;

	if(feature && creaLayers.getLayerByName(nomeLayer) && creaLayers.getLayerByName(nomeLayer).getVisible()){
		
		if(highlight2 && highlight2 === feature){
			
			featureOverlayHighlight.getSource().clear();
			featureOverlayHighlight.getSource().addFeature(feature);
		}
		
		if (feature !== highlight2) {
			if (highlight2) {
				featureOverlayHighlight.getSource().clear();
			}
			if (feature) {
				featureOverlayHighlight.getSource().addFeature(feature);
		    }
		    highlight2 = feature;
		}
		featureOverlayHighlight.setMap(map);
	}
}

function highlightPhoto(foto){
	var layers = map.getLayers().getArray();
	if(typeof layers[2] !== 'undefined'){
		var layerTmp = layers[2].getSource().getFeatures();
		layerTmp.forEach((f)=>{ 
			if(f.get("id") == foto.id){
				map.getView().fit(f.getGeometry().getExtent());
				map.getView().setZoom(20);
			}
		})	
	}
}
function shutDownFeature(){
	if (highlight2) {
		featureOverlayHighlight.getSource().clear();
	}
}
