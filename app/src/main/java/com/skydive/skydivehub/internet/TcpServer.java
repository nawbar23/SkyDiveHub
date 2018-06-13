package com.skydive.skydivehub.internet;

import android.os.AsyncTask;
import android.util.Log;

import com.skydive.skydivehub.hub.CommInterface;
import com.skydive.skydivehub.hub.Hub;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer extends CommInterface {
    private static final String DEBUG_TAG = TcpServer.class.getSimpleName();

    private static final int port = 9999;
    private static final int maxPacketSize = 256;

    private enum State {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    private State state;

    private ServerSocket serverSocket;
    private Socket socket;
    private DataOutputStream outputStream;

    @Override
    public void connect() {
        Log.e(DEBUG_TAG, "connect");
        state = State.CONNECTING;
        new SocketConnection().execute();
    }

    @Override
    public void disconnect() {
        Log.e(DEBUG_TAG, "disconnect");
        state = State.DISCONNECTING;
    }

    @Override
    public void send(final byte[] data, final int dataSize) {
        try {
            //Log.e(DEBUG_TAG, "Sending: 0x" + Hub.byteArrayToHexString(data, dataSize));
            outputStream.write(data, 0, dataSize);
        } catch (IOException e) {
            listener.onError(e);
        }
    }

    private class SocketConnection extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.d(DEBUG_TAG, "Successfully started thread");

            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();
            } catch (IOException e) {
                listener.onError(e);
                return null;
            }

            try {
                outputStream = new DataOutputStream(socket.getOutputStream());

            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error while connecting output stream: " + e.getMessage());
                listener.onError(e);
                return null;
            }

            Log.d(DEBUG_TAG, "Connected connected stream");

            DataInputStream inputStream;
            try {
                inputStream = new DataInputStream(socket.getInputStream());
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error while connecting input stream: " + e.getMessage());
                listener.onError(e);
                return null;
            }

            state = State.CONNECTED;
            listener.onConnected();

            try {
                byte buffer[] = new byte[maxPacketSize];
                while (state != State.DISCONNECTING) {
                    int len = inputStream.available();
                    if (len > maxPacketSize) len = maxPacketSize;
                    int dataSize = inputStream.read(buffer, 0, len);

                    if (dataSize > 0) {
                        //Log.e(DEBUG_TAG, "Received: 0x" + Hub.byteArrayToHexString(buffer, dataSize));
                        listener.onDataReceived(buffer, dataSize);
                    }

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                inputStream.close();

            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error while receiving data: " + e.getMessage());
                listener.onError(e);
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            state = State.DISCONNECTED;
            listener.onDisconnected();

            try {
                socket.close();
                serverSocket.close();
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }

            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Error while closing socket: " + e.getMessage());
            }
        }
    }
}
