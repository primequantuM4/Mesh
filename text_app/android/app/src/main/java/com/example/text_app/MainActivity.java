package com.example.text_app;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

class Constants {
    public static final String PCMAC = "18:CC:18:82:D0:E7";
    public static final String MiddlePhoneIP = "192.168.78.169";
}

public class MainActivity extends FlutterActivity {

    private static final String CHANNEL = "mesh_channel";
    private final BluetoothHelper bluetoothHelper = new BluetoothHelper();
//    private final WifiDirectHelper wifiDirectHelper = new WifiDirectHelper(this, (data) -> Log.d("MainActivity", "Received data: "+ data));

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {

        WifiHelper.startTcpServer(this);
        WifiHelper.addMsg();
        Log.d("Server", "Tcp started");
        super.configureFlutterEngine(flutterEngine);
//        wifiDirectHelper.discoverPeers(this);
//        wifiDirectHelper.registerListeners(this);

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    if (call.method.equals("sendMessage")) {

                        String deviceAddress = call.argument("deviceAddress");
                        String message = call.argument("message");
                        bluetoothHelper.sendMessage(this,
                                bluetoothHelper.getDeviceByAddress(Constants.PCMAC),
                                message);

                        requestBluetoothPermission();
                        result.success("Devices printed");
                    } else if (call.method.equals("sendTCP")) {
                        new Thread(() -> {
                            String ip = Constants.MiddlePhoneIP;
                            Log.d("REsponse", "re Clicked5" + ip);
                            String message = call.argument("message");
                            WifiHelper.sendTcpRequest(this, ip, 50000, message);

                        }).start();
                    } else if (call.method.equals("getMessages")) {
                        result.success(WifiHelper.getMessages());
                    } else {

                        result.notImplemented();
                    }

                });
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 1);
            }
        }
    }
}





class BluetoothSocketManager {
    private static BluetoothSocketManager instance;
    private Map<String, BluetoothSocket> socketMap = new HashMap<>();

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


