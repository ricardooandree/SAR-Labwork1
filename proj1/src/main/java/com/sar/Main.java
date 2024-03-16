package com.sar;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Main {
    
    //Static Files Location directory
    public final String ServerName= "SAR Server by ----/-----";
    public final static String StaticFiles = "proj1/html";
    public final static String HOMEFILENAME = "index.htm";
    //Keep alive settings
    public final static boolean keepAlive = true;
    public final static int KeepAliveTime = 0; // set time in miliseconds to keep connection open
    //Authorization settings
    public final static boolean Authorization = false ;
    public final static String UserPass = "Username:Pass";
    //port for Server socket serving HTTP requests
    public final static int HTTPport = 20000;
    //port for Server Socket serving HTTPs requests
    public final static int HTTPSport = 20043;
    static public SSLContext sslContext= null;
    final static int MaxAcceptLog= 10;  // Accepts up to 10 pending TCP connections
    ServerThread MainThread= null;
    ServerThread MainSecureThread = null; //https thread
    public ServerSocket server;
    public javax.net.ssl.SSLServerSocket serverS; // Secure TCP server
    public int n_threads=0;
    
    //logs a message on the command line
    public void Log (String s) {
         System.out.print (s);
    }
    
    //Method to start the Server Socket
    public void startServer(){
            // Starts http web server
            try {
                server= new ServerSocket (HTTPport, MaxAcceptLog);
            } catch (java.io.IOException e) {
                Log ("Server start failure: " + e + "\n");
                return;
            }
             // Starts https server
            try {
                SSLServerSocketFactory sslSrvFact = sslContext.getServerSocketFactory();
                serverS =(SSLServerSocket)sslSrvFact.createServerSocket(HTTPSport);
                serverS.setNeedClientAuth(false);
            } catch (java.io.IOException e) {
                Log("Server start failure: " + e + "\n");
                return;
            }
            // Gets local IP
            try {
                Log ("Local IP: " + InetAddress.getLocalHost ().getHostAddress () + "\n");
            } catch (UnknownHostException e) {
                Log ("Failed to get local IP: "+e+"\n");
            }
            // starts main thread
            MainThread= new ServerThread ( this, server);
            MainSecureThread= new ServerThread( this, serverS);
            MainThread.start ();
            MainSecureThread.start ();

    }

      /** Callback called when a new HTTP connection thread starts */
    public void thread_started () {
        if (MainThread != null) {
            n_threads++;
            Log("Number of active Threads: "+n_threads);
        }
    }
    
       /** Callback called when a new HTTP connection thread starts */
    public void thread_ended () {
        if (MainThread != null) {
            n_threads--;
            Log("Number of active Threads: "+n_threads);
        }
    }
    
     /** Returns the HTTP port number
     * @return Port Value HTPP */
    public int getPortHTTP () {
        return  HTTPport;
      
    }
    /** Returns the HTTPS port number */
    public int getPortHTTPS() {
       return  HTTPSport;
    }
    
    /** Returns the keep alive interval value in miliseconds */
    public int getKeepAlive () {
        return 1000*KeepAliveTime;
       
    }
    
      /** Returns the root html directory
     * @return  */
    public String getStaticFilesUrl () {
        return StaticFiles;
    }
    
    /** Returns the number of active connections */
    public int active_connects () {
        if (MainThread == null && MainSecureThread==null)
            return 0;
        return n_threads;
    }

    /** Returns true if a main_thread is active */
    public boolean active () {
        return (MainThread != null || MainSecureThread !=null );
    }
    
    /** Open up the KeyStore to obtain the Trusted Certificates.
     *  KeyStore is of type "JKS". Filename is "serverAppKeys.jks"
     *  and password is "myKeys".
     */
    private static void initContext() throws Exception {
        if (sslContext != null)
            return;
        
        try {
            // MAke sure that JSSE is available
           // Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            
            // Create/initialize the SSLContext with key material
            char[] passphrase = "password".toCharArray(); // if the certificate was created with the password = "password"
            
            KeyStore ksKeys;
            try {
                // First initialize the key and trust material.
                ksKeys= KeyStore.getInstance("JKS");
            } catch (Exception e) {
                System.out.println("KeyStore.getInstance: "+e);
                return;
            }
            ksKeys.load(new FileInputStream("proj1/keystore"), passphrase);
            System.out.println("KsKeys has "+ksKeys.size()+" keys after load");
            
            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, passphrase);
            System.out.println("KMfactory default alg.: "+KeyManagerFactory.getDefaultAlgorithm());
            
            
            sslContext = SSLContext.getInstance("TLSv1.2"); 
            sslContext.init(
                    kmf.getKeyManagers(), null /*tmf.getTrustManagers()*/, null);
            
        } catch (Exception e) {
            System.out.println("Failed to read keystore and trustfile.");
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            initContext();
        } catch (Exception e) {
            System.out.println("Error loading security context");
        }
        System.out.println("Starting Server");
        //start server
        Main HttpServer = new Main();
        HttpServer.startServer();
    }
}