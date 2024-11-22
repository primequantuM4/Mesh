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

  Future<void> FetchMessages() async {
    try {
      final result = await platform.invokeMethod('getMessages');
      debugPrint("Messages: $result");
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

  Future<void> sendTCP() async {
    try {
      final result = await platform .invokeMethod('sendTCP');
      debugPrint(result);
    } catch (e) {
      debugPrint("Failed to connect: $e");
    }
  }

  Future<void> sendMessage(String deviceAddress, String message) async {
    //Todo: update device address
    try {
      final result = await platform
          .invokeMethod('sendMessage', {'deviceAddress':deviceAddress , 'message':message});
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

    const address = "10:5B:AD:8B:BC:3C";
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
              ),


              ElevatedButton(
                  onPressed: () async{
                    await blManager.startServer();
                  },
                  child: const Text("Start Server1")
              ),

              ElevatedButton(
                  onPressed: () async{
                    await blManager.connectToServer(address);
                  },
                  child: const Text("Connect to server 2")
              ),

              ElevatedButton(
                  onPressed: () async{
                    await blManager.sendMessage(address, "Send message");
                  },
                  child: const Text("Send message to $address")
              ),

              ElevatedButton(
                  onPressed: () async{
                    await blManager.sendTCP();
                  },
                  child: const Text("Send TCP")
              ),

              ElevatedButton(
                  onPressed: () async{
                    var ip = "hello world";
                    await Clipboard.setData(ClipboardData(text: ip));
                  },
                  child: const Text("Get ip")
              ),


              ElevatedButton(onPressed: () async {
                await blManager.FetchMessages();
              }, child: const Text("Get Messages") )
            ],
          ),
        )),
      ),
    );
  }
}