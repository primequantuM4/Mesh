package com.example.text_app;

import android.bluetooth.BluetoothSocket;

import java.util.HashMap;
import java.util.Map;

public class BluetoothSocketManager {

    private static BluetoothSocketManager instance;
    private final Map<String, BluetoothSocket> socketMap = new HashMap<>();

    private BluetoothSocketManager() {
    }

    public static synchronized BluetoothSocketManager getInstance() {
        if (instance == null) {
            instance = new BluetoothSocketManager();
        }
        return instance;
    }

    public void addSocket(String deviceAddress, BluetoothSocket socket) {
        socketMap.put(deviceAddress, socket);
    }

    public BluetoothSocket getSocket(String deviceAddress) {
        return socketMap.get(deviceAddress);
    }
}
