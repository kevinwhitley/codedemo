angular.module('ssDirectiveModule')
  .directive('ssPageWrapper', function() {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: '/components/ssPageWrapper.html'
    };
  });
