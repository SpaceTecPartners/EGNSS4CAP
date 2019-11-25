var app = angular.module('plunker', ['ng-mfb']);

app.controller('MainCtrl', function($scope) {
  $scope.name = 'World';
  $scope.menuState = 'closed';
});
