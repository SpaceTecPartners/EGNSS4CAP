var sessionService = function () {

    var self = this;
    self.session = {};

    /* COSTANTI */
    self.NAVIGATE_SESSION = 0;
    self.TRACK_SESSION = 1;
    self.PICTURE_SESSION = 2;
    self.FC_SESSION = 3;

    self.openSession = function(){
    	if(typeof self.session.session_id === 'undefined'){
    		self.session = {};
    		self.session.session_id = Date.now();
    		self.serializeCurrentSession();
    	}else{

    	}
    };

    self.getCodiRegi = function(){
    	return (typeof self.session.datiFoglio !== 'undefined' ? ((""+self.session.datiFoglio.codiRegi).length < 2 ? "0"+self.session.datiFoglio.codiRegi : self.session.datiFoglio.codiRegi) : null);
    };

    self.getAuthId = function(){
    	return (typeof self.session.datiFoglio !== 'undefined' ? self.session.datiFoglio.authid : null);
    };

    self.getIdLav = function(){
    	return self.session.datiLavorazione.idLav;
    };

    self.pauseTrackingSession = function(trackingData){
    	self.trackingData = {
    			'layerTracks' : trackingData.layerTracks,
    			'layerVerticiTracks' : trackingData.layerVerticiTracks
    	}
    };

    self.getAnyOpenTrackingSession = function(){
    	if(typeof self.trackingData !== 'undefined' && self.trackingData != null){
    		return self.trackingData;
    	}else{
    		return null;
    	}
    };

    self.clearTrackingPauseDate = function(){
    	self.trackingData = null;
    };

    self.setSessionLoaded = function(){
        if (self.session){
            self.session.loaded = true;
        }
    };

    self.removeSession = function(){
        self.session = {};
    };

    self.startAndEndSetted = function(){
        return self.session &&
            (typeof self.session.startPointLat !== 'undefined' && self.session.startPointLat  !== null) &&
            (typeof self.session.startPointLng !== 'undefined' && self.session.startPointLng  !== null) &&
            (typeof self.session.endPointLat !== 'undefined' && self.session.endPointLat    !== null) &&
            (typeof self.session.endPointLng !== 'undefined' && self.session.endPointLng    !== null);
    };

    self.serializeCurrentSession = function(){
    	localStorage.setItem("SESSION",JSON.stringify(self.session));
    };

    self.deserializeSession = function(){
    	if(typeof localStorage.getItem("SESSION") !== 'undefined' && localStorage.getItem("SESSION") != null){
    		self.session = JSON.parse(localStorage.getItem("SESSION"));
    		if(typeof self.session.session_id !== 'undefined'){
    			self.session.loaded = false;
    			return true;
    		}else{
    			return false;
    		}
    	}else{
    		return false;
    	}
    }
};

angular.module('mappa.module').service('sessionService', sessionService);
