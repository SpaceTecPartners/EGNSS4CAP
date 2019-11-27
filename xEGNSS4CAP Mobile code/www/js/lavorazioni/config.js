'use strict';

var lavoConfig = function config($stateProvider) {
  $stateProvider
    .state('lavorazioni', {
      url: '/lavorazioni',
      views: {
        'menuContent': {
          templateUrl: 'templates/lavorazioni/lavorazioni.html',
          controller: 'lavorazioniCtrl'
        }
      }
    })
};


lavoConfig.$inject = ['$stateProvider'];
angular.module('lavorazioni.module')
  .config(lavoConfig);
