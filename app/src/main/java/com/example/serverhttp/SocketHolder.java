package com.example.serverhttp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketHolder {
    private final Socket socket;
    private final OutputStream outputStream;
    private final InputStream inputStream;

    public final Socket getSocket() {
        return this.socket;
    }

    public final OutputStream getOutputStream() {
        return this.outputStream;
    }

    public final InputStream getInputStream() {
        return this.inputStream;
    }

    public SocketHolder( Socket socket,  OutputStream outputStream,  InputStream inputStream) {
        this.socket = socket;
        this.outputStream = outputStream;
        this.inputStream = inputStream;
    }
}
