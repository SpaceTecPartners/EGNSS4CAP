'use strict';

var sensorConfig =function config($stateProvider) {
  $stateProvider
    .state('sensoregps', {
      url: '/sensoregps',
      views: {
        'menuContent': {
          templateUrl: 'templates/sensoregps/sensoregps.html',
          controller: 'sensoregpsCtrl'
        }
      }
    })
};

sensorConfig.$inject = ['$stateProvider'];
angular.module('sensoregps.module')
  .config(sensorConfig);
