package com.sar.format;

import com.sar.*;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 *
 * @author pedroamaral 
 * 
 * Class that stores all information about a HTTP request
 * Incomplete Version 22/23
 */
public class Request {
    
    public String method;   // stores the HTTP Method of the request
    public String UrlText;  // stores the url of the request
    public String version;  // stores the HTTP version of the request
    public Headers headers; // stores the HTTP headers of the request
    public Properties cookies; //stores cookies received in the Cookie Headers
    private final Main HttpServer;  // log object UserInterface
    public String text;     //store possible contents in an HTTP request (for example POST contents)
    private final String idStr;    // idString for logging purposes
    public int LocalPort;  // local HTTP server port
    
    /** 
     * Creates a new instance of HTTPQuery
     * @param _UserInterface   log object
     * @param id    log id
     * @param LocalPort local HTTP server port
     */
    public Request (Main _HttpServer, String id, int LocalPort) {
        // initializes everything to null
        this.headers= new Headers (_HttpServer);
        this.HttpServer= _HttpServer;
        this.idStr= id;
        this.LocalPort= LocalPort;
        this.UrlText= null;
        this.method= null;
        this.version= null;
        this.text= null;
        this.cookies = new Properties();
    }

    public void Log(String s) {
        HttpServer.Log (idStr+ "  " + s );
    }
    
     /**
     * Get a header property value
     * @param hdrName   header name
     * @return          header value
     */
    public String getHeaderValue(String hdrName) {
        return headers.getHeaderValue(hdrName);
    }
    
    /**
     * Set a header property value
     * @param hdrName   header name
     * @param hdrVal    header value
     */
    public void setHeader(String hdrName, String hdrVal) {
        headers.setHeader(hdrName, hdrVal);
    }

    
    /** Returns the Cookie Properties object */
    public Properties get_cookies () {
        return this.cookies;
    }
    
    
    /**
     * Remove a header property name
     * @param hdrName   header name
     * @return true if successful
     */
    public boolean removeHeader(String hdrName) {
        return headers.removeHeader(hdrName);
    } 
}