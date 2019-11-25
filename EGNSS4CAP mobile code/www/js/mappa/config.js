'use strict';

var mappaConfig = function config($stateProvider) {
  $stateProvider

    .state('pictureMapOl', {
      url: '/pictureMap',
      cache: false,
      views: {
        'menuContent': {
          templateUrl: 'templates/mappa/pictureMapOl.html',
          controller: 'pictureMapOlCtrl'
        }
      }
    })

    .state('takePictureAR', {
      url: '/takePictureAR',
      params: {
        'startPointLat':null,
        'startPointLng':null,
        'reasons': null,
        'fakegpspos': null
      },
      views: {
        'menuContent': {
          templateUrl: 'templates/mappa/takePictureAR.html',
          controller: 'takePictureARCtrl',
        }
      },
      cache: false
    })

    .state('trackPathMapOl', {
      url: '/trackPathMapOl/:idlav/:codNazionale/:foglio/:authid/:idParticella/:particella',
      cache: true,
      views: {
        'menuContent': {
          templateUrl: 'templates/mappa/trackPathMapOl.html',
          controller: 'trackPathMapOlCtrl'
        }
      }
    })

};

mappaConfig.$inject = ['$stateProvider'];
angular.module('mappa.module').config(mappaConfig);
