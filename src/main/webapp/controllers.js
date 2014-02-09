function HomeCtrl($scope, ssdata)
{
    // start a fetch of the standard data, so that it is available ASAP
    ssdata.requireRetailers($scope);
    ssdata.requireBrands($scope);
    ssdata.requireColors($scope);
    ssdata.requireWomensCategories($scope);
}

function RetailersCtrl($scope, ssdata)
{
    $scope.retailers = null;
    
    // get required data
    ssdata.requireRetailers($scope).then(function(retailers){
        $scope.retailers = retailers;
    });
}

function CompareCtrl($scope, ssdata)
{
    ssdata.requireRetailers($scope).then(function(){
        $scope.chosenProducts = ssdata._chosenProducts;
    });
    $scope.showRetailer = function(product) {
        var br = ssdata.getRetailerById(product.retailerId);
        return br ? br.name : '';
    };
    $scope.showBrand = function(product) {
        var br = ssdata.getBrandById(product.brandId);
        return br ? br.name : '';
    };
}

function BrowseCtrl($scope, ssdata)
{
    $scope.products = null;
    $scope.bd = {  // browse data
        color: 'any',
        brand: 'any',
        category: '',
        sortOrder: 0,
    }
    $scope.browseCrumb = 'Anything';
    
    // range of the query
    var queryOffset = 0;
    var queryLimit = 30;
    
    // for display of large image
    $scope.largeImageUrl = null;
    
    // thumb display
    $scope.thumbProducts = ssdata._chosenProducts;
    
    // sort options
    $scope.sortOrderOptions = [
        {label: 'relevance', value: 0},
        {label: 'lo-hi', value: 1},
        {label: 'hi-lo', value: 2}
    ];
    $scope.bd.sortOrder = $scope.sortOrderOptions[0];
    
    // get required data
    ssdata.requireBrands($scope).then(function(){
        // once we've loaded all the brands, just get the first 10
        $scope.brands = ssdata.getTwentyBrands();
    });
    ssdata.requireColors($scope).then(function(colors){
        $scope.colors = colors;
    });
    
    // we don't want to query until the categories have loaded
    var canQuery = false;
    ssdata.requireWomensCategories($scope).then(function(categories){
        $scope.categories = categories;
        $scope.bd.category = 'dresses';
        canQuery = true;
    });
    
    // watch for changes in the browse options
    $scope.$watch('bd.color + "|" + bd.brand + "|" + bd.category + "|" + bd.sortOrder.value', function(){
        // update the crumb
        var colorName = '';
        var brandName = '';
        var categoryName = '';
        var filter = null;
        if ($scope.bd.color !== 'any') {
            colorName = ssdata.getColorById($scope.bd.color).name;
            filter = 'fl=c'+$scope.bd.color;
        }
        if ($scope.bd.brand !== 'any') {
            brandName = ssdata.getBrandById($scope.bd.brand).name;
            filter = (filter ? (filter+'&') : '') + 'fl=b'+$scope.bd.brand;
        }
        if ($scope.bd.category != '') {
            categoryName = ssdata.getCategoryById($scope.bd.category).name;
        }
        $scope.browseCrumb = colorName + ' ' + brandName + ' ' + categoryName;
        if (canQuery) {
            ssdata.requireProducts($scope, $scope.bd.category, filter, $scope.bd.sortOrder.value).then(function(products){
                $scope.products = products;
            });
        }
    });

    // to show and hide a large image
    $scope.onExpand = function(productId) {
        $scope.largeImageUrl = ssdata.getProductById(productId).largeUrl;
        
    };
    $scope.closeLarge = function() {
        $scope.largeImageUrl = null;
    };
    
    // to add or remove thumb
    $scope.onImageClick = function(productId) {
        var product = ssdata.getProductById(productId);
        if (product) {
            // make sure we don't try to add duplicate
            var ix = ssdata._chosenProducts.indexOf(product);
            if (ix < 0) {
                ssdata._chosenProducts.push(product);
            }
        }
    };
    $scope.thumbClick = function(thumbId) {
        // remove the product from the thumb list
        var product = ssdata.getProductById(thumbId);
        var ix = ssdata._chosenProducts.indexOf(product);
        if (ix >= 0) {
            ssdata._chosenProducts.splice(ix, 1);
        }
    };
    $scope.isChosen = function(product) {
        return ssdata._chosenProducts.indexOf(product) >= 0;
    };
}

