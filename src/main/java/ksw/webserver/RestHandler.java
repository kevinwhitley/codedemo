package ksw.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ksw.kwutil.JSONReader;
import ksw.kwutil.JSONReader.JSONParseException;
import ksw.kwutil.JSONWriter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/* beginnings of a Rails-style JSON REST servlet */
//see http://stackoverflow.com/questions/14922623/what-is-the-complete-list-of-expected-json-responses-for-ds-restadapter
//for discussion of json responses that ember expects

// needs error handling for bad REST requests
// needs to add support for user permissions (not everybody can edit everything)

public abstract class RestHandler extends AbstractHandler
{
    public RestHandler()
    {
    }
    
    // get the singular name of the class
    public abstract String getSingularName();
    
    // get the plural name of the class
    public String getPluralName()
    {
        return getSingularName()+"s";
    }
    
    public boolean doesSideload()
    {
        return false;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException
    {
        // what kind of request do we have?
        String method = request.getMethod();
        String pathInfo = request.getPathInfo().substring(1);
        boolean isEdit = false;
        boolean isBulk = false;
        int docId = -1;
        boolean notFound = false;
        if ("/bulk".equals(pathInfo)) {
            isBulk = true;
        }
        else if (pathInfo != null && pathInfo.length() > 0) {
            try {
                docId = Integer.parseInt(pathInfo);
            }
            catch (NumberFormatException exc) {
                // we don't understand this request
                notFound = true;
            }
        }
        
        // our response is always json
        response.setContentType("application/json");
        // generate json for all the storiez
        JSONWriter jWriter = new JSONWriter();
        jWriter.startObject();

        if (!notFound) {
            
            // additional case for GET:
            //Comments for a post can be loaded by post.get('comments'). The REST adapter
            // will send a GET request to /comments?ids[]=1&ids[]=2&ids[]=3.
            
            if ("GET".equals(method)) {
                if (docId >= 0) {
                    ClientJSON toClient = find(docId);
                    jWriter.addObjectToObject(getSingularName());
                    toClient.toClientJSON(jWriter);
                    jWriter.endObject();
                    if (doesSideload()) {
                        List<ClientJSON> lc = new ArrayList<ClientJSON>(1);
                        lc.add(toClient);
                        sideload(jWriter, lc);
                    }
                }
                else {
                    List<ClientJSON> docs = null;
                    List<Integer> requestedIds = getIdList(request);
                    if (requestedIds == null) {
                        // we're sending back everythings
                        docs = findAll();
                    }
                    else {
                        // pull out the requested docs
                        docs = findByIds(requestedIds);
                    }
                    jWriter.addArrayToObject(getPluralName());
                    for (ClientJSON doc : docs) {
                        jWriter.startObject();
                        doc.toClientJSON(jWriter);
                        jWriter.endObject();
                    }
                    jWriter.endArray();
                    if (doesSideload()) {
                        sideload(jWriter, docs);
                    }
                }
            }
            else if ("POST".equals(method)) {
                Map<String, Object> data = readRequestData(request);
                Map<String, Object> singleData = (Map<String, Object>)data.get(getSingularName());
                if (singleData != null) {
                    // creating a single object
                    ClientJSON doc = createFromClientJSON(singleData);
                    jWriter.addObjectToObject(getSingularName());
                    doc.toClientJSON(jWriter);
                    jWriter.endObject();
                }
                else {
                    // this is a bulk create
                    jWriter.addArrayToObject(getPluralName());
                    List<Map<String, Object>> docsData = (List<Map<String, Object>>)data.get(getPluralName());
                    for (Map<String, Object> docData : docsData) {
                        ClientJSON doc = createFromClientJSON(docData);
                        jWriter.startObject();
                        doc.toClientJSON(jWriter);
                        jWriter.endObject();
                    }
                    jWriter.endArray();
                }
                isEdit = true;
            }
            else if ("PUT".equals(method)) {
                Map<String, Object> data = readRequestData(request);
                if (docId >= 0) {
                    ClientJSON doc = find(docId);
                    Map<String, Object> singleDocData = (Map<String, Object>)data.get(getSingularName());
                    doc.updateFromClientJSON(singleDocData);
                    
                    jWriter.addObjectToObject(getSingularName());
                    doc.toClientJSON(jWriter);
                    jWriter.endObject();
                    isEdit = true;
                }
                else if (isBulk) {
                    jWriter.addArrayToObject(getPluralName());
                    List<Map<String, Object>> docsData = (List<Map<String, Object>>)data.get(getPluralName());
                    for (Map<String, Object> docData : docsData) {
                        Integer id = (Integer)docData.get("id");
                        ClientJSON doc = find(id);
                        doc.updateFromClientJSON(docData);
                        jWriter.startObject();
                        doc.toClientJSON(jWriter);
                        jWriter.endObject();
                    }
                    jWriter.endArray();
                    isEdit = true;
                }
                else {
                    notFound = true;
                }
            }
            else if("DELETE".equals(method)) {
                if (docId >= 0) {
                    delete(docId);
                    isEdit = true;
                }
                else if (isBulk) {
                    // we expect to see an array of ids
                    Map<String, Object> data = readRequestData(request);
                    List<Integer> ids = (List<Integer>)data.get(getPluralName());
                    jWriter.addArrayToObject(getPluralName());
                    for (Integer id : ids) {
                        delete(id);
                        jWriter.addItem(null, id);
                    }
                    jWriter.endArray();
                    isEdit = true;
                }
                else {
                    notFound = true;
                }
            }
        }

        if (notFound) {
            // some invalid request
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            jWriter.addItem("message", "invalid REST request");
        }
        
        jWriter.endObject();
    
        PrintWriter writer = response.getWriter();
        String result = jWriter.toString();
        System.out.println(result);
        writer.print(result);
        writer.close();
        
        if (isEdit) {
            commitEdit();
        }
    }
    
    private Map<String, Object> readRequestData(HttpServletRequest request)
    {
        Map<String, Object> result = null;
        
        // turn the entire request into a String
        try {
            BufferedReader reader = request.getReader();
            StringBuilder buffer = new StringBuilder();
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                buffer.append(line);
            }
            reader.close();
            
            JSONReader jReader = new JSONReader();
            result = (Map<String, Object>)jReader.parse(buffer);
            return result;
        } catch (IOException e) {
            return null;
        } catch (JSONParseException e) {
            return null;
        }
    }
    
    private List<Integer> getIdList(HttpServletRequest request)
    {
        // if ids are specified, we expect a query string like:
        // ids[]=17&ids[]=4
        String[] idStrings = request.getParameterValues("ids[]");
        if (idStrings != null) {
            List<Integer> ids = new ArrayList<Integer>(idStrings.length);
            for (String idString : idStrings) {
                try {
                    ids.add(Integer.parseInt(idString));
                }
                catch(NumberFormatException exc) {
                    // just ignore bad strings
                }
            }
            return ids;
        }
        else {
            return null;
        }
    }
    
    public abstract ClientJSON createFromClientJSON(Map<String, Object> data);
    
    public abstract void delete(int docId);
    
    public abstract ClientJSON find(int docId);
    public abstract List<ClientJSON> findAll();
    public abstract List<ClientJSON> findByIds(Collection<Integer> ids);
    public void sideload(JSONWriter jWriter, List<ClientJSON> docs) {}
    
    // used to notify class that an has completed successfully
    public void commitEdit() {};

    public static interface ClientJSON
    {
        public void toClientJSON(JSONWriter jWriter);
        public void updateFromClientJSON(Map<String, Object> data);
    }
}
