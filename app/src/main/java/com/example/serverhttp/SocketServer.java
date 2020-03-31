package com.example.serverhttp;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class SocketServer extends Thread {

    private final Handler handler;
    private final CameraController cameraController;
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
        this.cameraController = new CameraController(camera);
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
                executorService.execute(new HttpServerThread(s, handler, sem, this.cameraController));
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
