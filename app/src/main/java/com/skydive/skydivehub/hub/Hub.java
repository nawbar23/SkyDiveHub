package com.skydive.skydivehub.hub;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.skydive.skydivehub.MainActivity;
import com.skydive.skydivehub.usb.UsbOtgPort;

import java.io.IOException;

public class Hub {
    private static final String DEBUG_TAG = Hub.class.getSimpleName();

    private Context context;
    private Listener listener;

    private UsbOtgPort usb;
    private CommInterface internet;

    public Hub(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void connect(CommInterface commInterface) {
        usb = new UsbOtgPort(context);
        internet = commInterface;

        usb.setListener(usbListener);
        internet.setListener(internetListener);

        usb.connect();
    }

    public void disconnect() {
        internet.disconnect();
    }

    private void handleError(final IOException e) {
        e.printStackTrace();
        usb.disconnect();
        internet.disconnect();
        listener.onMessage(e.getMessage());
    }

    private CommInterface.CommInterfaceListener usbListener = new CommInterface.CommInterfaceListener() {
        @Override
        public void onConnected() {
            Log.e(DEBUG_TAG, "USB connected");
            internet.connect();
        }

        @Override
        public void onDisconnected() {
            internet.disconnect();
            listener.onDisconnected();
        }

        @Override
        public void onError(IOException e) {
            handleError(e);
        }

        @Override
        public void onDataReceived(byte[] data, int dataSize) {
            internet.send(data, dataSize);
        }
    };

    private CommInterface.CommInterfaceListener internetListener = new CommInterface.CommInterfaceListener() {
        @Override
        public void onConnected() {
            Log.e(DEBUG_TAG, "Connection established!");
            listener.onConnected();
        }

        @Override
        public void onDisconnected() {
            usb.disconnect();
        }

        @Override
        public void onError(IOException e) {
            handleError(e);
        }

        @Override
        public void onDataReceived(byte[] data, int dataSize) {
            usb.send(data, dataSize);
        }
    };

    public interface Listener {
        void onConnected();
        void onDisconnected();
        void onMessage(String message);
    }

    public static String byteToHexString(byte b) {
        String ret = "";
        int intVal = b & 0xff;
        if (intVal < 0x10) ret += "0";
        ret += Integer.toHexString(intVal);
        return ret;
    }

    public static String byteArrayToHexString(byte[] in, int size) {
        String ret = "";
        for (int i = 0; i < size; ++i) {
            ret += byteToHexString(in[i]);
        }
        return ret;
    }
}
