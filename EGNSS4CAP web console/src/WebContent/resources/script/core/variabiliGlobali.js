var imgPath = "resources/img";
var fotoMap = new Map();

var showInfoPopup = true;

var cryptoFoto=null;
var datiAlfanumerici = null;
var gestioneMappa = null;
var gestioneUtenti = null;
var creaLayers = null;

var popupAperto = false;

var map = null;

var codiRegi,epsg = null;

var LayerName = {	
		LAYER_ORTOFOTO:'ORTOFOTO',
		LAYER_CONO: 'CONO',
		LAYER_FOTO_POPUP: 'FOTO_POPUP'
};

var collection = new ol.Collection();

var fill = new ol.style.Fill({
	   color: 'rgba(255,255,255,0.4)'
	 });


var stroke = new ol.style.Stroke({
	color: [0, 153, 255, 1],
	width: 3
});

var styles = [new ol.style.Style({
		image: new ol.style.Circle({
			fill: fill,
			stroke: stroke,
			radius: 5
		}),
		fill: fill,
		stroke: stroke
	})
];

var featureOverlay2 = new ol.layer.Vector({
	  map: map,
	  source: new ol.source.Vector({
	    features: collection,
	    useSpatialIndex: false // optional, might improve performance
	  }),
	  style: styles,
	  updateWhileAnimating: true, // optional, for instant visual feedback
	  updateWhileInteracting: true // optional, for instant visual feedback
});

var collectionHighlight = new ol.Collection();
var featureOverlayHighlight = new ol.layer.Vector({
	  map: map,
	  source: new ol.source.Vector({
	    features: collectionHighlight,
	    useSpatialIndex: false // optional, might improve performance
	  }),
	  style: styles,
	  updateWhileAnimating: true, // optional, for instant visual feedback
	  updateWhileInteracting: true // optional, for instant visual feedback
});

var helpTooltip;
var featureOverlayCollection;
var featureOverlay;

var featureOverlayMeasureCollection;
var featureOverlayMeasure;

var highlight2;

var eventHandler = null;

var modify,draw,selectDelete,drag;

var datiAlfanumericiFullScreen=false;
var noDatiAlfanumerici = false;
var layoutWestWidthOld = 0;
var layoutEastWidthOld = 0;