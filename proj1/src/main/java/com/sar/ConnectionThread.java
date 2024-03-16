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
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class ConnectionThread extends Thread  {
    Main HTTPServer;
    ServerSocket ServerSock;
    Socket client;
    DateFormat HttpDateFormat;
    
    /** Creates a new instance of httpThread */
    public ConnectionThread (Main _HTTPServer, ServerSocket ServerSock, Socket client) {
        this.HTTPServer= _HTTPServer;
        this.ServerSock= ServerSock;
        this.client = client;
        HttpDateFormat= new SimpleDateFormat ("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        HttpDateFormat.setTimeZone (TimeZone.getTimeZone ("GMT"));
        setPriority ( NORM_PRIORITY - 1 );
    }
    
        /** Guess the MIME type from the file extension */
    String GuessMime (String fn) {
        String lcname = fn.toLowerCase ();
        int extenStartsAt = lcname.lastIndexOf ('.');
        if (extenStartsAt<0) {
            if (fn.equalsIgnoreCase ("makefile"))
                return "text/plain";
            return "unknown/unknown";
        }
        String exten = lcname.substring (extenStartsAt);
        if (exten.equalsIgnoreCase (".htm"))
            return "text/html";
        else if (exten.equalsIgnoreCase (".html"))
            return "text/html";
        else if (exten.equalsIgnoreCase (".gif"))
            return "image/gif";
        else if (exten.equalsIgnoreCase (".jpg"))
            return "image/jpeg";
        else if (exten.equalsIgnoreCase (".js"))
            return "text/javascript";
        else if (exten.equalsIgnoreCase (".css"))
            return "text/css";

        else
            return "application/octet-stream";
    }

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
        String request = TextReader.readLine( );  	// Reads the first line
        if (request == null) {
            if (echo) Log ("Invalid request Connection closed\n");
            return null;
        }
        Log("Request: " + request + "\n");
        StringTokenizer st= new StringTokenizer(request);
        if (st.countTokens() != 3) {
           if (echo) Log ("Invalid request received: " + request + "\n");
           return null;  // Invalid request
        } 
         //create an object to store the http request
         Request req= new Request (HTTPServer, client.getInetAddress ().getHostAddress () + ":"
         + client.getPort (), ServerSock.getLocalPort ());  
         req.method= st.nextToken();    // Store HTTP method
         req.UrlText= st.nextToken();    // Store URL
         req.version= st.nextToken();  // Store HTTP version
     
        // read the remaining headers using the readHeaders method of the headers property of the request object   
        req.headers.readHeaders(TextReader, echo);
        
        // check if the Content-Length size is different than zero. If true read the body of the request (that can contain POST data)
        int clength= 0;
        try {
            String len= req.headers.getHeaderValue("Content-Length");
            if (len != null)
                clength= Integer.parseInt (len);
            else if (!TextReader.ready ())
                clength= 0;
        } catch (NumberFormatException e) {
            if (echo) Log ("Bad request\n");
            return null;
        }
        if (clength>0) {
            // Length is not 0 - read data to string
            String str= new String ();
            char [] cbuf= new char [clength];
            //the content is not formed by line ended with \n so it need to be read char by char
            int n, cnt= 0;
            while ((cnt<clength) && ((n= TextReader.read (cbuf)) > 0)) {
                str= str + new String (cbuf);
                cnt += n;
            }
            if (cnt != clength) {
                Log ("Read request with "+cnt+" data bytes and Content-Length = "+clength+" bytes\n");
                return null;
            }
            req.text= str;
            if (echo)
                Log ("Contents('"+req.text+"')\n");
        }

        return req;
    }    
   
    
     @Override
    public void run( ) {

        Response res= null;   // HTTP response object
        Request req = null;   //HTTP request object
        PrintStream TextPrinter= null;

        try {
            /*get the input and output Streams for the TCP connection and build
              a text (ASCII) reader (TextReader) and writer (TextPrinter) */
            InputStream in = client.getInputStream( );
            BufferedReader TextReader = new BufferedReader(
                    new InputStreamReader(in, "8859_1" ));
            OutputStream out = client.getOutputStream( );
            TextPrinter = new PrintStream(out, false, "8859_1");
            //Create Request using GetRequest method
            req= GetRequest(TextReader, true); //reads the input http request if everything was read ok it returns true
            if (req!=null){// if a valid request was received 
                //Create an HTTP response object. 
                res= new Response(HTTPServer,
                    client.getInetAddress ().getHostAddress () + ":" + client.getPort (),
                    HTTPServer.ServerName+" - "+InetAddress.getLocalHost().getHostName ()+"-"+HTTPServer.server.getLocalPort ());

                //API URL received 
                if (req.UrlText.toLowerCase().endsWith ("api")) 
                {
                    while (req.UrlText.startsWith ("/") )
                      req.UrlText = req.UrlText.substring (1);  ///remove "/" from url_txt
                      JavaRESTAPI api= new JavaRESTAPI ();
                      try {
                           Log("run JavaAPI\n");
                            api.doGet (client, req.headers, req.get_cookies(), res);
                          } catch (Exception e) {
                              res.set_error (ReplyCode.BADREQ, req.version);
                          }
                }else {
                    // Get file with contents
                    String filename= HTTPServer.getStaticFilesUrl() + req.UrlText + (req.UrlText.equals("/")?"index.htm":"");
                    System.out.println("Filename= "+filename);
                    File f= new File(filename);

                    if (f.exists() && f.isFile()) {
                        // Define reply contents
                        res.setCode(ReplyCode.OK);
                        res.setVersion(req.version);
                        res.setFileHeaders(new File(filename), GuessMime(filename));
                        // NOTICE that only the first line of the reply is sent!
                        // No additional headers are defined!
                    } else {
                        System.out.println( "File not found" );
                        res.set_error(ReplyCode.NOTFOUND, req.version);
                        // NOTICE that some code is missing in HTTPAnswer!
                    }
                }
                // Send reply
                res.send_Answer(TextPrinter, true, true);
            }
            

            in.close();
            TextPrinter.close();
            out.close();
        } catch ( IOException e ) {
            if (HTTPServer.active())
                System.out.println( "I/O error " + e );
        } finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
                System.out.println("Error closing client"+e);
            }
            HTTPServer.thread_ended();
            Log("Closed TCP connection\n");
        }
    }
}
