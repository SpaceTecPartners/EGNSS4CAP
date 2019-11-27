'use strict';
var aboutConfig = function config($stateProvider) {
  $stateProvider
    .state('about', {
      url: '/about',
      views: {
        'menuContent': {
          templateUrl: 'templates/about/about.html',
          controller: 'aboutCtrl'
        }
      }
    })
};
aboutConfig.$inject = ['$stateProvider'];

angular.module('about.module')
  .config(aboutConfig);
