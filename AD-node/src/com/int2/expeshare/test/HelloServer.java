package com.int2.expeshare.test;

import java.io.IOException;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpc;

import com.int2.expeshare.test.HelloHandler;

public class HelloServer {

  public static void main(String args[]) throws IOException {

    // Check if portnumber was supplied.
    //if (args.length < 1) {
    //  System.out.println("Usage: java HelloServer [port]");
    //  System.exit(-1);
    //}

    try {
      // Use the Apache Xerces SAX driver.
      XmlRpc.setDriver("uk.co.wilson.xml.MinML");

      // int port = Integer.parseInt(args[0]);
      int port = 9999;
      
      // Start the server.
      System.out.println("Server: starting XML-RPC server on port " + port );
      WebServer server = new WebServer(port);

      // Register our handler class.
      server.addHandler("hello", new HelloHandler());
      System.out.println("Server: registered HelloHandler class to 'hello'");
      System.out.println("Server: now accepting requests...");
    }

    catch (ClassNotFoundException e) {
      System.out.println("Server: could not locate Apache Xerces SAX driver.");
    }

  } // End of main().

} // End of class HelloServer.
