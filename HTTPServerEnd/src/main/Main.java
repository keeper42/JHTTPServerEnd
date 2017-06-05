package main;
// Created by LJF on 2017/6/4. 

import main.BasicServer.JHTTP;
import main.RedirectServer.Redirector;

import java.io.*;

public class Main {

    public static void main(String[] args) {

        // New parser class and parsing configuration file
        ConfigParser config = new ConfigParser("conf/config.txt");

        // Get redirect ports and sites
        int redirectPort = config.getRedirectPort();
        String redirectSite = config.getRedirectSite();

        // Judging whether there is a redirect server in the configuration file,
        // If not, then provide normal server.
        if(redirectPort != 0 && redirectSite != null){
            try {
                if (redirectSite.endsWith("/")) {
                    redirectSite = redirectSite.substring(0, redirectSite.length()-1);
                }
            } catch (Exception e) {
                System.out.println("Usage: java redirector occurred error.");
                return;
            }
            // Start server.
            new Thread(new Redirector(redirectSite, redirectPort)).start();

        } else if(redirectPort == 0 || redirectSite == null){
            try {
                int port = config.getPort();
                String fileName = config.getIndexFile();
                String docRootStr = config.getDocRoot();
                File docroot = new File(docRootStr);

                // Start server
                JHTTP webserver = new JHTTP(docroot, port, fileName);
                webserver.start();
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Usage: java JHTTP docroot port indexfile");
            } catch (IOException e) {
                System.out.println("Server could not start because of an "+e.getClass());
                System.out.println(e);
            }
        }

    }

}
