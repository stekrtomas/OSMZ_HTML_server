package com.example.serverhttp;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpServerThread implements Runnable {

    private final Handler handler;
    private Semaphore sem;
    private CameraServer cameraServer;
    private Socket s;


    public HttpServerThread(Socket socket, Handler handler, Semaphore sem, CameraServer cameraServer) {
        this.s = socket;
        this.handler = handler;
        this.sem = sem;
        this.cameraServer = cameraServer;
        Log.d("SERVER", "Start vlákna HttpServerThread!");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        Boolean noBlocked = sem.tryAcquire();
        try {
            OutputStream out = s.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            String tmp = in.readLine();

            if (tmp == null || tmp == "") {
                s.close();
                return;
                //continue;

            }

            if (noBlocked) {
                Pattern pattern = Pattern.compile("GET (.+) HTTP.*");
                Matcher matcher = pattern.matcher(tmp);

                String path = "/";
                while (matcher.find()) {
                    path = matcher.group(1);
                }
                //Log.d("SERVER", "Rozparsovana cesta z GETu: " + path);

                while (!tmp.isEmpty()) {
                    Log.d("SERVER", "HTTP REQ:" + tmp);
                    tmp = in.readLine();
                }

                String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String pathFile = sdPath + path;

                // Log.d("SERVER", "Cesta k otevreni souboru:  " + pathFile);

                File toOpen = new File(pathFile);


                if (path.equals("/camera")) {
                    this.cameraServer.addSocket(out, this.s);
                } else if (path.startsWith("/cgi")) {
                       String splitted[] =  path.split("/");
                    splitted = Arrays.copyOfRange(splitted, 2, splitted.length);
                    ProcessBuilder pb = new ProcessBuilder(splitted);
                    try {
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        String body = "";
                        InputStreamReader stream = new InputStreamReader(process.getInputStream());
                        int ch;
                        StringBuilder sb = new StringBuilder();
                        while((ch = stream.read()) != -1){
                            sb.append((char)ch);
                        }
                        body = sb.toString();

                        process.waitFor(10, TimeUnit.SECONDS);
                        String response = makeHeader200(toOpen, body.length()) + body;
                        out.write(response.getBytes());
                        out.flush();
                    } catch (Exception ex) {
                        String body = "Error in command: " + String.join(",", splitted) + " \n " + ex.getLocalizedMessage();

                        String response = "";
                        response = makeHeader200(toOpen, body.length()) + body;
                        out.write(response.getBytes());
                        out.flush();
                    }
                    s.close();
                } else {
                    if (toOpen.exists()) {
                        //cesta k souboru nebo adresáři existuje

                        if (toOpen.isFile()) {
                            //pokud se jedná o soubor tak ho vypiš

                            out.write(makeHeader200(toOpen, toOpen.length()).getBytes());
                            out.write(Files.readAllBytes(toOpen.toPath()));
                            bundle.putString("LOG", pathFile + "\nPocet bitu: " + String.valueOf(toOpen.length()) + "\n");
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                            out.flush();
                        } else {
                            //pokud se nejedná o soubor tak se vypíše obsah adresáře

                            File[] files = toOpen.listFiles();
                            if (path.equals("/")) {
                                path = "";
                            }

                            String content = "<html>\n" +
                                    "<head><meta charset='UTF-8'></head>\n" +
                                    "<body>\n" +
                                    "<h1>Výpis obsahu adresáře:</h1>\n";

                            for (File file : files
                            ) {
                                String li = "<li><a href='" + path + "/" + file.getName() + "'>" + file.getName() + "</a></li> \n";
                                content += li;
                            }

                            content += "</body>\n" +
                                    "</html>\n";

                            String ok = makeHeader200(toOpen, content.length());
                            ok += content;
                            bundle.putString("LOG", pathFile + "\nPocet bitu: " + String.valueOf(content.length()) + "\n");

                            msg.setData(bundle);
                            handler.sendMessage(msg);
                            out.write(ok.getBytes());
                            out.flush();
                        }
                    } else {
                        //cesta k souboru nebo adresáři neexistuje (vypíšeme error 404)

                        out.write(makeHeader404().getBytes());
                        out.flush();
                    }
                }
                if (!path.equals("/camera")) {
                    s.close();
                }
            } else {
                out.write(makeHeader500().getBytes());
                out.flush();
                s.close();
            }
            Log.d("SERVER", "Socket Closed");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (noBlocked) {
                sem.release();
            }
        }


    }

    private String makeHeader500() {
        Log.d("SERVER", "Server too busy 500!");
        String content = "<html>\n" +
                "<head><meta charset='UTF-8'></head>\n" +
                "<body>\n" +
                "<h1>Server is too busy 500!</h1>\n" +
                "</body>\n" +
                "</html>";
        String serverBusy = "HTTP/1.1 500 Internal Server Error\n" +
                "Date: Mon, 27 Jul 2009 12:28:53 GMT\n" +
                "Server: Apache/2.2.14 (Win32)\n" +
                "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT\n" +
                "Content-Length: " + content.length() + "\n" +
                "Content-Type: text/html; charset=utf-8\n\n" + content;
        return serverBusy;
    }

    private String makeHeader404() {
        //cesta k souboru nebo adresáři neexistuje (vypíšeme error 404)
        Log.d("SERVER", "OdpověĎ serveru 404 File not Found");
        //vypsání odpovědi 404 Chyba
        String content = "<html>\n" +
                "<head><meta charset='UTF-8'></head>\n" +
                "<body>\n" +
                "<h1>File not found 404!</h1>\n" +
                "</body>\n" +
                "</html>";
        String notFound = "HTTP/1.1 404 Not Found\n" +
                "Date: Mon, 27 Jul 2009 12:28:53 GMT\n" +
                "Server: Apache/2.2.14 (Win32)\n" +
                "Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT\n" +
                "Content-Length: " + content.length() + "\n" +
                "Content-Type: text/html; charset=utf-8\n\n" + content;
        return notFound;
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
                "Content-Type:" + mimeType + "; charset=utf-8 \n" +
                "\n";
        return ok;
    }


}
