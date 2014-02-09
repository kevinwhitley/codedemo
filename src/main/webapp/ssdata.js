
// ShopStyle data service for an angular app
function SSData(qq, http)
{
    // remember the angular services
    this._qq = qq;
    this._http = http;
    
    // retailer data, as returned from SS API - an array
    this._retailers = null;
    this._retailerIndex = null;  // retailers by id
    // brand data
    this._brands = null;
    this._brandIndex = null;  // brands by Id
    // color data from shopstyle
    this._colors = null;
    this._colorIndex = null;
    // women's categories
    this._wCategories = null;
    this._categoryIndex = null;
    // product data
    this._products = null;  // the list of products in the current query
    this._productIndex = {};  // this contains ALL the products we've seen
    
    // the currently chosen products
    this._chosenProducts = [];
}

//convenience function that returns a resolved promise
SSData.prototype.resolve = function(result)
{
    var deferred = this._qq.defer();
    deferred.resolve(result);
    return deferred.promise;
};

//get products of a given color
//theScope is the current $scope (from the controller)
// sortOrder is 0: relevance, 1: lo price to hi, 2: hi price to low
//returns a promise with all the retailers
SSData.prototype.requireProducts = function(theScope, category, filter, sortOrder)
{
    // for now, always fetch a new set from the server, even if just sorting
    if (true) {
        var self = this;
        var url = '/ss/products?category=' + category;
        if (filter) {
            url += '&' + filter;
        }
        return this._http.get(url).then(function(result){
            self._products = [];
            if (result.data.action == 'success') {
                angular.forEach(result.data.message, function(pp){
                    // if we already have this product, we want to keep existing object
                    var oldP = self._productIndex[pp.id];
                    if (!oldP) {
                        self._productIndex[pp.id] = pp;
                    }
                    else {
                        // use old object
                        pp = oldP;
                    }
                    self._products.push(pp);
                });
            }
            if (sortOrder === 1) {
                // sort low to hi
                self._products = self._products.sort(function(p0, p1){
                    return p0.price - p1.price;
                });
            }
            else if (sortOrder === 2) {
                // sort hi to low
                self._products = self._products.sort(function(p0, p1){
                    return p1.price - p0.price;
                });
            }
            return self._products;
        });
    }
    else {
        return this.resolve(this._products);
    }
};

//get all the retailers
//theScope is the current $scope (from the controller)
// returns a promise with all the retailers
SSData.prototype.requireRetailers = function(theScope)
{
  if (this._retailers === null) {
      var self = this;
      var url = '/ss/retailers';
      return this._http.get(url).then(function(result){
          self._retailers = [];
          self._retailerIndex = {};
          if (result.data.action == 'success') {
              angular.forEach(result.data.message, function(rr){
                  self._retailers.push(rr);
                  self._retailerIndex[rr.id] = rr;
              });
          }
          return self._retailers;
      });
  }
  else {
      return this.resolve(this._retailers);
  }
};

//get all the brands
//theScope is the current $scope (from the controller)
//returns a promise with all the brands
SSData.prototype.requireBrands = function(theScope)
{
    if (this._brands === null) {
        var self = this;
        var url = '/ss/brands';
        return this._http.get(url).then(function(result){
            self._brands = [];
            self._brandIndex = {};
            if (result.data.action == 'success') {
                angular.forEach(result.data.message, function(bb){
                    self._brands.push(bb);
                    self._brandIndex[bb.id] = bb;
                });
            }
            return self._brands;
        });
    }
    else {
        return this.resolve(this._brands);
    }
};

//get all the shopstyle colors
//theScope is the current $scope (from the controller)
//returns a promise with all the colors
SSData.prototype.requireColors = function(theScope)
{
  if (this._colors === null) {
      var self = this;
      var url = '/ss/colors';
      return this._http.get(url).then(function(result){
          self._colors = [];
          self._colorIndex = {};
          if (result.data.action == 'success') {
              angular.forEach(result.data.message, function(cc){
                  self._colors.push(cc);
                  self._colorIndex[cc.id] = cc;
              });
          }
          return self._colors;
      });
  }
  else {
      return this.resolve(this._colors);
  }
};

//get the shopstyle women's categories
//theScope is the current $scope (from the controller)
//returns a promise with the categories
SSData.prototype.requireWomensCategories = function(theScope)
{
    if (this._wCategories === null) {
        var self = this;
        var url = '/ss/wCategories';
        return this._http.get(url).then(function(result){
            self._wCategories = [];
            if (self._categoryIndex === null) {
                self._categoryIndex = {};
            }
            if (result.data.action == 'success') {
                angular.forEach(result.data.message, function(cc){
                    self._wCategories.push(cc);
                    self._categoryIndex[cc.id] = cc;
                });
            }
            return self._wCategories;
        });
    }
    else {
        return this.resolve(this._wCategories);
    }
};

SSData.prototype.getTwentyBrands = function(theScope)
{
    // just return the first 20 brands
    
    // we're assuming that this is called after requireBrands has returned
    // but just in case
    if (!this._brands || this._brands.length < 20) {
        return [];
    }
    
    var result = []
    var ii;
    for (ii=0; ii<20; ii++) {
        result.push(this._brands[ii]);
    }
    
    return result;
};

SSData.prototype.getProductById = function(id)
{
    return this._productIndex[id];
};

SSData.prototype.getBrandById = function(id)
{
    return this._brandIndex[id];
};

SSData.prototype.getRetailerById = function(id)
{
    return this._retailerIndex[id];
};

SSData.prototype.getColorById = function(id)
{
    return this._colorIndex[id];
};

SSData.prototype.getCategoryById = function(id)
{
    return this._categoryIndex[id];
};



