package main.BasicServer;
// Created by LJF on 2017/6/1. 

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

        String root = documentRootDirectory.getPath();

        while (true) {
            Socket connection;
            synchronized (pool) {
                while (pool.isEmpty()) {
                    try {
                        pool.wait();
                    } catch (InterruptedException e) {
                    }
                }
                connection = (Socket)pool.remove(0);
            }

            try {
                String fileName;
                String contentType;
                BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
                OutputStreamWriter out = new OutputStreamWriter(bos);
                InputStreamReader isr = new InputStreamReader(new BufferedInputStream(connection.getInputStream()), "ASCII");

                StringBuffer request = new StringBuffer(80);
                while (true) {
                    int ch = isr.read();
                    if (ch == '\t' || ch == '\n' || ch == -1) {
                        break;
                    }
                    request.append((char) ch);
                }

                // Print the request log.
                String req = request.toString();
                System.out.println(req);

                String version = "";
                Date now = new Date();
                StringTokenizer st = new StringTokenizer(req);
                String method = st.nextToken();

                // Judging whether the request is a get request.
                if (method == "GET" || method.contains("GET")) {

                    System.out.println("The request was successful");

                    fileName = st.nextToken();
                    if (fileName.endsWith("/")) {
                        fileName += indexFileName;
                    }
                    contentType = guessContentTypeFromName(fileName);
                    if (st.hasMoreTokens()) {
                        version = st.nextToken();
                    }

                    // Create a new request file and load it into the DataInputStream.
                    File requestFile = new File(documentRootDirectory,fileName.substring(1,fileName.length()));
                    if (requestFile.canRead() && requestFile.getCanonicalPath().startsWith(root)) {
                        DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream(requestFile)));
                        byte[] requestData = new byte[(int)requestFile.length()];
                        fis.readFully(requestData);

                        // Create the response and load it into the OutputStreamWriter, and so on.
                        if (version.startsWith("HTTP")) {
                            out.write("HTTP/1.1 200 OK\r\n");
                            out.write("Date: " + now + "\r\n");
                            out.write("Server: JHTTP 1.1\r\n");
                            out.write("Content-length: " + requestData.length + "\r\n");
                            out.write("Content-Type: " + contentType + "\r\n\r\n");
                            out.flush();
                        }
                        bos.write(requestData);
                        bos.flush();
                        bos.close();
                        fis.close();
                    }else {
                        if (version.startsWith("HTTP")) {
                            out.write("HTTP/1.1 404 File Not Found\r\n");
                            out.write("Date: " + now + "\r\n");
                            out.write("Server: JHTTP 1.1\r\n");
                            out.write("Content-Type: text/html\r\n\r\n");
                            out.flush();
                        }
                        out.write("<HTML>\r\n");
                        out.write("<HEAD><TITLE>File Not Found</TITLE></HRAD>\r\n");
                        out.write("<BODY>\r\n");
                        out.write("<H1>HTTP Error 404: File Not Found</H1>");
                        out.write("</BODY></HTML>\r\n");
                    }
                }else {
                    if (version.startsWith("HTTP")) {
                        out.write("HTTP/1.1 501 Not Implemented\r\n");
                        out.write("Date: " + now + "\r\n");
                        out.write("Server: JHTTP 1.1\r\n");
                        out.write("Content-Type: text/html\r\n\r\n");
                        out.flush();
                    }
                    out.write("<HTML>\r\n");
                    out.write("<HEAD><TITLE>Not Implemented</TITLE></HRAD>\r\n");
                    out.write("<BODY>\r\n");
                    out.write("<H1>HTTP Error 501: Not Implemented</H1>");
                    out.write("</BODY></HTML>\r\n");
                }
                out.close();
            } catch (IOException e) {

            }finally{
                try {
                    connection.close();
                } catch (IOException e2) {

                }
            }
        }
    }

    /**
     * Judging the type of contentype.
     * @param fileName
     * @return
     */
    public static String guessContentTypeFromName(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        }else if (fileName.endsWith(".txt") || fileName.endsWith(".java")) {
            return "text/plain";
        }else if (fileName.endsWith(".gif")) {
            return "image/gif";
        }else if (fileName.endsWith(".class")) {
            return "application/octet-stream";
        }else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }else {
            return "text/plain";
        }
    }

}
