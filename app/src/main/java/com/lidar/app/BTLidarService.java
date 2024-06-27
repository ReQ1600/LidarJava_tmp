package com.lidar.app;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.util.UUID;

public class BTLidarService {
    private static final String TAG = "BTLidarService";
    private static final String APP_NAME = "LIDAR";
    private static final UUID APP_UUID = UUID.randomUUID();

    private final BluetoothAdapter mBTAdapter;
    Context mContext;

    //constructor
    public BTLidarService(Context context)
    {
        mContext = context;
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //listens for connections. Will run until connection is accepted or until cancelled
    private class AcceptThread extends Thread
    {
        private final BluetoothServerSocket mServerSocket;
        @SuppressLint("MissingPermission")
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBTAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, APP_UUID);
                Log.d(TAG, "AcceptThread: Server set up with UUID = "+APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "AcceptThread: Server set up failed; ",e);
                AlertDialog.Builder dlgConnectionError_builder = new AlertDialog.Builder(mContext, android.R.style.Theme_DeviceDefault_Dialog);
                dlgConnectionError_builder.setMessage(e.getMessage());
                dlgConnectionError_builder.setTitle("Connection Error");
                dlgConnectionError_builder.setNeutralButton("OK",null);
                dlgConnectionError_builder.create();
            }

            mServerSocket = tmp;
        }

        public void Run()
        {
            Log.d(TAG, "Run: AcceptThread running");

            BluetoothSocket socket = null;
            //will only return on successful connection or cancellation
            while(true)
            {
                try {
                    socket = mServerSocket.accept();

                    Log.d(TAG, "Run: RFCOM socket accepted connection");
                } catch (IOException e) {
                    Log.e(TAG, "Run: sokcet acception failed", e);
                    break;
                }

                //if connection accpted
                if (socket != null) {

                    //DO SHIT HERE but in different thread
                    Cancel();
                    break;
                }
            }
        }

        public void Cancel()
        {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Run: Socket closure failed", e);
            }
        }
    }
}
