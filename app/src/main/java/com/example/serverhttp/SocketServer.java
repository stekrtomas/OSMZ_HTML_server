package com.example.serverhttp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

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
	
	public void run() {
        try {
        	Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {
            	Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept(); 
                Log.d("SERVER", "Socket Accepted");
                
                OutputStream o = s.getOutputStream();
	        	BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
	        	BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

	            String tmp = in.readLine();
	            //out.write(tmp.toUpperCase());
                //out.flush();

				out.write("HTTP/1.0 200 OK\n" + "Content-Type: text/html\n" + "\n" + "<html>\n" + "<body>\n" + "<h1>Test OSMZ</h1>");
				out.flush();

                s.close();
                Log.d("SERVER", "Socket Closed");
            }
        } 
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
        	bRunning = false;
        }
    }

}
