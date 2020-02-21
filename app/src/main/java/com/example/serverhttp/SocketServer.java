package com.example.serverhttp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class SocketServer extends Thread {

    ServerSocket serverSocket;
    public final int port = 12345;
    boolean bRunning;

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
    }

    private String makeHeader404(){
        //cesta k souboru nebo adresáři neexistuje (vypíšeme error 404)
        Log.d("SERVER", "OdpověĎ serveru 404 File not Found");
        //vypsání odpovědi 404 Chyba
        String content = "<html>\n" +
                "<head><meta charset='UTF-8'></head>\n"+
                "<body>\n" +
                "<h1>File not found 404!</h1>\n" +
                "</body>\n" +
                "</html>";
        String notFound = "HTTP/1.1 404 OK\n" +
                "Date: Mon, 27 Jul 2009 12:28:53 GMT\n" +
                "Server: Apache/2.2.14 (Win32)\n" +
                "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT\n" +
                "Content-Length: " + content.length() + "\n" +
                "Content-Type: text/html; charset=utf-8\n" + content;
        return  notFound;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String makeHeader200(File toOpen, long contentLength) throws IOException {
        Log.d("SERVER", "OdpověĎ serveru 200 OK");
        String mimeType = Files.probeContentType(toOpen.toPath());
        String ok = "HTTP/1.1 200 OK\n" +
                "Date: Mon, 27 Jul 2009 12:28:53 GMT\n" +
                "Server: Apache/2.2.14 (Win32)\n" +
                "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT\n" +
                "Content-Length: " + contentLength + "\n" +
                "Content-Type:" + mimeType +  "; charset=utf-8 \n" +
                "\n";
        return ok;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void run() {
        try {
            Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                Log.d("SERVER", "Socket Accepted");

                Thread thread = new Thread(new HttpServerThread(s));
                thread.run();
                /*
                OutputStream out = s.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                String tmp = in.readLine();
                if (tmp == null || tmp == "") {
                    s.close();
                    continue;

                }

                Pattern pattern = Pattern.compile("GET (.+) HTTP.*");
                Matcher matcher = pattern.matcher(tmp);

                String path = "/";
                while (matcher.find()) {
                    path = matcher.group(1);
                }
                Log.d("SERVER", "Rozparsovana cesta z GETu: " + path);

                while (!tmp.isEmpty()) {
                    Log.d("SERVER", "HTTP REQ:" + tmp);
                    tmp = in.readLine();
                }

                String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String pathFile = sdPath + path;

                Log.d("SERVER", "Cesta k otevreni souboru:  " + pathFile);

                File toOpen = new File(pathFile);

                /*if(path.equals("/")){
                    //pokud se ve složce nachází index.html tak jej vypíšeme
                    Log.d("SERVER", "OdpověĎ serveru 200 OK");
                    pathFile += "index.html";
                    File toOpenIndex = new File(pathFile);

                    String mimeType = Files.probeContentType(toOpenIndex.toPath());
                    //vypsání odpovědi 200 OK
                    String ok = "HTTP/1.1 200 OK\n" +
                            "Date: Mon, 27 Jul 2009 12:28:53 GMT\n" +
                            "Server: Apache/2.2.14 (Win32)\n" +
                            "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT\n" +
                            "Content-Length: " + toOpenIndex.length() + "\n" +
                            "Content-Type:" + mimeType + "\n" +
                            "\n";
                    out.write(ok.getBytes());
                    out.write(Files.readAllBytes(toOpenIndex.toPath()));
                    out.flush();
                }*/
                /*
                if (toOpen.exists()) {
                    //cesta k souboru nebo adresáři existuje

                    if(toOpen.isFile()){
                        //pokud se jedná o soubor tak ho vypiš

                        out.write(makeHeader200(toOpen,toOpen.length()).getBytes());
                        out.write(Files.readAllBytes(toOpen.toPath()));
                        out.flush();
                    }
                    else {
                        //pokud se nejedná o soubor tak se vypíše obsah adresáře

                        File[] files = toOpen.listFiles();
                        if(path.equals("/")){
                            path="";
                        }

                        String content = "<html>\n" +
                                "<head><meta charset='UTF-8'></head>\n"+
                                "<body>\n" +
                                "<h1>Výpis obsahu adresáře:</h1>\n";

                        for (File file : files
                        ) {
                            String li = "<li><a href='" +path+"/"+file.getName() + "'>" + file.getName() + "</a></li> \n";
                            content += li;
                        }

                        content += "</body>\n" +
                                "</html>\n";

                        String ok = makeHeader200(toOpen,content.length());
                        ok+=content;

                        out.write(ok.getBytes());
                        out.flush();
                    }
                } else {
                    //cesta k souboru nebo adresáři neexistuje (vypíšeme error 404)

                    out.write(makeHeader404().getBytes());
                    out.flush();
                }

                s.close();
                Log.d("SERVER", "Socket Closed");
                */
            }
        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
                Log.d("SERVER", "Normal exit");
            else {
                Log.d("SERVER", "Error");
                e.printStackTrace();
            }
        } finally {
            serverSocket = null;
            bRunning = false;
        }
    }

}
