package main;
// Created by LJF on 2017/6/4. 

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a class that parses the configuration file
 */
public class ConfigParser {

    private int port;
    private String indexFile;
    private String docRoot;
    private int redirectPort;
    private String redirectSite;

    public int getPort(){
        return port;
    }

    public String getIndexFile(){
        return indexFile;
    }

    public String getDocRoot(){
        return docRoot;
    }

    public int getRedirectPort(){
        return redirectPort;
    }

    public String getRedirectSite(){
        return redirectSite;
    }

    /**
     * This is the constructor of the parses class, and the parsing is done in this constructor
     * @param configFileName
     */
    public ConfigParser(String configFileName){
        try{
            // Read the config file and parse its contents
            File configFile = new File(configFileName);
            FileInputStream fis = new FileInputStream(configFile);
            InputStreamReader isr = new InputStreamReader(fis);
            int ch = 0;
            String config = "";
            while((ch = isr.read()) != -1){
                config += String.valueOf((char)ch);
            }

            String httpRegEx = "\\bhttp[\\s\\S]*";
            Matcher matcher = Pattern.compile(httpRegEx).matcher(config);
            String httpStr = "";
            if(matcher.find()){
                httpStr = matcher.group();
            }

            String serverRegEx = "\\bserver[\\s\\S]*";
            matcher = Pattern.compile(serverRegEx).matcher(httpStr);
            String serverStr = "";
            if(matcher.find()){
                serverStr = matcher.group();
            }

            String listenRegEx = "\\blisten\\s[0-9]*";
            matcher = Pattern.compile(listenRegEx).matcher(serverStr);
            String listenStr = "";
            if(matcher.find()){
                listenStr = matcher.group();
            }

            String portRegEx = "\\d+";
            matcher = Pattern.compile(portRegEx).matcher(listenStr);
            String portStr = "";
            if(matcher.find()){
                portStr = matcher.group();
            }
            this.port = Integer.parseInt(portStr);

            String locationRegEx = "\\blocation[\\s\\S]*";
            matcher = Pattern.compile(locationRegEx).matcher(serverStr);
            String locationStr = "";
            if(matcher.find()){
                locationStr = matcher.group();
            }

            String rootRegEx = "\\broot\\s[a-z]*";
            matcher = Pattern.compile(rootRegEx).matcher(locationStr);
            String rootStr = "";
            if(matcher.find()){
                rootStr = matcher.group();
            }

            String[] rootArray = rootStr.split("\\s");
            this.docRoot = rootArray[1];

            String indexRegEx = "\\bindex\\s[\\S]*\\s[\\S]*";
            matcher = Pattern.compile(indexRegEx).matcher(locationStr);
            String indexStr = "";
            if(matcher.find()){
                indexStr = matcher.group();
            }

            String[] indexFileArray = indexStr.split("\\s");
            this.indexFile = indexFileArray[1];

            String proxyRegEx = "(\\s|\\S)\\bproxy[\\s\\S]*\\berror";
            matcher = Pattern.compile(proxyRegEx).matcher(locationStr);
            String proxyStr = "";
            if(matcher.find()){
                proxyStr = matcher.group();
            }
            String[] proxyArray = proxyStr.split(";");
            String proxyPathStr = proxyArray[0];
            String proxyHostStr = proxyArray[1];
            String proxyPortStr = proxyArray[2];

            if(!proxyPortStr.contains("#")){
                String proxyPortRegEx = "\\d+";
                matcher = Pattern.compile(proxyPortRegEx).matcher(proxyPortStr);
                String proxyPort = "";
                if(matcher.find()){
                    proxyPort = matcher.group();
                }
                this.redirectPort = Integer.parseInt(proxyPort);
            }

            if(!proxyPathStr.contains("#")){
                String[] proxyPathArray = proxyPathStr.split("\\s");
                this.redirectSite = proxyPathArray[2];
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
