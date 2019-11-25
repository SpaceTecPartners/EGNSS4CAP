var cell_info = {
	 getNetworkInfo: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'ErasmicoinNetworkInfo',
            'getNetworkInfo',
            []
        );
    }
};

module.exports = cell_info;