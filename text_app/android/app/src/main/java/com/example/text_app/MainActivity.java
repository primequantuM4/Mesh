package com.example.text_app;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;


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

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    if (call.method.equals("sendMessage")) {

                        String deviceAddress = call.argument("deviceAddress");
                        String message = call.argument("message");
                        bluetoothHelper.sendMessage(this,
                                bluetoothHelper.getDeviceByAddress(Constants.PC_MAC),
                                message);

                        requestBluetoothPermission();
                        result.success("Devices printed");
                    } else if (call.method.equals("sendTCP")) {
                        new Thread(() -> {
                            String ip = Constants.MiddlePhoneIP;
                            Log.d("Response", "re Clicked5" + ip);
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
