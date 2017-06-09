package main.BasicServer;
// Created by LJF on 2017/6/1.
// re-created by CB on 2017/6/7.

import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;


public class RequestProcessor implements Runnable {

    private File documentRootDirectory;
    private String indexFileName;
    private static List pool = new LinkedList();

    /**
     * Handle the constructor of the request class.
     * @param documentRootDirectory
     * @param indexFileName
     */
    public RequestProcessor(File documentRootDirectory,String indexFileName) {

        // Judging whether the file exists.
        if (documentRootDirectory.isFile()) {
            throw new IllegalArgumentException();
        }
        this.documentRootDirectory = documentRootDirectory;
        try {
            this.documentRootDirectory = documentRootDirectory.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (indexFileName != null) {
            this.indexFileName=indexFileName;
        }
    }

    public RequestProcessor(File documentRootDirectory){
        this(documentRootDirectory, "index.html");
    }

    // The pool of socket request.
    public static void processRequest(Socket request) {
        synchronized (pool) {
            pool.add(pool.size(),request);
            pool.notifyAll();
        }
    }

    @Override
    public void run() {
        while (true) {
            Socket connection;
            String requestLine = null;
            String requestHeader = null;
            BufferedReader br = null;

            // Get connection
            synchronized (pool) {
                while (pool.isEmpty()) {
                    try {
                        pool.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                connection = (Socket) pool.remove(0);
            }

            try {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                // Get the Request Line
                requestLine = br.readLine();
                System.out.println("Get request. Here is the Request Line:\n" + requestLine + "\n");

                // Get the Request Header
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (!(line==null || line.equals("") || line.equals("\r\n"))) {
                    //System.out.println(line);
                    sb.append(line);
                    sb.append("\r\n");
                    line = br.readLine();
                }
                requestHeader = sb.toString();
                System.out.println("Here is the request header:\n" + requestHeader);

                // Deal with the request
                if(requestLine != null) {
                    if (requestLine.startsWith("GET"))
                        get(connection, requestLine);
                    else if (requestLine.startsWith("POST"))
                        post(connection, requestHeader, br);
                    else
                        error501(connection);
                    System.out.println("Respond Complete.");
                    System.out.println("Socket closed.");
                    System.out.println("--------------------------------------------------------------------------\n");
                }

                connection.close();

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // Deal with the GET Request
    private void get(Socket socket, String requestLine){
        String fileName;
        String contentType;
        Date now = new Date();
        String root = documentRootDirectory.getPath();

        // get the File Name
        String[] parts = requestLine.split(" ");
        fileName = parts[1];
        if (fileName.endsWith("/")) {
            fileName += indexFileName;
        }

        // get the Content Type
        if (fileName.endsWith("html") || fileName.endsWith("htm"))
            contentType = "text/html";
        else if (fileName.endsWith("jpg") || fileName.endsWith("jpeg"))
            contentType = "image/jpeg";
        else if (fileName.endsWith("gif"))
            contentType = "image/gif";
        else if (fileName.endsWith("txt"))
            contentType = "text/plain";
        else if (fileName.endsWith(".class"))
            contentType = "application/octet-stream";
        else
            contentType = "text/plain";

        // send the response
        try {
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            File requestFile = new File(documentRootDirectory,fileName.substring(1,fileName.length()));
            // successfully find the file, and send
            if (requestFile.canRead()){
                DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream(requestFile)));
                byte[] requestData = new byte[(int)requestFile.length()];
                fis.readFully(requestData);
                // send the status line and header
                osw.write("HTTP/1.1 200 OK\r\n");
                osw.write("Date: " + now + "\r\n");
                osw.write("Server: JHTTP 1.1\r\n");
                osw.write("Content-length: " + requestData.length + "\r\n");
                osw.write("Content-Type: " + contentType + "\r\n\r\n");
                osw.flush();
                // send the body, the required file
                bos.write(requestData);
                bos.flush();
                fis.close();
                System.out.println("Successfully sent the file.");
            }
            // fail to find the file, send 404 error page
            else {
                String errorResponseHead = "";
                errorResponseHead += "HTTP/1.1 404 File Not Found\r\n";
                errorResponseHead += "Date: " + now + "\r\n";
                errorResponseHead += "Server: JHTTP 1.1\r\n";
                errorResponseHead += "Content-Type: text/html\r\n\r\n";
                String errorResponseBody = "";
                errorResponseBody += "<!DOCTYPE html>\r\n";
                errorResponseBody += "<html>\r\n";
                errorResponseBody += "<head><title>File Not Found</title></head>\r\n";
                errorResponseBody += "<body>\r\n";
                errorResponseBody += "<h1>HTTP Error 404: File Not Found</h1>\r\n";
                errorResponseBody += "</body>\r\n</html>";
                osw.write(errorResponseHead);
                osw.write(errorResponseBody);
                osw.flush();
                System.out.println("Sent the 404 Error Page");
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // Deal with the POST Request
    private void post(Socket socket, String requestHeader, BufferedReader br) {
        String requestBody;
        String boundary;
        int contentLength = 0;
        Date now = new Date();
        String[] requestHeadLines = requestHeader.split("\r\n"); //split the request head into lines

        // Analysis the request head
        for(int i=0; i<requestHeadLines.length; i++){
            if (requestHeadLines[i].contains("Content-Length")) {
                String length = requestHeadLines[i].substring(requestHeadLines[i].indexOf("Content-Length") + 16);
                contentLength = Integer.parseInt(length);
                System.out.println("Get Content Length: " + contentLength);
            }

            // If it is a file upload
            else if(requestHeadLines[i].contains("multipart/form-data")){
                // Get multiltipart boundary
                boundary = requestHeadLines[i].substring(requestHeadLines[i].indexOf("boundary") + 9);
                System.out.println("Get boundary: " + boundary);
                // 上传文件
                doMultiPart(br, socket, boundary, contentLength, now);
                return;
            }
        }

        // If it is not a file upload
        try {
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            System.out.println("begin reading posted data......");

            StringBuilder sb = new StringBuilder();
            while (br.ready()){
                sb.append((char)br.read());
            }
            requestBody = sb.toString();
            System.out.println("Here is the request body:\n" + requestBody);

            // send the response
            String responseHead = "";
            responseHead += "HTTP/1.1 200 OK\n";
            responseHead += "Date: " + now + "\r\n";
            responseHead += "Server: JHTTP 1.1\r\n";
            responseHead += "Content-Type: text/html\r\n\r\n";
            String responseBody = "";
            responseBody += "<HTML>\r\n";
            responseBody += "<HEAD><TITLE>Test POST</TITLE></HEAD>\r\n";
            responseBody += "<BODY>\r\n";
            responseBody += "<p>Post Successfully:</p>\r\n";
            responseBody += "<p>" + requestBody +"</p>\r\n";
            responseBody += "</BODY></HTML>\r\n";
            osw.write(responseHead);
            osw.write(responseBody);
            osw.flush();
            System.out.println("Successfully get the post and sent a response.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deal with file upload
    private void doMultiPart(BufferedReader br, Socket socket, String boundary, int contentLength, Date now){
        System.out.println("doMultiPart ......");
        String dataString = null;
        String fileName = null;

        /*下面的注释是一个浏览器发送带附件的请求的全文，所有中文都是说明性的文字*****
        <HTTP头部内容略>
        ............
        Cache-Control: no-cache
        <这里有一个空行，表明接下来的内容都是要提交的正文>
        -----------------------------7d925134501f6<这是multipart分隔符>
        Content-Disposition: form-data; name="myfile"; filename="mywork.doc"
        Content-Type: text/plain

        <附件正文>........................................
        .................................................

        -----------------------------7d925134501f6<这是multipart分隔符>
        Content-Disposition: form-data; name="myname"<其他字段或附件>
        <这里有一个空行>
        <其他字段或附件的内容>
        -----------------------------7d925134501f6--<这是multipart分隔符，最后一个分隔符多两个->
        ****************************************************************/
        /**
         * 上面的注释是一个带附件的multipart类型的POST的全文模型
         * 要把附件去出来，就是要找到附件正文的起始位置和结束位置
         * **/
        if (contentLength != 0) {

            //把所有的提交的正文，包括附件和其他字段都先读到字符串
            char[] chs = new char[contentLength];
            int totalRead;
            try {
                StringBuilder sb = new StringBuilder();
                totalRead = br.read(chs, 0, contentLength);
                sb.append(chs);
                dataString = sb.toString();
                System.out.println("totalread: " + totalRead);
            }catch (NullPointerException | IOException e){
                e.printStackTrace();
            }

            System.out.println("the data user posted:\n" + dataString);
            int pos = dataString.indexOf(boundary);
            //以下略过4行就是第一个附件的位置
            pos = dataString.indexOf("\r\n", pos) + 1;
            pos = dataString.indexOf("\r\n", pos) + 1;
            pos = dataString.indexOf("\r\n", pos) + 1;
            pos = dataString.indexOf("\r\n", pos) + 1;
            //附件开始位置
            int start = dataString.substring(0, pos).getBytes().length;
            pos = dataString.indexOf(boundary, pos) - 4;
            //附件结束位置
            int end = dataString.substring(0, pos).getBytes().length;
            //以下找出filename
            int fileNameBegin = dataString.indexOf("filename") + 10;
            int fileNameEnd = dataString.indexOf("\r\n", fileNameBegin);
            fileName = dataString.substring(fileNameBegin, fileNameEnd - 1);

            System.out.println("Get filename: " + fileName);
            /*
             * 有时候上传的文件显示完整的文件名路径,比如c:/my file/somedir/project.doc
             * 但有时候只显示文件的名字，比如myphoto.jpg.
             * 所以需要做一个判断。
             */
            if(fileName.contains("/")){
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            }
            try {
                byte[] buf = dataString.getBytes();
                OutputStream fileOut = new FileOutputStream(System.getProperty("user.dir") + "/uploads/" + fileName);
                fileOut.write(buf, start, end - start);
                fileOut.close();
                System.out.println("file saved: " + System.getProperty("user.dir") + "/uploads/" + fileName);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        // send the response
        try {
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            String responseHead = "";
            responseHead += "HTTP/1.1 200 OK\r\n";
            responseHead += "Date: " + now + "\r\n";
            responseHead += "Server: JHTTP 1.1\r\n";
            responseHead += "Content-Type: text/html\r\n";
            responseHead += "Accept-ranges: bytes\r\n\r\n";
            String responseBody = "";
            responseBody += "<HTML>\r\n";
            responseBody += "<HEAD><TITLE>Test POST</TITLE></HEAD>\r\n";
            responseBody += "<BODY>\r\n";
            responseBody += "<p>Post Successfully:</p>\r\n";
            responseBody += "<p>" + fileName +"</p>\r\n";
            responseBody += "</BODY></HTML>\r\n";
            osw.write(responseHead);
            osw.write(responseBody);
            osw.flush();
            System.out.println("Successfully get the post and sent a response.");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // Deal with other request / Errorr501
    private void error501(Socket socket){
        Date now = new Date();
        try {
            BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
            OutputStreamWriter osw = new OutputStreamWriter(bos);
            osw.write("HTTP/1.1 501 Not Implemented\r\n");
            osw.write("Date: " + now + "\r\n");
            osw.write("Server: JHTTP 1.1\r\n");
            osw.write("Content-Type: text/html\r\n\r\n");
            osw.flush();
            osw.write("<HTML>\r\n");
            osw.write("<HEAD><TITLE>Not Implemented</TITLE></HEAD>\r\n");
            osw.write("<BODY>\r\n");
            osw.write("<H1>HTTP Error 501: Not Implemented</H1>");
            osw.write("</BODY></HTML>\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
