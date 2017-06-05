package test.EasyServer;
// Created by LJF on 2017/6/1. 

import jdk.nashorn.internal.runtime.regexp.joni.constants.OPSize;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiWebServer {

    public static void main(String[] args) {

        String baseUrl = "E:/jee-mars/project/HTTPServerEnd/WebRoot/index.html";

        while (true) {
            try {
                ServerSocket serverSocket = new ServerSocket(80);
                System.out.println("正在请求中...");

                Socket socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = br.readLine();
                System.out.println(line);

                int len = 0;
                byte[] buff = new byte[1024];
                FileInputStream fis = new FileInputStream(baseUrl);
                OutputStream os = socket.getOutputStream();
                while((len = fis.read(buff)) != -1){
                    os.write(buff, 0, len);
                }
                os.flush();

                is.close();
                br.close();
                fis.close();
                os.close();
                socket.close();
                serverSocket.close();
            } catch (Exception e) {

            }
        }
    }
}