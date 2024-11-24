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
    public static final String MiddlePhoneIP = "192.168.1.7";
}

public class MainActivity extends FlutterActivity {

    private static final String CHANNEL = "";
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


class WifiHelper {
    final static String TAG = "Wifi Helper";
    private static ConcurrentHashMap<String, Socket> socketMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Thread> listenerMap = new ConcurrentHashMap<>();
    private static Thread serverThread;
    private static final int SERVER_PORT = 50000;
    private static ServerSocket serverSocket;
    private static ArrayList<String> messages = new ArrayList<>();

    static void addMsg() {
        messages.add("Sample message");
    }

    static ArrayList<String> getMessages() {
        return messages;
    }

    private static void startSocketListener(Context context, String serverIp, Socket socket) {
        if (socket.isClosed()) {
            Log.e(TAG, "Socket is already closed");
            return;
        } else {
            Log.d(TAG, "Socket is ready to be listened to");
        }
        Thread listenerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = reader.readLine()) != null) {
                    byte[] buffer = message.getBytes();
                    Map<Integer, String> response = socketConnectionHandler(null, buffer);

                    if (message.charAt(0) != 'm') {
                        messages.add(message);
                        Log.d(TAG, "Message received from " + serverIp + ": " + message);
                    } else {
                        BluetoothHelper helper = new BluetoothHelper();
                        String deviceAddress = Constants.PCMAC;
                        BluetoothDevice device = helper.getDeviceByAddress(deviceAddress);
//                        helper.sendMessage(context, device, response.get(1));
                        helper.sendMessage(context, device, message);
                    }

                }
            } catch (IOException e) {
                Log.e(TAG, "Error in socket listener for IP " + serverIp, e);
            } finally {
                // Remove socket and thread when closed
                socketMap.remove(serverIp);
                listenerMap.remove(serverIp);
                Log.d(TAG, "Socket listener for IP " + serverIp + " stopped.");
            }
        });

        listenerMap.put(serverIp, listenerThread);
        listenerThread.start();
        Log.d(TAG, "Socket listener started for IP " + serverIp);
    }

    public static Map<Integer, String> socketConnectionHandler(Context context, byte[] buffer) {
        ByteBuffer bufferWrapper = ByteBuffer.wrap(buffer);
        int isForwardedToPc = Byte.toUnsignedInt(buffer[0]);

        String message = new String(buffer, 1, buffer.length - 1, StandardCharsets.UTF_8);
        Map<Integer, String> response = new HashMap<>();
        response.put(isForwardedToPc, message);
        return response;
    }

    static Socket getOrCreateSocket(String serverIp, int serverPort, Context context) throws IOException {
        if (socketMap.containsKey(serverIp)) {
            Socket retrieved = socketMap.get(serverIp);
            if (retrieved != null && !retrieved.isClosed()) {
                return retrieved;
            }
        }
        Socket socket = new Socket(serverIp, serverPort);
        socketMap.put(serverIp, socket);
        startSocketListener(context, serverIp, socket);
        return socket;
    }

    public static void sendTcpRequest(Context context, String serverIp, int serverPort, String message) {
        Socket socket = null;
        PrintWriter out = null;
        InputStream in = null;

        Log.d(TAG, "Sending TCP request");
        try {
            socket = getOrCreateSocket(serverIp, serverPort, context);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
            Log.d(TAG, "Sent message" + message);
            messages.add(message);

        } catch (IOException e) {
            Log.e(TAG, "Error sending TCP request:", e);
        }
    }


    public static void startTcpServer(Context context) {
        if (serverThread != null && serverThread.isAlive()) {
            Log.d(TAG, "TCP Server is already running");
            return;
        }

        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
//                serverSocket.bind(new InetSocketAddress("0.0.0.0", SERVER_PORT));

                String serverIp = getLocalIpAddress(); // Get the local IP address
                if (serverIp.equals("0.0.0.0")) {
                    serverIp = "127.0.0.1"; // Fallback to localhost for unspecified addresses
                }
                Log.d(TAG, "TCP Server started on IP " + serverIp + " and port " + SERVER_PORT);


                while (!serverSocket.isClosed()) {
                    try {
                        // Accept an incoming connection
                        Socket clientSocket = serverSocket.accept();
                        String clientIp = clientSocket.getInetAddress().getHostAddress();
                        Log.d(TAG, "New client connected: " + clientIp);

                        // Store the connection in socketMap
                        socketMap.put(clientIp, clientSocket);

                        // Handle client messages in a separate thread
                        startSocketListener(context, clientIp, clientSocket);
                    } catch (IOException e) {
                        Log.e(TAG, "Error accepting client connection", e);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error starting TCP server", e);
            } finally {
                Log.e(TAG, "Stopped TCP server");
            }
        });

        serverThread.start();
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaces.nextElement();
                for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses(); addresses.hasMoreElements(); ) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress(); // Return the first non-loopback IPv4 address
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP address", e);
        }
        return "0.0.0.0"; // Fallback to default unspecified address
    }


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
            if (context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request NEARBY_WIFI_DEVICES permission
                requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, 1001);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request ACCESS_FINE_LOCATION permission
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
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
            if (context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request NEARBY_WIFI_DEVICES permission
                requestPermissions(new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, 1001);
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
            if (context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
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
