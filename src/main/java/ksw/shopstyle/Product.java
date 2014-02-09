package ksw.shopstyle;

import java.util.Map;

import ksw.kwutil.JSONWriter;

public class Product
{
    private Integer _id;
    private String _name;
    private Double _price;
    private String _priceLabel;
    private String _brandId;
    private String _retailerId;
    private String _description;
    private String _url;
    private String _imageUrl;
    private String _thumbUrl;
    private String _largeUrl;
    private boolean _inStock;
    
    private static final String IdField = "id";
    private static final String NameField = "name";
    private static final String PriceField = "price";
    private static final String PriceLabelField = "priceLabel";
    private static final String BrandIdField = "brandId";
    private static final String RetailerIdField = "retailerId";
    private static final String DescriptionField = "description";
    private static final String UrlField = "url";
    private static final String ImageField = "imageUrl";
    private static final String ThumbImageField = "thumbUrl";
    private static final String LargeImageField = "largeUrl";
    private static final String InStockField = "inStock";
    
    public static Product fromJSON(Map pj)
    {
        Integer id = (Integer)pj.get(IdField);
        String name = (String)pj.get(NameField);
        
        // get image urls (sized to 112x140)
        String imageUrl = null;
        String thumbUrl = null;
        String largeUrl = null;
        Map imageM = (Map)pj.get("image");
        if (imageM != null) {
            Map sizeM = (Map)imageM.get("sizes");
            if (sizeM != null) {
                Map im = (Map)sizeM.get("Medium");
                if (im != null) {
                    imageUrl = (String)im.get("url");
                }
                im = (Map)sizeM.get("Small");
                if (im != null) {
                    thumbUrl = (String)im.get("url");
                }
                im = (Map)sizeM.get("Original");
                if (im != null) {
                    largeUrl = (String)im.get("url");
                }
            }
        }

        // try to get the retailer id
        String retailerId = null;
        Map retailerM = (Map)pj.get("retailer");
        if (retailerM != null) {
            retailerId = (String)retailerM.get("id");
        }
        // try to get the brand id
        String brandId = null;
        Map brandM = (Map)pj.get("brand");
        if (brandM != null) {
            brandId = (String)brandM.get("id");
        }
        if (id != null && name != null && imageUrl != null) {
            Product prod = new Product(id, name, imageUrl);
            Object price = pj.get(PriceField);
            prod._price = (price instanceof Double) ? (Double)price : (Integer)price;
            prod._priceLabel = (String)pj.get(PriceLabelField);
            prod._brandId = brandId;
            prod._retailerId = retailerId;
            prod._description = (String)pj.get(DescriptionField);
            prod._url = (String)pj.get(UrlField);
            prod._inStock = (Boolean)pj.get(InStockField);
            prod._thumbUrl = thumbUrl;
            prod._largeUrl = largeUrl;
            
            return prod;
        }
        else {
            return null;
        }
    }
    
    private Product(Integer id, String name, String imageUrl)
    {
        _id = id;
        _name = name;
        _imageUrl = imageUrl;
    }
    
    public void toJSON(JSONWriter jwriter)
    {
        jwriter.addItem(IdField, _id);
        jwriter.addItem(NameField, _name);
        jwriter.addItem(ImageField, _imageUrl);
        if (_priceLabel != null) {jwriter.addItem(PriceLabelField, _priceLabel);}
        if (_brandId != null) {jwriter.addItem(BrandIdField, _brandId);}
        if (_retailerId != null) {jwriter.addItem(RetailerIdField, _retailerId);}
        if (_description != null) {jwriter.addItem(DescriptionField, _description);}
        if (_url != null) {jwriter.addItem(UrlField, _url);}
        if (_thumbUrl != null) {jwriter.addItem(ThumbImageField, _thumbUrl);}
        if (_largeUrl != null) {jwriter.addItem(LargeImageField, _largeUrl);}
        jwriter.addItem(InStockField, _inStock);
        jwriter.addItem(PriceField, _price);
    }
}
