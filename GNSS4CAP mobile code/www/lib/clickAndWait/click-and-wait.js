'use strict';

angular.module('clickAndWait', []).directive('clickAndWait', function ($parse, $timeout) {
  return {
    // restrict: 'A',
    // scope: {
    //   asyncAction: '&clickAndWait'
    // },
    link: function link(scope, element, attr) {
      element.bind('click', function () {
        element.prop('disabled', true);
        $parse(attr.clickAndWait)(scope).finally(function () {
          // scope.$apply(function(){
          $timeout(function(){
            element.prop('disabled', false);
          });
          // element.prop('disabled', false);
          // scope.$apply(function () {
          //     element.prop('disabled', false);
          //   });
        });

      });
    }
  };
});
