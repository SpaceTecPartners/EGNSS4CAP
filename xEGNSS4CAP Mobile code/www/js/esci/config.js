'use strict';
var esciConfig = function config($stateProvider) {
  $stateProvider
    .state('esci', {
      url: '/esci',
      views: {
        'menuContent': {
          templateUrl: 'templates/esci/esci.html',
          controller: 'esciCtrl'
        }
      }
    })
};
esciConfig.$inject = ['$stateProvider'];

angular.module('esci.module')
  .config(esciConfig);
