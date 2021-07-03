/**
 * 
 */
package com.int2.expeshare.test;

/**
 * @author skminh9
 *
 */
import java.util.Vector;

public class HelloHandler {

  HelloHandler() {
    System.out.println("Handler: default constructor called.");
  }
  
  public Vector sayHello() {
    Vector results = new Vector();
    results.addElement("Hello, anonymous!");
    return results;
  }

  public Vector sayHello(String name) {
    Vector results = new Vector();
    results.addElement("Hello, " + name + "!");
    return results;
  }

} // End of class HelloHandler.
