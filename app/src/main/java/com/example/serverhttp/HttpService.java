package com.example.serverhttp;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Messenger;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;

import java.io.IOException;

public final class HttpService extends IntentService {
    private final int NOTIFICATION_ID = 6969;
    private SocketServer s;
    public static boolean running = false;

    public Camera getCameraInstance() {
        Camera cam;
        try {
            cam = Camera.open();
        } catch (Exception var3) {
            cam = null;
        }

        return cam;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void onHandleIntent(Intent intent) {
        if (!running) {
            String channelId = this.createNotificationChannel();
            running = true;
            Builder notificationBuilder = new Builder((Context) this, channelId);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setCategory("service")
                    .build();
            this.startForeground(this.NOTIFICATION_ID, notification);
            int threads = intent.getIntExtra("threads", 1) ;
            try {
                this.s = new SocketServer(null, threads, this.getApplicationContext(), this.getCameraInstance());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            this.s.start();
            try {
                this.s.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void onDestroy() {
        if (this.s != null) {
            this.s.close();
        }
        running = false;
        try {
            if (this.s != null) {
                this.s.join();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        NotificationChannel chan = new NotificationChannel("http_service", "Http Service", NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(-16776961);
        chan.setLockscreenVisibility(0);
        NotificationManager service = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        ;
        assert service != null;
        service.createNotificationChannel(chan);
        return "http_service";
    }

    public HttpService() {
        super("ServerIntentService");
    }
}
