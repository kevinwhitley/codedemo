angular.module('ssDirectiveModule')
  .directive('ssExpander', function() {
    return {
      link: function(scope, element, attrs) {
              // set up our local scope
              scope.showing = false;
          },
      restrict: 'E',
      scope: {
          header: '@header',
      },
      transclude: true,
      templateUrl: '/components/ssExpander.html'
    };
  });
