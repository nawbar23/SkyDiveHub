package com.skydive.skydivehub.hub;

import java.io.IOException;

public abstract class CommInterface {

    protected CommInterfaceListener listener;

    public void setListener(CommInterfaceListener listener) {
        this.listener = listener;
    }

    public abstract void connect();

    public abstract void disconnect();

    public abstract void send(final byte[] data, final int dataSize);

    public interface CommInterfaceListener {
        void onConnected();

        void onDisconnected();

        void onError(IOException e);

        void onDataReceived(final byte[] data, final int dataSize);
    }
}
