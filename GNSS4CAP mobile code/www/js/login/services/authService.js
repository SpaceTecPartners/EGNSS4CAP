var LoginService = function($http, $httpParamSerializerJQLike){
		var self = this;
		self.getRuolo = function(user, success, failure){
			try{
        $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
				$http.post(REMOTE_SERVER + "authServlet",
                $httpParamSerializerJQLike({"u": user.username, "p": user.password, "from_app":"1"}),
                {timeout:120000})
                .then(function(response){
                	console.log(response);
                  if(response.data.result === true){
                    success(response.data.token);
                  }else{
                    failure();
                  }
                },
                function(error){
                  console.log("failure "+error);
                  failure(error);
                });
			}catch(ex){
				console.log(ex);
				failure(ex, "error"+ex);
			}
		}
};

LoginService.$inject = ['$http', '$httpParamSerializerJQLike'];
angular.module('login.module').service('LoginService', LoginService);
