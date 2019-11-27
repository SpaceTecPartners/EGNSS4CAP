'use strict';

var appCtrl = function ($scope, $rootScope, $stateParams, $state, $ionicPlatform) {
  if (!window.indexedDB) {
      window.alert("La webview non supporta indexedDB, l'applicazione non sarà utilizzabile.");
  }
};

appCtrl.$inject = ['$scope', '$rootScope', '$stateParams', '$state', '$ionicPlatform'];
angular.module('app.module', ['ui.router']).controller('appCtrl', appCtrl);
