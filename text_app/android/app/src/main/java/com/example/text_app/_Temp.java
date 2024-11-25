package com.example.text_app;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import io.flutter.Log;

public class _Temp {
}
class WifiDirectHelper {
    private static final String TAG = "WifiDirectHelper";

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.PeerListListener peerListListener;
    private WifiP2pManager.ConnectionInfoListener connectionInfoListener;

    private ArrayList<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private boolean isGroupOwner = false;
    private String groupOwnerAddress;

    public interface DataCallback {
        void onDataReceived(String data);
    }

    private DataCallback dataCallback;

    public WifiDirectHelper(Context context, DataCallback callback) {

        this.manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(context, context.getMainLooper(), null);
        this.dataCallback = callback;
        initializeListeners();

    }

    private void initializeListeners() {
        this.peerListListener = peerList -> {
            peers.clear();
            peers.addAll(peerList.getDeviceList());
            Log.d(TAG, "Peers discovered: " + peers.size());
        };

        this.connectionInfoListener = info -> {
            groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
            isGroupOwner = info.isGroupOwner;

            if (isGroupOwner) {
                startServer();
            } else {
                connectToServer(groupOwnerAddress);
            }
        };
    }

    void requestPermissions(String[] permissions, int requestCode) {

    }

    public void discoverPeers(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request NEARBY_WIFI_DEVICES permission
                requestPermissions(new String[]{android.Manifest.permission.NEARBY_WIFI_DEVICES}, 1001);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request ACCESS_FINE_LOCATION permission
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
                }
            }
        }
        try {

            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Discovery started");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Discovery failed: " + reason);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Couldnt discover" + e);
        }
    }

    public void connectToPeer(WifiP2pDevice device, Context context) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request NEARBY_WIFI_DEVICES permission
                requestPermissions(new String[]{android.Manifest.permission.NEARBY_WIFI_DEVICES}, 1001);
            }
        }


        try {

            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Connection initiated");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Connection failed: " + reason);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Couldnt discover" + e);
        }

    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888)) {
                Log.d(TAG, "Server waiting for connection...");
                Socket socket = serverSocket.accept();
                Log.d(TAG, "Connected");

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = reader.readLine();

                Log.d(TAG, "Recieved: " + message);


                if (dataCallback != null) {
                    dataCallback.onDataReceived(message);
                }
                reader.close();
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Server error:" + e);
            }
        }).start();

    }

    public ArrayList<WifiP2pDevice> getDiscoveredPeers(Context context) {
        return peers;
    }

    public void registerListeners(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request NEARBY_WIFI_DEVICES permission
                requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, 1001);
            }
        }


        try {
            manager.requestPeers(channel, peerListListener);
            manager.requestConnectionInfo(channel, connectionInfoListener);
        } catch (Exception e) {
            Log.e(TAG, "Couldnt register listeners" + e);
        }
    }

    public void connectToServer(String host) {
        new Thread(() -> {
            try (Socket socket = new Socket(host, 8888)) {
                Log.d(TAG, "Connected to server");
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write("Hello world!".getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
            }
        }).start();

    }
}
class BluetoothScanner {
    private static final String TAG = "Bluetooth Scanner";
    private final BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private final Context context;
    private final BroadcastReceiver receiver;

    public BluetoothScanner(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    try {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                Log.e(TAG, "Permission not granted");
                                return;
                            }
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (device != null) {
                                String deviceName = device.getName() == null ? "Unknown Device" : device.getName();
                                String deviceAddress = device.getAddress();
                                Log.d(TAG, "Discovered device: " + deviceName + " " + deviceAddress);
                                discoveredDevices.add(device);
                            }
                        }

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(TAG, "Discovery finished");
                }
            }
        };
    }

    public void startScanning() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is disabled. Please enable Bluetooth first.");
            return;
        }

        // Clear previous scan results
        discoveredDevices.clear();

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(receiver, filter);

        // Start discovery
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission not granted");
                    return;
                }

                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothAdapter.startDiscovery();
                Log.d(TAG, "Bluetooth discovery started.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while starting discovery", e);
        }
    }

    public void stopScanning() {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Permission not granted");
                    return;
                }

                if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "Bluetooth discovery stopped.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while stopping discovery", e);
        }
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered or already unregistered.");
        }
    }

    public List<BluetoothDevice> getDiscoveredDevices() {
        return new ArrayList<>(discoveredDevices);
    }
}
