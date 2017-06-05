package main.RedirectServer;
// Created by LJF on 2017/6/3.

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class Redirector implements Runnable {

    private int port;
    private String newSite;

    public Redirector(String site, int port){
        this.port = port;
        this.newSite = site;
    }

    @Override
    public void run() {
        try {
            ServerSocket server=new ServerSocket(port);
            System.out.println("Redirecting connection on port "  + server.getLocalPort() + " to "+newSite);

            // Create new sockets and threads.
            while (true) {
                try {
                    Socket socket = server.accept();
                    Thread thread = new RedirectThread(socket);
                    thread.start();
                } catch (IOException e) {
                }
            }
        } catch (BindException e) {
            System.err.println("Could not start server. Port Occupied");
        }catch (IOException e) {
            System.err.println(e);
        }

    }

    /**
     * The redirection of the thread class
     */
    class RedirectThread extends Thread {

        private Socket socket;

        RedirectThread(Socket s) {
            this.socket = s;
        }

        public void run() {
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"ASCII"));
                InputStreamReader isr = new InputStreamReader(new BufferedInputStream(socket.getInputStream()));

                // Read the requests in the input stream.
                StringBuffer request = new StringBuffer(80);
                while (true) {
                    int ch = isr.read();
                    if (ch == '\t' || ch == '\n' || ch == -1) {
                        break;
                    }
                    request.append((char)ch);
                }

                String req = request.toString();
                System.out.println(req);

                // Intercept the string and parse the request file.
                int firstSpace = req.indexOf(' ');
                int secondSpace = req.indexOf(' ', firstSpace+1);
                String requestFile = req.substring(firstSpace+1, secondSpace);

                // Judging whether there is HTTP in the request.
                if (req.indexOf("HTTP") != -1) {
                    bw.write("HTTP/1.0 302 FOUND\r\n");
                    Date now = new Date();
                    bw.write("Date: " + now + "\r\n");
                    bw.write("Server: Redirector 1.0\r\n");
                    bw.write("Location: " + newSite + requestFile + "\r\n");
                    bw.write("Content-Type: text/html\r\n\r\n");
                    bw.flush();
                }

                // Not all browsers support redirection, so we need to generate an HTML file for all browsers to describe this behavior
                bw.write("<HTML><HEAD><TITLE>Document moved</TITLE></HEAD>\r\n");
                bw.write("<BODY><H1>Document moved</H1></BODY>\r\n");
                bw.write("The document " + requestFile
                        + " has moved to \r\n<A HREF=\"" + newSite + requestFile+"\">"
                        + newSite + requestFile + "</A>.\r\n Please update your bookmarks");
                bw.write("</BODY></HTML>\r\n");
                bw.flush();
            } catch (IOException e) {
            } finally{
                try {
                    if (socket!=null) {
                        socket.close();
                    }
                } catch (IOException e2) {

                }
            }
        }
    }
}
