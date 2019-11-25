var gnss_status = {
	 registerReadCallback: function(successCallback, errorCallback, options) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ErasmicoinGnssStatus',
            'registerReadCallback',
            [options]
        );
    },

    stopGnssStatus: function(successCallback, errorCallback){
        cordova.exec(
            successCallback,
            errorCallback,
            'ErasmicoinGnssStatus',
            'stopGnssStatus',
            []
        );
    }
};

module.exports = gnss_status;