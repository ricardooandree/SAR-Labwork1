package com.sar;

import com.sar.format.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ConnectionThread extends Thread {
    Main HTTPServer;
    ServerSocket ServerSock;
    Socket client;
    DateFormat HttpDateFormat;
    
    /** Creates a new instance of httpThread */
    public ConnectionThread (Main _HTTPServer, ServerSocket ServerSock, Socket client) {
        this.HTTPServer = _HTTPServer;
        this.ServerSock = ServerSock;
        this.client = client;
        HttpDateFormat = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        HttpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        setPriority(NORM_PRIORITY - 1);
    }
    
    /** Guess the MIME type from the file extension */
    String GuessMime (String fn) {
        String lcname = fn.toLowerCase();
        int extenStartsAt = lcname.lastIndexOf('.');

        if (extenStartsAt < 0) {
            if (fn.equalsIgnoreCase ("makefile"))
                return "text/plain";
            return "unknown/unknown";
        }

        String exten = lcname.substring(extenStartsAt);

        if (exten.equalsIgnoreCase(".htm"))
            return "text/html";
        else if (exten.equalsIgnoreCase(".html"))
            return "text/html";
        else if (exten.equalsIgnoreCase(".gif"))
            return "image/gif";
        else if (exten.equalsIgnoreCase(".jpg"))
            return "image/jpeg";
        else if (exten.equalsIgnoreCase(".js"))
            return "text/javascript";
        else if (exten.equalsIgnoreCase(".css"))
            return "text/css";

        else
            return "application/octet-stream";
    }


    /** Logs a message on the command line */
    public void Log(String s) {
        HTTPServer.Log ("" + client.getInetAddress ().getHostAddress () + ";"
                    + client.getPort () + "  " + s);
    }


    /** Reads a new HTTP Request from the input steam in to a Request object
     * @param TextReader   input stream Buffered Reader coonected to client socket
     * @param echo  if true, echoes the received message to the screen
     * @return Request object containing the request received from the client, or null in case of error
     * @throws java.io.IOException 
     */
    public Request GetRequest (BufferedReader TextReader, boolean echo) throws IOException {
        // Get first line
        String request = TextReader.readLine();  	// Reads the first line
        if (request == null) {
            if (echo) Log ("Invalid request Connection closed\n");
            return null;
        }

        Log("Request: " + request + "\n");

        StringTokenizer st = new StringTokenizer(request);

        if (st.countTokens() != 3) {
           if (echo) Log ("Invalid request received: " + request + "\n");
           return null;  // Invalid request
        } 
        
        // Create an object to store the http request
        Request req = new Request ( HTTPServer, client.getInetAddress().getHostAddress() + ":"
                                    + client.getPort(), ServerSock.getLocalPort() );  
        req.method= st.nextToken();    // Store HTTP method
        req.UrlText= st.nextToken();   // Store URL
        req.version= st.nextToken();   // Store HTTP version
     
        // Read the remaining headers using the readHeaders method of the headers property of the request object   
        req.headers.readHeaders(TextReader, echo);

        // Check if the Content-Length size is different than zero. If true read the body of the request (that can contain POST data)
        int clength = 0;
        try {
            String len = req.headers.getHeaderValue("Content-Length");

            if (len != null)
                clength= Integer.parseInt (len);
            else if (!TextReader.ready ())
                clength= 0;

        } catch (NumberFormatException e) {
            if (echo) Log ("Bad request\n");
            return null;
        
        }
        if (clength > 0) {
            // Length is not 0 - read data to string
            String str = new String();
            char [] cbuf = new char[clength];

            // The content is not formed by line ended with \n so it need to be read char by char
            int n, cnt = 0;
            while ((cnt < clength) && ((n = TextReader.read(cbuf)) > 0)) {
                str = str + new String(cbuf);
                cnt += n;
            }

            if (cnt != clength) {
                Log ("Read request with " + cnt + " data bytes and Content-Length = " + clength + " bytes\n");
                return null;
            }

            req.text = str;
            if (echo)
                Log ("Contents('" + req.text + "')\n");
        }
        return req;
    }    
   

    /* Authenticates the user 
     * @param authHeader - String containing the Authorization header value
     * @return bool - true if the user is authenticated, false otherwise
     */
    public boolean authentication(String authHeader) {
        // Check if the Authentication header exists
        if (authHeader == null)
            return false;

        // Decode and extract the username and password from the Authorization header
        String encodedCredentials = authHeader.substring("Basic".length()).trim();
        byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
        String decodedCredentials = new String(decodedBytes, StandardCharsets.UTF_8);
        String[] credentials = decodedCredentials.split(":");

        // Assign the credentials to their respective variables
        String usernameForm = credentials[0];
        String passwordForm = credentials[1];

        System.out.println("<- DEBUG CREDENTIALS -> " + usernameForm + ":" + passwordForm);

        // Access the crendetials from the Main class
        String[] userPass = Main.UserPass.split(":");

        // Assign the credentials to their respective variables
        String username = userPass[0];
        String password = userPass[1];

        // Return the result of the authentication
        return usernameForm.equals(username) && passwordForm.equals(password);
    }


    /* Checks if the file has been modified 
     * @param req - Request object containing the request received from the client
     * @param file - File object containing the file to be checked
     * @return bool - true if the file has been modified, false otherwise
    */
    public boolean modified(Request req, File f) {
        // Get the last modified date of the file
        long lastModified = f.lastModified();

        // Get the If-Modified-Since header value
        String ifModifiedSinceHeader = req.headers.getHeaderValue("If-Modified-Since");

        // Check if the If-Modified-Since header is set
        if (ifModifiedSinceHeader != null) {
            try {
                // Parse the If-Modified-Since header value
                long ims = HttpDateFormat.parse(ifModifiedSinceHeader).getTime();

                // DEBUG
                System.out.println("<- DEBUG IMS -> " + ims + "\n<- DEBUG LAST-MODIFIED -> " + lastModified);

                // Check if the file has been modified
                return lastModified > ims;

            } catch (Exception e) {
                System.out.println("Error parsing If-Modified-Since header: " + e);
                return false;
            }
        }

        // File has been modified
        return true;
    }


    @Override
    public void run() {
        Response res = null;   // HTTP response object
        Request req = null;   //HTTP request object
        PrintStream TextPrinter = null;

        try {
            /* Get the input and output Streams for the TCP connection and build
              a text (ASCII) reader (TextReader) and writer (TextPrinter) */
            InputStream in = client.getInputStream();
            BufferedReader TextReader = new BufferedReader( new InputStreamReader(in, "8859_1") );
            OutputStream out = client.getOutputStream();
            TextPrinter = new PrintStream(out, false, "8859_1");

            // TODO-DONE: WEEK 3 - Re-use the HTTP connection with a do-while loop
            // Initialize keep-alive variable to signalise that the connection should be kept alive
            boolean keepAlive = true;

            do {
                // Create Request using GetRequest method
                req = GetRequest(TextReader, true); //reads the input http request if everything was read ok it returns true
                
                // If a valid request was received
                if (req != null) { 

                    // Create HTTP response object
                    res = new Response( HTTPServer, client.getInetAddress().getHostAddress() + ":"
                                        + client.getPort(), HTTPServer.ServerName + " - "
                                        + InetAddress.getLocalHost().getHostName() + "-"
                                        + HTTPServer.server.getLocalPort() );

                    // TODO-DONE: WEEK 3 - Redirect connection to HTTPS if needed
                    if (req.LocalPort != HTTPServer.getPortHTTPS()) {   // HTTP Request - Redirect to HTTPS
                        // Set code to temporary redirect
                        res.setCode(ReplyCode.TMPREDIRECT);

                        // Set version to HTTPS
                        res.setVersion(req.version);

                        // Set the location header to the HTTPS URL
                        try {
                            // Construct the HTTPS URL string
                            String httpsUrl = "https://" + req.headers.getHeaderValue("Host").replace("20000","20043") + req.UrlText;

                            // Create a URL object
                            URL url = new URL(httpsUrl);

                            // Set the location header in the response
                            res.responseHeaders.setHeader("Location", url.toString());
                            
                            // DEBUG
                            System.out.println("<- DEBUG HTTPS-REDIRECT-URL -> " + url.toString());

                        } catch (MalformedURLException e) {
                            System.out.println("Error creating URL: " + e);
                        }

                        // Sends answer - without data
                        res.send_Answer(TextPrinter, false, true);

                    } else {    // HTTPS Request - Processing
                        // TODO: WEEK 4 - Authorization
                        if (Main.Authorization) {
                            System.out.println("<- DEBUG AUTHORIZATION ->");
                            // Check if the request contains a valid authorization token (cookie)
                            Properties cookies = req.get_cookies();
                            String authCookie = cookies.getProperty("authToken");

                            // Check if there's any authorization token (cookie)
                            if (authCookie == null) {
                                System.out.println("<- DEBUG AUTH-TOKEN -> " + authCookie);
                                String authHeader = req.headers.getHeaderValue("Authorization");

                                // Authenticates the user credentials
                                if (!authentication(authHeader)) {  // Authentication unsuccessful
                                    System.out.println("<- DEBUG FAILED-AUTH-HEADER -> " + authHeader);
                                    res.setCode(ReplyCode.UNAUTHORIZED);
                                    res.setVersion(req.version);

                                    // Set WWW-Authenticate header 
                                    res.responseHeaders.setHeader("WWW-Authenticate", "Basic");

                                    res.send_Answer(TextPrinter, false, true);

                                    // Exit the loop to prompt for credentials again
                                    break;

                                } else {    // Authentication successful
                                    System.out.println("<- DEBUG SUCCESS-AUTH-HEADER -> " + authHeader);
                                    // Generate authorization token (session token)
                                    // TODO: Get current date and create an expiring date
                                    String formattedExpirationDate = "Fri, 29-Mar-2024 17:43:00 GMT";
                                    String authToken = "authToken=yes;expires=" + formattedExpirationDate;
                                    
                                    // Set authorization cookie
                                    res.setCookie(authToken);

                                    // Remove Authorization header
                                    // req.headers.removeHeader("Authorization");
                                }
                            }
                        }
                        System.out.println("<- DEBUG PROCESSING-HTTPS-REQ ->");
                        // Check if the request is to the API - API URL received 
                        if (req.UrlText.toLowerCase().endsWith("api")) {
                            while (req.UrlText.startsWith("/"))
                                req.UrlText = req.UrlText.substring(1);  // Remove "/" from url_txt

                            // Create JavaRESTAPI object
                            JavaRESTAPI api = new JavaRESTAPI();
                            try {
                                Log("run JavaAPI\n");
                                
                                // Runs API get method
                                api.doGet(client, req.headers, req.get_cookies(), res);

                                // TODO: WEEK 5 - Check the request method + cookies + send answer

                                } catch (Exception e) {
                                    res.set_error(ReplyCode.BADREQ, req.version);
                                }
                        } else {
                            // Get file with contents
                            String filename = HTTPServer.getStaticFilesUrl() + req.UrlText + (req.UrlText.equals("/") ? "index.htm" : "");

                            System.out.println("Filename= " + filename);

                            File f = new File(filename);

                            if (f.exists() && f.isFile()) {
                                // TODO: WEEK 4 - Modified file DONE
                                if (modified(req, f)) {    // File has been modified
                                    res.setCode(ReplyCode.OK);
                                    res.setVersion(req.version);
                                    res.setFileHeaders(new File(filename), GuessMime(filename));

                                } else {   // File has not been modified
                                    res.setCode(ReplyCode.NOTMODIFIED);
                                    res.setVersion(req.version);
                                }

                                /* 
                                // Define reply contents
                                res.setCode(ReplyCode.OK);
                                res.setVersion(req.version);
                                res.setFileHeaders(new File(filename), GuessMime(filename));
                                // NOTICE that only the first line of the reply is sent!
                                // No additional headers are defined!
                                */
                            } else {
                                System.out.println("File not found");
                                res.set_error(ReplyCode.NOTFOUND, req.version);
                                // NOTICE that some code is missing in HTTPAnswer!
                            }
                        }
                        // Send reply - with data
                        res.send_Answer(TextPrinter, true, true);

                        // Check if the connection should be kept alive
                        String connectHeader = req.headers.getHeaderValue("Connection");

                        // Check if the connection header is set to keep-alive
                        keepAlive = connectHeader != null && connectHeader.equalsIgnoreCase("keep-alive");

                        // DEBUG
                        System.out.println("<- DEBUG CONNECTION -> " + connectHeader + "\n<- DEBUG KEEP-ALIVE -> " + keepAlive);
                    }
                } else {    // Invalid request - if req == null
                    res = new Response( HTTPServer, client.getInetAddress().getHostAddress() + ":"
                                        + client.getPort(), HTTPServer.ServerName + " - "
                                        + InetAddress.getLocalHost().getHostName() + "-"
                                        + HTTPServer.server.getLocalPort() );

                    // Sets the error code to bad request
                    res.set_error(ReplyCode.BADREQ, "HTTP/1.1");

                    keepAlive = false;
                }
            } while (keepAlive);

            in.close();
            TextPrinter.close();
            out.close();

        } catch (IOException e) {
            if (HTTPServer.active())
                System.out.println("I/O error " + e);

        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
                System.out.println("Error closing client" + e);
            }

            HTTPServer.thread_ended();
            Log("Closed TCP connection\n");
        }
    }
}
