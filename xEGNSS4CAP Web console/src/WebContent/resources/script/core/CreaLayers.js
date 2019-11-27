cryptoFoto = cryptoFoto || {};

cryptoFoto.creaLayers = function(){
	
};

cryptoFoto.creaLayers.prototype.addLayer = function (nomeLayer, layer, visible, otherMap)
{
	layer.nomeLayer=nomeLayer;
	
	if (otherMap){
		otherMap.addLayer(layer);
	}else{
		map.addLayer(layer);
	}
	
};

cryptoFoto.creaLayers.prototype.getLayerByName = function(nomeLayer, targetMap){
	
	var layerSel = null;
	if(targetMap){
		targetMap.getLayers().getArray().forEach(function(layer){
			
			if(layer.nomeLayer == nomeLayer)
				layerSel = layer;
		});
		return layerSel;
	}else{
		if(map){
			map.getLayers().getArray().forEach(function(layer){
		
				if(layer.nomeLayer == nomeLayer)
					layerSel = layer;
			});
			return layerSel;
		}
		else
			return null;
	}
	
};