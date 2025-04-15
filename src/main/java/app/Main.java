package app;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;
import controller.*;
import util.HibernateUtil;
import org.apache.catalina.servlets.DefaultServlet;

public class Main {
    public static void main(String[] args) throws Exception {
        // Test Hibernate initialization
        System.out.println("Initializing Hibernate...");
        HibernateUtil.getSessionFactory(); // This should create tables if configured correctly
        System.out.println("Hibernate initialized");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8088);

        String projectRoot = new File(".").getCanonicalPath();
        String docBase = new File(projectRoot, "src/main/webapp").getAbsolutePath();
        File docBaseFile = new File(docBase);
        System.out.println("Project Root: " + projectRoot);
        System.out.println("DocBase: " + docBase);
        System.out.println("DocBase exists: " + docBaseFile.exists());
        System.out.println("DocBase is directory: " + docBaseFile.isDirectory());

        // Set up the Tomcat context for the project
        Context context = tomcat.addContext("", docBase);
        context.addWelcomeFile("/index.html");

        // Add DefaultServlet to serve static files
        Tomcat.addServlet(context, "default", new DefaultServlet());
        context.addServletMappingDecoded("/*", "default");

        // Ensure static files are served properly
        context.setResources(new org.apache.catalina.webresources.StandardRoot(context));

        // Add servlets for the Online Medical Shop
        tomcat.addServlet("", "AuthServlet", "controller.AuthServlet");
        context.addServletMappingDecoded("/auth/*", "AuthServlet");

        tomcat.addServlet("", "MedicineServlet", "controller.MedicineServlet");
        tomcat.addServlet("", "CartServlet", "controller.CartServlet");
        tomcat.addServlet("", "OrderServlet", "controller.OrderServlet");
        context.addServletMappingDecoded("/medicines/*", "MedicineServlet");
        context.addServletMappingDecoded("/cart/*", "CartServlet");
        context.addServletMappingDecoded("/orders/*", "OrderServlet");

        // Ensure the connector is initialized
        tomcat.getConnector(); // This forces connector setup

        System.out.println("Starting Tomcat...");
        tomcat.start();
        System.out.println("Tomcat started on http://localhost:8088");

        // Await server requests
        tomcat.getServer().await();
    }
}
