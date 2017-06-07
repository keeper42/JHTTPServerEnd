package main.FileServer;
// Created by LJF on 2017/6/7.

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class FileUploadHandler implements HttpHandler{

    private String path;

    public FileUploadHandler(String webroot) {
        this.path = webroot;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        FileUploadUtil fileUploadUtil = new FileUploadUtil();
        fileUploadUtil.flieUpload(httpExchange, path);
    }

}
