package com.example.serverhttp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class SocketServer extends Thread {

    private final Handler handler;
    private final CameraServer cameraServer;
    ExecutorService executorService = Executors.newCachedThreadPool();
    ServerSocket serverSocket;
    public final int port = 12345;
    boolean bRunning;
    Semaphore sem;
    private final Context applicationContext;
    private final Camera camera;

    public SocketServer(Handler handler, int threadCount, Context applicationContext, Camera camera) throws IOException, InterruptedException {
        this.handler = handler;
        this.sem = new Semaphore(threadCount);
        this.applicationContext = applicationContext;
        this.camera = camera;
        this.cameraServer = new CameraServer(camera);
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d("SERVER", "Error, probably interrupted in accept(), see log");
            e.printStackTrace();
        }
        bRunning = false;
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
                executorService.execute(new HttpServerThread(s, handler, sem, this.cameraServer ));
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
