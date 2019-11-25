'use strict';
var opzConfig = function config($stateProvider) {
  $stateProvider
    .state('opzioni', {
      url: '/opzioni',
      views: {
        'menuContent': {
          templateUrl: 'templates/opzioni/opzioni.html',
          controller: 'opzioniCtrl'
        }
      }
    })
};

opzConfig.$inject = ['$stateProvider'];
angular.module('opzioni.module')
  .config(opzConfig);

