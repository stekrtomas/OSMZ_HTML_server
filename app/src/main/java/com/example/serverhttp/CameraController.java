package com.example.serverhttp;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public  class CameraController {
    private  ArrayList<SocketHolder> sockets;
    private  Semaphore shootSemaphore;
    private  Camera camera;
    private SurfaceTexture surfaceTexture;
    PreviewCallback picturePreviewCallback;

    private void sendPhoto(byte[] data) throws IOException {
        ArrayList<SocketHolder> socketsToRemove = new ArrayList<>();
        synchronized (this) {
            for (SocketHolder holder : sockets
            ) {
                try {
                    OutputStream out = holder.getOutputStream();
                    out.write("--frame\n".getBytes());
                    out.write("Content-Type: image/jpeg\n\n".getBytes());
                    holder.getOutputStream().write(data);
                    holder.getOutputStream().flush();
                } catch (IOException e) {
                    socketsToRemove.add(holder);
                }
            }

            for (SocketHolder holder : socketsToRemove
            ) {
                holder.getSocket().close();
                holder.getOutputStream().close();
                holder.getInputStream().close();
            }
            this.sockets.removeAll(socketsToRemove);
        }
    }

    public void addSocket(OutputStream out, Socket socket) throws IOException {
        String header = "HTTP/1.1 200 OK\nDate: Mon, 27 Jul 2009 12:28:53 GMT\nServer: Apache/2.2.14 (Win32)\nLast-Modified: Wed, 22 Jul 2009 19:15:56 GMT\nContent-Type: multipart/x-mixed-replace; boundary=frame\n\n";
        out.write(header.getBytes());
        synchronized (this) {
            InputStream inputStream = socket.getInputStream();
            this.sockets.add(new SocketHolder(socket, out, inputStream));
        }
    }

    public CameraController(Camera camera) throws InterruptedException, IOException {
        this.camera = camera;
        this.sockets = new ArrayList<>();
        surfaceTexture = new SurfaceTexture(0);
        this.shootSemaphore = new Semaphore(1);
        picturePreviewCallback = (PreviewCallback) (new PreviewCallback() {
            public final void onPreviewFrame(final byte[] data, Camera cammera) {
                Thread th = new Thread(() -> {
                    if (CameraController.this.shootSemaphore.tryAcquire()) {
                        Parameters parameters = CameraController.this.camera.getParameters();
                        int width = parameters.getPreviewSize().width;
                        int height = parameters.getPreviewSize().height;
                        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, (int[]) null);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        yuv.compressToJpeg(new Rect(0, 0, width, height), 90, (OutputStream) out);
                        try {
                            CameraController.this.sendPhoto(out.toByteArray());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        CameraController.this.shootSemaphore.release();
                    }
                });
                th.start();
            }
        });
        this.camera.setPreviewTexture(surfaceTexture);
        Thread.sleep(600L);
        this.camera.setPreviewCallback(picturePreviewCallback);
        Thread.sleep(600L);
        this.camera.startPreview();
    }
}
