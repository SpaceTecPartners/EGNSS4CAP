var appService = function ($ionicLoading) {

        var self = this;

        self.show = function(templatem) {
            if (!templatem){
                templatem = 'Loading...';
            }
            $ionicLoading.show({
                template: templatem
            }).then(function(){});
        };

        self.hide = function(){
            $ionicLoading.hide().then(function(){});
        };

        self.safeApply = function(thiss, fn) {
            if(thiss.$$phase == '$apply' || thiss.$$phase == '$digest') {
                if(fn && (typeof(fn) === 'function')) {
                    fn();
                }
            } else {
                thiss.$apply(fn);
            }
        };

        self.getCurrentDate = function(){

            var today = new Date();
            var dd = today.getDate();
            var mm = today.getMonth()+1; //January is 0!
            var yyyy = today.getFullYear();
            var h = today.getHours();
            var m = today.getMinutes();
            var s = today.getSeconds();
            if(dd<10) { dd='0'+dd; }
            if(mm<10) { mm='0'+mm; }
            if(h<10) { h='0'+h; }
            if(m<10) { m='0'+m; }
            if(s<10) { s='0'+s; }

            today = dd+'/'+mm+'/'+yyyy+'-'+h + ":" + m + ":" + s;

            return today;

        };
    };

appService.$inject = ['$ionicLoading'];
angular.module('app').service('appService', appService);
