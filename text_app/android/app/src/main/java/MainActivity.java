import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.UUID;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "bluetooth_channel";
    private final BluetoothHelper bluetoothHelper = new BluetoothHelper();

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine){
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                        if(call.method.equals("startServer")) {
                            requestBluetoothPermission();
                            bluetoothHelper.startServer(this);
                            result.success("Server Started");
                        } else if (call.method.equals("connectToServer")) {
                            String deviceAddress = call.argument("deviceAddress");
                            BluetoothDevice device = bluetoothHelper.getDeviceByAddress(deviceAddress);
                            if (device == null) {
                                result.error("Device not found", null, null);
                            } else {
                                requestBluetoothPermission();
                                bluetoothHelper.connectToServer(this, device);
                                result.success("Connected to server");
                            }
                            bluetoothHelper.connectToServer(this, device);
                            result.success("Connected to server");
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
    }}


class BluetoothHelper{
    private static final String TAG = "BluetoothHelper";
    private static final String APP_NAME = "BluetoothEchoApp";
    private static final UUID APP_UUID = UUID.fromString("e8d8d914-5d36-11ec-bf63-0242ac130002");


    private final BluetoothAdapter bluetoothAdapter;
    public BluetoothHelper() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not available");
        }

    }

    public void startServer(Context context) {
        new Thread(() -> {
            try {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if(checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Permission not granted");
                        return;
                    }
                }

                try(BluetoothServerSocket serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)) {
                    Log.d(TAG, "Server Waiting for connection...");
                    BluetoothSocket socket = serverSocket.accept();
                    Log.d(TAG, "Server Connection Accepted...");
                }
                catch(Exception e) {
                    Log.e(TAG, "SERVER ERROR WHILE LISTENING FOR CONNECTION");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Missing Bluetooth Permission", e);

            }
        }).start();
    }

    public void connectToServer(Context context, BluetoothDevice device) {
        new Thread(() -> {

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Permission not granted");
                        return;
                    }
                }
                try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(APP_UUID)) {
                    Log.d(TAG, "Client: Connecting");
                    socket.connect();
                    Log.d(TAG, "Client: Connected");
                } catch (Exception e) {
                    Log.e(TAG, "Client: Connection failed", e);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public BluetoothDevice getDeviceByAddress(String address) {
        if (bluetoothAdapter == null) {
            Log.e(TAG,"Bluetooth adapter is not available");
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

