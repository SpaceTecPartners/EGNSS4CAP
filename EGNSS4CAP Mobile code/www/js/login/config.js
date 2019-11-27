'use strict';

var configLogin = function config($stateProvider) {
  $stateProvider
    .state('login', {
      url: '/login',
      views: {
        'menuContent': {
          templateUrl: 'templates/login/login.html',
          controller: 'loginCtrl'
        }
      }
    })
};
configLogin.$inject = ['$stateProvider'];
angular.module('login.module').config(configLogin);