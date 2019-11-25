var loginCtrl = function ($scope, $state, LoginService, $ionicSideMenuDelegate,
                  PopupFactory, $q, appService, sessionService, $filter) {

       	$scope.errorMessage = "";

       	$scope.password_shown = false;

       	$scope.user = {username:"", password:""};

       	$scope.togglePasswordVisibility = function(){
          var deferred = $q.defer();
       	  $scope.password_shown = !$scope.password_shown;
          var x = document.getElementById("password_input");
          if (x.type === "password") {
            x.type = "text";
          } else {
            x.type = "password";
          }
          deferred.resolve("");
          return deferred.promise;
        };

        $scope.$on('$ionicView.enter', function(){
          $("#mainBody").show();
          if(localStorage.getItem("logged") === "true"){
            if(sessionService.deserializeSession()){
              $state.go("lavorazioni");
            } else {
              $scope.showNoSessionPopup().finally(function(){
                deferred.resolve("");
              });
            }
          }
          else{
            var user = JSON.parse(localStorage.getItem("pre_compiled_user_info"));
            if (user !== null) {
              console.log("name::::::"+user.username);
              $scope.user.username = user.username;
            }
            $scope.user.password = "";
            $scope.errorMessage = "";
          }
        });

       	$scope.$on('$ionicView.enter', function() {
       		$ionicSideMenuDelegate.canDragContent(false);
        });

       	$scope.login = function(){
            var deferred = $q.defer();
            $scope.user.username = $scope.user.username.toLowerCase().trim();
            $scope.user.password = $scope.user.password.toLowerCase().trim();
            if($scope.user.username !==  "" && $scope.user.password !== ""){
              appService.show();
              LoginService.getRuolo($scope.user,
                  function success(token){
                      localStorage.setItem("pre_compiled_user_info", JSON.stringify($scope.user));
                      localStorage.setItem("user", JSON.stringify($scope.user));
                      localStorage.setItem("username", $scope.user.username);
                      localStorage.setItem("logged","true");
                      localStorage.setItem("token", token);
                      sessionService.openSession();
                      $state.go("lavorazioni");
                      appService.hide();
                      deferred.resolve("");
                  },
                  function failure(httpObj){
                    console.log(httpObj);
                    $scope.errorMessage = $filter('translate')('login.labels.wrongCredentials');
                    localStorage.setItem("logged", "false");
                    appService.hide();
                    deferred.resolve("");
                  });
            }else{
              $scope.showIncompleteFormPopup().then(function(){
                  deferred.resolve("");
              });
            }
            return deferred.promise;
       	};

       	$scope.showIncompleteFormPopup = function(){
            return PopupFactory.showPopup("login.popups.incompleteForm");
        };

       	$scope.showNoSessionPopup = function(){
       		PopupFactory.showPopup("login.popups.noSession");
        };

};

loginCtrl.$inject = ['$scope', '$state', 'LoginService', '$ionicSideMenuDelegate',
  'PopupFactory', '$q', 'appService', 'sessionService', '$filter'];
angular.module('login.module').controller('loginCtrl', loginCtrl);
