package com.lidar.app;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;


//class to open bluetooth socket to arduino
public class ConnectThread extends Thread {
    private final BluetoothSocket Socket;
    private static final String TAG = "LIDAR_APP";
    public static Handler handler;
    private final static int ERROR_READ = 0;

    @SuppressLint("MissingPermission")//for private use so no need to check permissions
    public ConnectThread(BluetoothDevice device, UUID MY_UUID, Handler handler)
    {
        BluetoothSocket tmp = null;
        this.handler = handler;

        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket creation failed", e);
        }
        Socket = tmp;
    }

    @SuppressLint("MissingPermission")
    public void run()
    {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.cancelDiscovery();
        try {
            Socket.connect();
        } catch (IOException connectE) {
            // If unable to connect; close the socket and return.
            handler.obtainMessage(ERROR_READ, "Unable to connect to the BT device").sendToTarget();
            Log.e(TAG, "connectException: " + connectE);
            try {
                Socket.close();
            } catch (IOException closeE) {
                Log.e(TAG, "Could not close the client socket", closeE);
            }
            return;
        }
    }

    public void cancel()
    {
        try
        {
            Socket.close();
        }catch(IOException e)
        {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public BluetoothSocket getSocket() {return Socket;}
}
