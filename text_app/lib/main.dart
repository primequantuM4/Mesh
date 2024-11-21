import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class BluetoothManager {
  static const platform = MethodChannel('bluetooth_channel');

  Future<void> startServer() async {
    try {
      final result = await platform.invokeMethod('startServer');
      debugPrint(result);
    } catch (_) {
      debugPrint("Failed to start server");
    }
  }

  Future<void> printDevices() async {
    try {
      final result = await platform.invokeMethod('printDevices');
      debugPrint("My output $result");
    }catch(e) {
      debugPrint("Failed to print devices: $e");
    }
  }
  Future<void> connectToServer(String deviceAddress) async {
    try {
      final result = await platform
          .invokeMethod('connectToServer', {'deviceAddress': deviceAddress});
      debugPrint(result);
    } catch (e) {
      debugPrint("Failed to connect: $e");
    }
  }


  Future<void> sendMessage(String deviceAddress, String message) async {
    try {
      final result = await platform
          .invokeMethod('sendMessage', {'deviceAddress': deviceAddress, 'message':message});
      debugPrint(result);
    } catch (e) {
      debugPrint("Failed to connect: $e");
    }
  }
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    final BluetoothManager blManager = BluetoothManager();
    return MaterialApp(
      title: 'Flutter Demo',
      home: Scaffold(
        body: SafeArea(
            child: Center(
          child: Column(
            children: [
              ElevatedButton(
                onPressed: () async{
                 await blManager.connectToServer("Device address");
                },
                child: const Text("Check connection"),
              ),
              ElevatedButton(
                onPressed: () async{
                  await blManager.startServer();
                },
                child: const Text("Check server"),
              ),
              ElevatedButton(
                onPressed: () async{
                  await blManager.printDevices();
                },
                child: const Text("Get connected device"),
              )
            ],
          ),
        )),
      ),
    );
  }
}