package ksw.shopstyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import ksw.kwutil.JSONReader;
import ksw.kwutil.JSONReader.JSONParseException;
import ksw.servlet.AppServlet;

public class ShopStyleData extends AppServlet.Application
{
    private List<Retailer> _retailers;
    private List<Brand> _brands;
    private List<Color> _colors;
    private List<Category> _womensCategories;
    
    private Map<String, List<Product>> _products;  // map of lists of products keyed by filter
    
    public ShopStyleData()
    {
        _retailers = null;
        _brands = null;
        _colors = null;
        _products = new HashMap<String, List<Product>>(100);
    }
    
    public List<Retailer> getRetailers()
    {
        if (_retailers == null) {
            // blocking!!
            fetchRetailers();
        }
        
        // should return immutable version!
        return _retailers;
    }
    
    public List<Brand> getBrands()
    {
        if (_brands == null) {
            // blocking!!
            fetchBrands();
        }
        
        // should return immutable version!
        return _brands;
    }
    
    public List<Color> getColors()
    {
        if (_colors == null) {
            // blocking!!
            fetchColors();
        }
        
        // should return immutable version!
        return _colors;
    }
    
    public List<Category> getWomensCategories()
    {
        if (_womensCategories == null) {
            // blocking!!
            fetchWomensCategories();
        }
        
        // should return immutable version!
        return _womensCategories;
    }
    
    // we always go to SS server to get products
    public List<Product> getProducts(String category, String filter)
    {
        List<Product> result = _products.get(category+filter);
        if (result == null) {
            result = fetchProducts(category, filter);
            if (_products.size() >= 100) {
                // pssht - should use an LRU
                _products.clear();
            }
            _products.put(category+filter, result);
        }
        
        return result;
    }
    
    // we always go to SS server to get products
    public List<Product> fetchProducts(String category, String filter)
    {
        ArrayList<Product> result = null;
        
        String parameters = "cat="+category+"&offset=0&limit=20";
        if (filter != null) {
            parameters += "&" + filter;
        }
        System.out.println("fetch products with " + parameters);
        String url = formatUrl("products", parameters);
        
        Map data = (Map)fetch(url);
        
        List<Map> products = (List<Map>)data.get("products");
        result = new ArrayList<Product>(products.size());
        for (Map pmap : products) {
            Product pp = Product.fromJSON(pmap);
            if (pp != null) {
                result.add(pp);
            }
            else {
                System.out.println("bad product in json");
            }
        }
        
        return result;
    }
    
    private void fetchRetailers()
    {
        String url = formatUrl("retailers", null);
        
        Map data = (Map)fetch(url);
        
        List<Map> retailers = (List<Map>)data.get("retailers");
        _retailers = new ArrayList<Retailer>(retailers.size());
        for (Map rmap : retailers) {
            Retailer rr = Retailer.fromJSON(rmap);
            if (rr != null) {
                _retailers.add(rr);
            }
            else {
                System.out.println("bad retailer in json");
            }
        }
    }

    private void fetchBrands()
    {
        String url = formatUrl("brands", null);
        
        Map data = (Map)fetch(url);
        
        List<Map> brands = (List<Map>)data.get("brands");
        _brands = new ArrayList<Brand>(brands.size());
        for (Map bmap : brands) {
            Brand bb = Brand.fromJSON(bmap);
            if (bb != null) {
                _brands.add(bb);
            }
            else {
                System.out.println("bad brand in json");
            }
        }
    }

    private void fetchColors()
    {
        String url = formatUrl("colors", null);
        
        Map data = (Map)fetch(url);
        
        List<Map> colors = (List<Map>)data.get("colors");
        _colors = new ArrayList<Color>(colors.size());
        for (Map cmap : colors) {
            Color cc = Color.fromJSON(cmap);
            if (colors != null) {
                _colors.add(cc);
            }
            else {
                System.out.println("bad color in json");
            }
        }
    }

    private void fetchWomensCategories()
    {
        // just get the top-level women's categories
        String url = formatUrl("categories", "depth=1&cat=womens-clothes");
        
        Map data = (Map)fetch(url);
        
        List<Map> cats = (List<Map>)data.get("categories");
        _womensCategories = new ArrayList<Category>(cats.size());
        for (Map cmap : cats) {
            Category cc = Category.fromJSON(cmap);
            if (cats != null) {
                _womensCategories.add(cc);
            }
            else {
                System.out.println("bad color in json");
            }
        }
    }

    private String formatUrl(String api, String parameters)
    {
        if (parameters != null) {
            return String.format("http://api.shopstyle.com/api/v2/%s?pid=uid2004-79498-32&%s", api, parameters);
        }
        else {
            return String.format("http://api.shopstyle.com/api/v2/%s?pid=uid2004-79498-32", api);
        }
    }

    // fetch from the url - read result and parse JSON
    private Object fetch(String url)
    {
        HttpMethodBase method = new GetMethod(url);
        
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setContentCharset("UTF-8");
        HttpConnectionManager connectM = new SimpleHttpConnectionManager(true);
        HttpClient client = new HttpClient(clientParams, connectM);
        
        int statusCode;
        try {
            statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                System.out.println("error: " + statusCode);
            }
            String result = method.getResponseBodyAsString();
            JSONReader jr = new JSONReader();
            Map data = (Map)jr.parse(result);
            return data;
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
}
