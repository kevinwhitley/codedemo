package ksw.shopstyle;

import ksw.webserver.HerokuServer;

public class ShopStyleServer extends HerokuServer
{
    public static void main(String[] args)
    {
        try {
            ShopStyleServer ss = new ShopStyleServer();

            ss.initialize();
            
            ShopStyleData ssdata = new ShopStyleData();
            ss.setup("ksw/shopstyle/client", ssdata, "/ss", ShopStyleApi.class, null, null);

            ss.run();
        }
        catch (Exception exc) {
            System.err.println("Exception " + exc);
        }
    }
    
    private ShopStyleServer()
    {
        
    }
    
    
}
