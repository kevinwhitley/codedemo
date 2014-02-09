package ksw.shopstyle;

import java.util.List;

import ksw.kwutil.JSONWriter;
import ksw.servlet.AppRequest;

public class ShopStyleApi
{
    // get products of a given color
    public static void productsAction(AppRequest request)
    {
        ShopStyleData ssData = (ShopStyleData)request.getApp();

        String category = request.getParameter("category");
        if (category == null) {
            // show dresses
            category = "dresses";
        }
        
        // pass through the filter parameters (clumsy!)
        String[] filters = request.getParameterValues("fl");
        String filter = null;
        if (filters != null && filters.length > 0) {
            filter = "fl=" + filters[0];
            for (int ii=1; ii<filters.length; ii++) {
                filter += "&fl=" + filters[ii];
            }
        }

        List<Product> products = ssData.getProducts(category, filter);
        
        JSONWriter jwriter = new JSONWriter();
        jwriter.startArray();
        for (Product pp : products) {
            jwriter.startObject();
            pp.toJSON(jwriter);
            jwriter.endObject();
        }
        jwriter.endArray();
        request.writeSuccessJSON(jwriter);
    }

    // get retailers
    public static void retailersAction(AppRequest request)
    {
        ShopStyleData ssData = (ShopStyleData)request.getApp();
        
        List<Retailer> retailers = ssData.getRetailers();
        
        JSONWriter jwriter = new JSONWriter();
        jwriter.startArray();
        for (Retailer rr : retailers) {
            jwriter.startObject();
            rr.toJSON(jwriter);
            jwriter.endObject();
        }
        jwriter.endArray();
        request.writeSuccessJSON(jwriter);
    }

    // get brands
    public static void brandsAction(AppRequest request)
    {
        ShopStyleData ssData = (ShopStyleData)request.getApp();
        
        List<Brand> brands = ssData.getBrands();
        
        JSONWriter jwriter = new JSONWriter();
        jwriter.startArray();
        for (Brand bb : brands) {
            jwriter.startObject();
            bb.toJSON(jwriter);
            jwriter.endObject();
        }
        jwriter.endArray();
        request.writeSuccessJSON(jwriter);
    }

    // get colors
    public static void colorsAction(AppRequest request)
    {
        ShopStyleData ssData = (ShopStyleData)request.getApp();
        
        List<Color> colors = ssData.getColors();
        
        JSONWriter jwriter = new JSONWriter();
        jwriter.startArray();
        for (Color cc : colors) {
            jwriter.startObject();
            cc.toJSON(jwriter);
            jwriter.endObject();
        }
        jwriter.endArray();
        request.writeSuccessJSON(jwriter);
    }

    // get women's categories
    public static void wCategoriesAction(AppRequest request)
    {
        ShopStyleData ssData = (ShopStyleData)request.getApp();
        
        List<Category> cats = ssData.getWomensCategories();
        
        JSONWriter jwriter = new JSONWriter();
        jwriter.startArray();
        for (Category cc : cats) {
            jwriter.startObject();
            cc.toJSON(jwriter);
            jwriter.endObject();
        }
        jwriter.endArray();
        request.writeSuccessJSON(jwriter);
    }
}
