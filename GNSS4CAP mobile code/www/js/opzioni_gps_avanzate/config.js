'use strict';
var opz_gpsConfig = function config($stateProvider) {
  $stateProvider
    .state('opzioni_gps_avanzate', {
      url: '/opzioni_gps_avanzate',
      views: {
        'menuContent': {
          templateUrl: 'templates/opzioni_gps_avanzate/opzioni_gps_avanzate.html',
          controller: 'opzioniGpsAvanzateCtrl'
        }
      }
    })
};

opz_gpsConfig.$inject = ['$stateProvider'];
angular.module('opzioni_gps_avanzate.module')
  .config(opz_gpsConfig);
