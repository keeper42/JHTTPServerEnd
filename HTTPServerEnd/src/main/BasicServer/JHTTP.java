package main.BasicServer;
// Created by LJF on 2017/6/1. 

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class JHTTP extends Thread {

    private File documentRootDirectory;
    private String indexFileName;
    private ServerSocket server;
    private int numThreads = 50;    // The number of threads.

    /**
     * The constructor for the JHTTP class.
     * @param documentRootDirectory
     * @param port
     * @param indexFileName
     * @throws IOException
     */
    public JHTTP(File documentRootDirectory, int port , String indexFileName) throws IOException {
        if (!documentRootDirectory.isDirectory()) {
            throw new IOException(documentRootDirectory+" does not exist as a directory ");
        }
        this.documentRootDirectory = documentRootDirectory;
        this.indexFileName = indexFileName;
        this.server = new ServerSocket(port);
    }

    private JHTTP(File documentRootDirectory, int port)throws IOException {
        this(documentRootDirectory, port, "index.html");
    }

    @Override
    public void run(){
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(new RequestProcessor(documentRootDirectory, indexFileName));
            t.start();
        }

        System.out.println("Accepting connection on port " + server.getLocalPort());
        System.out.println("Document Root: " + documentRootDirectory);
        System.out.println("Index file: " + indexFileName);
        while (true) {
            try {
                Socket request = server.accept();
                RequestProcessor.processRequest(request);
            } catch (IOException e) {

            }
        }
    }

}
