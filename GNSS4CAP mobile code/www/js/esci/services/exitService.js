var LogoutService = function($http, $httpParamSerializerJQLike){
  var self = this;
  self.sendExitMessage = function (success, failure) {
    try{
      $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
      $http.post(REMOTE_SERVER + "LogoutServletAuth",
        $httpParamSerializerJQLike({"u":localStorage.getItem("username")}),
        {timeout:120000})
        .then(function (response) {
            console.log(response);
            if(response.data.result === true){
              success();
            }else{
              failure("failure returned false");
            }
          }
          ,function(error){
            console.log("failure "+error);
            failure(error);
          });
    }catch(err){
      console.log(err);
      failure(err);
    }
  }
};

LogoutService.$inject = ['$http', '$httpParamSerializerJQLike'];
angular.module('esci.module').service('LogoutService', LogoutService);
