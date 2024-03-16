package com.sar;

import java.net.ServerSocket;

/**
 * Server Thread
 * @author pedroamaral
 */
public class ServerThread extends Thread {
    
        Main HttpServer;
        ServerSocket ss;
        volatile boolean active;
        
        ServerThread ( Main app, ServerSocket ss ) {
            this.HttpServer= app;
            this.ss= ss;
        }
        
        public void wake_up () {
            this.interrupt ();
        }
        
        public void stop_thread () {
            active= false;
            HttpServer.thread_ended();
            this.interrupt ();
        }
        
        @Override
        public void run () {
            System.out.println (
                    "\n Server at port "+ss.getLocalPort ()+"\n"+ " started!\n");
            active= true;
            while ( active ) {
                try {
                    ConnectionThread conn = new ConnectionThread ( HttpServer, ss, ss.accept () );
                    conn.start ( );
                    HttpServer.thread_started ();
                } catch (java.io.IOException e) {
                    HttpServer.Log ("IO exception: "+ e + "\n");
                    active= false;
                }
            }
        }
}
