package com.sar.format;

import com.sar.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Response {
    private final int MAXBUFFER_SIZE = 8192;
    /**
     * Reply code information
     */
    public ReplyCode code;
    /**
     * Reply headers data
     */
    public Headers responseHeaders; // stores the HTTP headers for the response
    public ArrayList<String> setCookies;   // List to store Set-cookie header fields
    /**
     * Reply contents
     * They are stored either in a text buffer or in a file
     */
    public String text; // buffer with reply contents for dynamic API responses or server generated HTML code
    public File file;   // file used if text == null, for responses that contain a file  
    Main UserInterface;
    String IdStr;  // Thread id - for logging purposes
    /**
     * Creates a new instance of HTTPAnswer
     * @param _UserInterface
     * @param IdStr
     * @param server_name
     */
    public Response(Main _UserInterface, String IdStr, String server_name) {
        this.code = new ReplyCode(); // code constains an instance of the HTTPReplyCode Class thar contains HTTP code values and an HTTP version field.
        this.IdStr = IdStr;
        this.UserInterface = _UserInterface;
        responseHeaders = new Headers (UserInterface);  // Headers object to store response HTTP headers  
        setCookies = new ArrayList<>(); // Array List of Strings to contain the Strings that make up the several values of the Set_Cookie Header. 
        text = null;
        file = null;

        //Define Server header field name
        responseHeaders.setHeader("Server", server_name);
    }

    void Log(String s) {
         UserInterface.Log (IdStr + "  " + s );
    }
   
    // SETTER METHODS
    /* 
    Method to set the HTTP reply code of the answer
    */
    public void setCode(int _code) {
        code.setCode(_code);
    }
    /* 
    Method to set the HTTP version of the answer
    */
    public void setVersion(String v) {
        code.setVersion(v);
    }
    /* 
    Method to set an HTTP header of the answer
    */
    public void setHeader(String name, String value) {
        responseHeaders.setHeader(name, value);
    }
    /* 
    Method to add a cookie value to the list of cookies that are to be sent in Set-Cookie Headers
    */
    public void setCookie(String setcookieLine) {
        setCookies.add(setcookieLine);
    }

    /** Sets the headers needed in a reply with a static file content and fill
     * the file property with the File object of the static file to send
     * @param _f
     * @param mime_enc */
    public void setFileHeaders(File _f, String mime_enc) {
        file = _f;
        // TODO-DONE: WEEK 1 - header lines not set in 'Headers' object!
        // File Headers: HTTP/1.1 200 OK - Date, Server, Last-Modified, ETag, Content-Length, Connection, Content-Type - Data

        if (file != null){
            // Set 'Content-Length', 'Content-Type' and 'Last-Modified' header fields
            responseHeaders.setHeader("Content-Length", String.valueOf(file.length()));
            responseHeaders.setHeader("Content-Type", mime_enc);

            // Create DateFormat object - http date format
            DateFormat httpformat = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
            responseHeaders.setHeader("Last-Modified", httpformat.format( new Date(file.lastModified()) ));

            // NOTE: Set 'Connection' header field
        }

        // Check if the header fields were set
        if (responseHeaders.getHeaderValue("Content-Type") == null || responseHeaders.getHeaderValue("Content-Length") == null || responseHeaders.getHeaderValue("Last-Modified") == null){
            Log("Failed to set header fields in HTTPAnswer.set_file\n");
        }
    }
    
    /** Sets the headers needed in a reply with a locally generated HTML string
     * (_text object) and fill the text property with the String object 
     * containing the HTML to send
     * @param _text */
    public void setTextHeaders(String _text) {
        text = _text;
        // TODO-DONE: WEEK 1 - header lines not set in 'Headers' object!

        if (text != null){
            // Set 'Content-Length' and 'Content-Type' header fields
            responseHeaders.setHeader("Content-Length", String.valueOf(text.length())); 
            responseHeaders.setHeader("Content-Type", "text/html; charset=ISO-8859-1");
        }

        // Check if the header fields were set
        if (responseHeaders.getHeaderValue("Content-Type") == null || responseHeaders.getHeaderValue("Content-Length") == null){
            Log("Failed to set header fields in HTTPAnswer.set_text\n");
        }
    }

    // GETTER METHODS
    /**
     * Returns the current value of the answer code
     * @return 
     */
    public int getCode() {
        return code.getCode();
    }

    /**
     * Returns a string with the first line of the answer
     * @return 
     */
    public String get_first_line() {
        return code.toString();
    }

    /**
     * Returns an iterator over all header names
     * @return 
     */
    public Enumeration<Object> get_Iterator_parameter_names() {
        return responseHeaders.getAllHeaderNames();
    }

    /**
     * Returns the array list with all cookie Strings to use as values in Set-Cookie Headers
     * @return 
     */
    public ArrayList<String> getSetCookies() {
        return setCookies;
    }

    /** Sets the "Date" header field with the local date in HTTP format */
    void setDate() {
        DateFormat httpformat =
                new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        httpformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        responseHeaders.setHeader("Date", httpformat.format(new Date()));
    }

    /** Prepares an HTTP answer with an error code
     * @param _code
     * @param version */
    public void set_error(int _code, String version) {
        setVersion(version);
        setDate();
        code.setCode(_code);
        if (code.getCodeTxt() == null) {
            code.setCode(ReplyCode.BADREQ);
        }

        if (!version.equalsIgnoreCase("HTTP/1.0")) {
            responseHeaders.setHeader("Connection", "close");
        }
        // Prepares a web page with an error description
        String txt = "<HTML>\r\n";
        txt = txt + "<HEAD><TITLE>Error " + code.getCode() + " -- " + code.getCodeTxt()
                + "</TITLE></HEAD>\r\n";
        txt = txt + "<H1> Error " + code.getCode() + " : " + code.getCodeTxt() + " </H1>\r\n";
        txt = txt + "  by " + responseHeaders.getHeaderValue("Server") + "\r\n";
        txt = txt + "</HTML>\r\n";

        // Set the header properties
        setTextHeaders(txt);
    }

    /** Sends the HTTP reply to the client using 'pout' text device
     * @param TextPrinter
     * @param send_data indicates if data is present in the response or only headers
     * @param echo
     * @throws java.io.IOException */
    public void send_Answer(PrintStream TextPrinter, boolean send_data, boolean echo) throws IOException {
        //echo = true;    // Debug Purposes
        if (code.getCodeTxt() == null) {
            code.setCode(ReplyCode.BADREQ);
        }
        if (echo) {
            Log("Answer: " + code.toString() + "\n");
        }
        TextPrinter.print(code.toString() + "\r\n");
        // TODO-DONE: WEEK 1 - Send all headers except set_cookie using headers Object writeHeaders
        /*
         * HTTP/1.1 200 OK
         * Date: Wed, 27 Feb 2012 12:00:00 GMT
         * Server: Apache/2.2.8 (Fedora)
         * Last-Modified: Thu, 28 Set 2003 19:00:00 GMT
         * ETag: “405990-4ac-4478e4405c6c0”
         * Content-Length: 6821
         * Connection: close
         * Content-Type: text/html; charset=ISO-8859-1
         * … { dados html } …
         */
        System.out.println("<- DEBUG FIRST LINE -> " + get_first_line());
        // Set date
        setDate();

        // Send all headers - except 'Set-Cookie'
        responseHeaders.writeHeaders(TextPrinter, echo); 

        /**
         * TODO-DONE: WEEK 1 - Send 'Set-Cookie'
         * Check if there are cookies to send if so add the corresponding Set-Cookie Headers
         * Set-Cookies have to be sent manually using TextPrinter without using the Headers object 
         * since it uses a Properties Object to store the headers and there can be multiple Set-cookie headers 
         * and a Properties cannot have two Keys with the same value
         */
        
        ArrayList <String> cookiesList = getSetCookies();
        if (cookiesList.size() > 0){
            for (String cookies : cookiesList){
                TextPrinter.print("Set-Cookie: " + cookies + "\r\n");

                if (echo){
                    Log("Set-Cookie: " + cookies + "\n");
                }
            }
        }
        TextPrinter.print("\r\n");

        if (send_data) {

            if (text != null) {
                TextPrinter.print(text);
            } else if (file != null) {
                try (FileInputStream fin = new FileInputStream(file)) {
                    byte [] data = new byte [fin.available()];
                    // TODO-DONE: WEEK 1 - Modify this code to send a file chunk-by-chunk to avoid having CRASHES with BIG FILES

                    // Read the entire file to buffer 'data'
                    fin.read(data);
                    
                    // Read the file in chunks of MAXBUFFER_SIZE bytes and output it
                    int size = data.length;
                    int chunks = size / MAXBUFFER_SIZE;
                    
                    for (int i=0; i < chunks; i++) {
                        TextPrinter.write(data, i*MAXBUFFER_SIZE, MAXBUFFER_SIZE);
                    }
                    // Last chunk - might not be of size chunk
                    TextPrinter.write(data, chunks*MAXBUFFER_SIZE, size - chunks*MAXBUFFER_SIZE);
                    fin.close();

                    // Previous code - read the entire file to buffer 'data' and output it
                    //TextPrinter.write(data);
                }
                catch (IOException e) {
                  System.out.println( "I/O error opening FileInputStream " + e);
                }
            } else if ((code.getCode() != ReplyCode.NOTMODIFIED)&&(code.getCode() != ReplyCode.TMPREDIRECT)) {
                Log("Internal server error sending answer\n");
            }
        }
        TextPrinter.flush();
    }
    
}
