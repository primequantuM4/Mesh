package com.example.text_app;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class BluetoothHelper {
    private static final String TAG = "BluetoothHelper";
    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothHelper() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not available");
        }
    }


    public BluetoothSocket createSocketWithPort(BluetoothDevice device, int port) {
        try {
            @SuppressWarnings("JavaReflectionMemberAccess")
            //Using reflection to get the method
            /*
                TODO: use UUID to createRfcommSocketToServiceRecord
                 instead of using createRfCommSocket
            */

                    Method m = device.getClass().getMethod("createRfcommSocket", int.class);

            m.setAccessible(true);
            Log.d(TAG, "Socket with port created");
            return (BluetoothSocket) m.invoke(device, port);
        } catch (Exception e) {
            Log.d(TAG, "Error while creating a socket with port");
            return null;
        }
    }

    private BluetoothSocket getSocket(BluetoothDevice device) {
        BluetoothSocket socket = BluetoothSocketManager.getInstance().getSocket(device.getAddress());
        if (socket == null) {
            try {
                BluetoothSocket newSocket = createSocketWithPort(device, 4);
                BluetoothSocketManager.getInstance().addSocket(device.getAddress(), newSocket);
                return newSocket;
            } catch (Exception e) {
                Log.e(TAG, "Error while creating a socket with port");
            }
        }

        return socket;
    }

    public void sendMessage(Context context, BluetoothDevice device, String message) {
        new Thread(() -> {

            try (BluetoothSocket socket = getSocket(device)) {
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Permission not granted");
                        return;
                    }
                }

                String msg = message.isEmpty() ? "Hello mesh world" : message;
                Log.d(TAG, "Client: Connecting to " + device.getName());

                if (socket.isConnected()) {
                    Log.d(TAG, "Client: Connected");
                } else {
                    try {
                        socket.connect();
                        Log.d(TAG, "Socket connection created");
                    } catch (Exception e) {
                        Log.e(TAG, "Couldn't establish socket connection: ", e);
                    }
                }


                outputStream.write(msg.getBytes());
                outputStream.flush();

                Log.d(TAG, "Message sent: " + msg);

                byte[] buffer = new byte[1024];

                for (; ; ) {
                    try {
                        int bytes = inputStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "Received Message " + incomingMessage);
                    } catch (Exception e) {
                        Log.e(TAG, "Bluetooth Input stream was disconnected", e);
                        break;
                    }


                }

            } catch (Exception e) {
                Log.e(TAG, "Error while sending message", e);
            }
        }).start();
    }

    public BluetoothDevice getDeviceByAddress(String address) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter is not available");
            return null;
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "No device found with the address provided");
            return null;
        } else {
            Log.d(TAG, "Device found with the address provided");
        }

        return device;
    }
}