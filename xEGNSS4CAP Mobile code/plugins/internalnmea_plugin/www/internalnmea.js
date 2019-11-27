var internalnmea = {
	 registerReadCallback: function(successCallback, errorCallback, options) {
        cordova.exec(
            successCallback,
            errorCallback,
            'InternalNmea',
            'registerReadCallback',
            [options]
        );
    },

    stopNmea: function(successCallback, errorCallback){
        cordova.exec(
            successCallback,
            errorCallback,
            'InternalNmea',
            'stopNmea',
            []
        );
    }
};

module.exports = internalnmea;