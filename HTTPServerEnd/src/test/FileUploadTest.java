package test;
// Created by LJF on 2017/6/8. 

import com.sun.net.httpserver.HttpServer;
import main.FileServer.FileUploadHandler;

import java.net.InetSocketAddress;

public class FileUploadTest {

    public static void main (String[] args) {

//        server.createContext("/", new MyHandler());
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0);
            server.createContext("/", new FileUploadHandler("webroot"));
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
