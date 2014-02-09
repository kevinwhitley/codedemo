package ksw.shopstyle;

import java.util.Map;

import ksw.kwutil.JSONWriter;

public class Retailer
{
    private String _id;
    private String _name;
    
    private static final String IdField = "id";
    private static final String NameField = "name";
    
    public static Retailer fromJSON(Map rj)
    {
        String id = (String)rj.get(IdField);
        String name = (String)rj.get(NameField);
        if (id != null && name != null) {
            return new Retailer(id, name);
        }
        else {
            return null;
        }
    }
    
    private Retailer(String id, String name)
    {
        _id = id;
        _name = name;
    }
    
    public void toJSON(JSONWriter jwriter)
    {
        jwriter.addItem(IdField, _id);
        jwriter.addItem(NameField, _name);
    }
}
