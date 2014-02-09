package ksw.shopstyle;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import ksw.kwutil.JSONReader;
import ksw.kwutil.JSONReader.JSONParseException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

public class TestShopStyle
{
    public static void main(String[] args)
    {
        //URI uri = new URI("http", null, "localhost", -1, "/", params.toString(), null);
        //String url = uri.toASCIIString();
        //System.out.println("url is " + url);
        
        String url = "http://api.shopstyle.com/api/v2/retailers?pid=uid2004-79498-32";
        
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
            //System.out.println(result.substring(0, 1000));
            JSONReader jr = new JSONReader();
            Map data = (Map)jr.parse(result);
            List<Map> retailers = (List<Map>)data.get("retailers");
            System.out.println("# of retailers: " + retailers.size());
            for (int ii=0; ii<20; ii++) {
                Map<String, Object> retailer = retailers.get(ii);
                System.out.println("Retailer: " + retailer.get("name"));
            }
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

        System.out.println("done");
    }
}
