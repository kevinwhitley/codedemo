angular.module('ssFilters')
    .filter('clip25', function() {
        return function(value) {
            var ss = String(value);
            if (ss.length > 25) {
                return ss.substring(0, 25) + '...';
            }
            else {
                return ss;
            }
        }
    });
