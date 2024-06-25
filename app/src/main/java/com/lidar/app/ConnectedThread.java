package com.lidar.app;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//class to manage already opened socket
public class ConnectedThread extends Thread
{
    private static final String TAG = "LIDAR_APP";
    private final BluetoothSocket Socket;
    private final InputStream InStream;
    private final OutputStream OutStream;
    private String ReadVal;

    public ConnectedThread(BluetoothSocket socket)
    {
        Socket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try
        {
            tmpIn = socket.getInputStream();
        }catch(IOException e)
        {
            Log.e(TAG, "Input Stream creation failed", e);
        }

        try
        {
            tmpOut = socket.getOutputStream();
        }catch(IOException e)
        {
            Log.e(TAG, "Output Stream creation failed");
        }

        InStream = tmpIn;
        OutStream = tmpOut;
    }

    public String getReadVal() {return ReadVal;}

    public void run()
    {
        byte[] buf = new byte[1024];
        int bytes = 0;
        int numOfReadings = 0;

        //listens to the InputStream until an exception occurs(input stream disconnects)
        while (numOfReadings < 1)
        {
            try
            {
                buf[bytes] = (byte) InStream.read();
                String msg;

                if (buf[bytes] == '\n')//new line means everything has been read
                {
                    msg = new String(buf,0,bytes);
                    Log.e(TAG,msg);
                    ReadVal = msg;
                    bytes=0;
                    numOfReadings++;
                }
                else
                {
                    bytes++;
                }
            }catch(IOException e)
            {
                Log.d(TAG,"Input stream has been disconnected", e);
                break;
            }
        }
    }

    public void cancel()
    {
        try
        {
            Socket.close();
        }catch(IOException e)
        {
            Log.e(TAG,"Could not close connected socket");
        }
    }

    public void write(String input)
    {
        byte[] bytes = input.getBytes();
        try
        {
            OutStream.write(bytes);
        } catch (IOException e) {
            Log.e("MsgSendingError", "Unable to send message",e);
        }
    }
}
