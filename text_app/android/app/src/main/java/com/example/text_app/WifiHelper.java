package com.example.text_app;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.flutter.Log;

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
