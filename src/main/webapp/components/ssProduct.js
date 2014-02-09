angular.module('ssDirectiveModule')
  .directive('ssProduct', function() {
    return {
      link: function(scope, element, attrs) {
          scope.expandClick = function(id) {
              if (scope.onexpand) {
                  // pass object with keys corresponding to names of variables in the parent html
                  scope.onexpand({productId: id});
              }
          };
          scope.imageClick = function(id) {
              if (scope.onimageclick) {
                  scope.onimageclick({productId: id});
              }
          };
          scope.cellClass = function() {
              return scope.chosen ? ['productCell', 'productCellChosen'] : 'productCell';
          }
      },
      restrict: 'E',
      scope: {
        onexpand: '&onexpand',
        onimageclick: '&onimageclick',
        product: '=product',
        chosen: '=chosen'
      },
      templateUrl: '/components/ssProduct.html'
    };
  });
