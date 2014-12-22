package com.crunchify.tutorials;
 
import javax.servlet.*;
import javax.servlet.http.HttpServlet;
 
/**
 * @author Crunchify.com
 */
 
@SuppressWarnings("serial")
public class CrunchifyServletExample extends HttpServlet
{
 
    public void init() throws ServletException
    {
          System.out.println("----------");
          System.out.println("---------- CrunchifyExample Servlet Initialized successfully ----------");
          System.out.println("----------");
 
          System.out.println("\nApp Deployed Directory path: " + this.getServletContext().getRealPath("/"));
          System.out.println("getContextPath(): " + this.getServletContext().getContextPath());
          System.out.println("Apache Tomcat Server: " + this.getServletContext().getServerInfo());
          System.out.println("Servlet API version: " + this.getServletContext().getMajorVersion() + "." +this.getServletContext().getMinorVersion());
          System.out.println("Tomcat Project Name: " + this.getServletContext().getServletContextName());
    }
}