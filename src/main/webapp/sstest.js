
/* separate module for the data service  */
var dataModule = angular.module('ssDataModule', []);
dataModule.factory('ssdata', ['$q', '$http', function(qq, http) {
    return new SSData(qq, http);
}]);

/* separate module for the component directives  */
angular.module('ssDirectiveModule', []);

/* filters */
angular.module('ssFilters', []);

/* our main application module */
var myModule = angular.module('sstest', ['ngRoute', 'ssDataModule', 'ssDirectiveModule', 'ssFilters']);

myModule.config(['$routeProvider', function($routeProvider) {
  $routeProvider
      .when('/home', {templateUrl: 'home.html',   controller: HomeCtrl})
      .when('/browse', {templateUrl: 'browse.html',   controller: BrowseCtrl, reloadOnSearch: false})
      .when('/retailers', {templateUrl: 'retailers.html',   controller: RetailersCtrl})
      .when('/compare', {templateUrl: 'compare.html',   controller: CompareCtrl})
      .otherwise({redirectTo: '/home'});
}]);

