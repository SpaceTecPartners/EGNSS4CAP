var esciCtrl = function ($scope, $stateParams, $state, LogoutService) {
  $scope.$on('$ionicView.enter', function() {
    LogoutService.sendExitMessage(
      function(){
        localStorage.setItem("logged","false");
        localStorage.setItem("user", null);
        $state.go("login");
      },
      function(err){
        console.log(err);
        $state.go("home");
      }
    );

    })
};

esciCtrl.$inject = ['$scope', '$stateParams', '$state', 'LogoutService'];
angular.module('esci.module').controller('esciCtrl', esciCtrl);
