var PopupFactory = function($ionicPopup, $timeout, $filter) {
  return {
	  popupWithTemplate: function(trKey, templateUrl, $scope, cssClass){
		  cssClass = cssClass || "";
		  return $ionicPopup.show({
              templateUrl: templateUrl,
              title: $filter('translate')(trKey+".title"),
              scope: $scope,
              cssClass: "gnss-popup",
              buttons:[
                  {
                      text: $filter('translate')(trKey+".submitButton"),
                      type: "button button-energized popclose"
                  }]
          });
	  },

    popupWithTemplateNoButtons: function(trKey, templateUrl, $scope, cssClass){
      cssClass = cssClass || "";
      return $ionicPopup.show({
        templateUrl: templateUrl,
        title: $filter('translate')(trKey+".title"),
        scope: $scope,
        cssClass: "gnss-popup"
      });
    },

	  confirmPopup: function(trKey, confirmAction, cancelAction){

		  var confirmPopup = $ionicPopup.confirm({
			     title: $filter('translate')(trKey+".title"),
			     template: "<p style='text-align:center'>"+$filter('translate')(trKey+".subtitle")+"</p>",
			     okText: $filter('translate')(trKey+".submitButton"),
			     cancelText: $filter('translate')(trKey+".cancelButton")
			   });

			   confirmPopup.then(function(res) {
			     if(res) {
			       confirmAction();
			     } else {
			       cancelAction();
			     }
			   });
	  },
	  confirmPopupWVariable: function(trKey, variable, confirmAction, cancelAction){

		var confirmPopup = $ionicPopup.confirm({
			   title: $filter('translate')(trKey+".title"),
			   template: "<p style='text-align:center'>"+$filter('translate')(trKey+".subtitle").replace("%VAR%",variable)+"</p>",
			   okText: $filter('translate')(trKey+".submitButton"),
			   cancelText: $filter('translate')(trKey+".cancelButton")
			 });

			 confirmPopup.then(function(res) {
			   if(res) {
				 confirmAction();
			   } else {
				 cancelAction();
			   }
			 });
	},
	  showPopupPromise: function(trKey, pressed) {

		  var okButtonTxt =  $filter('translate')(trKey+".submitButton");
		  if(typeof okButtonTxt === 'undefined' || okButtonTxt == null || okButtonTxt == ""){
			  okButtonTxt = "Ok";
		  }
	      var alertPopup = $ionicPopup.alert({
	    	  title: $filter('translate')(trKey+".title"),
			  template: "<p style='text-align:center'>"+$filter('translate')(trKey+".subtitle")+"</p>",
			  buttons: [{ text: okButtonTxt }]
	      });
	      alertPopup.then(function(res) {
	        pressed();
	      });
	    },
    showPopup: function(trKey) {
    	var okButtonTxt =  $filter('translate')(trKey+".submitButton");
		  if(typeof okButtonTxt === 'undefined' || okButtonTxt == null || okButtonTxt == ""){
			  okButtonTxt = "Ok";
		  }
		  return $ionicPopup.alert({
			  title: $filter('translate')(trKey+".title"),
			  template: "<p style='text-align:center'>"+$filter('translate')(trKey+".subtitle")+"</p>",
			  buttons: [{ text: okButtonTxt }]
      });

    },
    showTimerPopup: function(trKey) {

      var alertPopup = $ionicPopup.alert({
    	  title: $filter('translate')(trKey+".title"),
		  template: "<p style='text-align:center'>"+$filter('translate')(trKey+".subtitle")+"</p>",
        buttons: []
      });
      alertPopup.then(function(res) {
        console.log('Tapped!');
      });
      $timeout(function() {
        alertPopup.close();
      }, 2000);
    }
  }
};

PopupFactory.$inject = ['$ionicPopup', '$timeout', '$filter'];

angular.module('mappa.module').factory('PopupFactory', PopupFactory);
