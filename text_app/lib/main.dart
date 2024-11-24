import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';

void main() {
  runApp(const MyApp());
}

class MeshManager {
  static const platform = MethodChannel('mesh_channel');


  Future<List<String>> fetchMessages() async {
    try {
      final result = await platform.invokeMethod('getMessages');
      if (result is List) {
        return result.map((item) => item.toString()).toList();
      } else {
        return [];
      }
    } catch (e) {
      debugPrint("Failed to fetch messages: $e");
      return [];
    }
  }

  Future<void> sendTCP( String message) async {
    try {
      final result = await platform.invokeMethod('sendTCP', {
        // 'ip': ip,
        'message': message,
        // 'port': port,
      });
      debugPrint(result);
    } catch (e) {
      debugPrint("Failed to send TCP: $e");
    }
  }

  Future<void> sendMessage(String deviceAddress, String message) async {
    try {
      final result = await platform.invokeMethod('sendMessage', {'deviceAddress': deviceAddress, 'message': message});
      debugPrint(result);
    } catch (e) {
      debugPrint("Failed to send message: $e");
    }
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final MeshManager blManager = MeshManager();
  List<String> messages = [];
  late Timer _timer;
  final TextEditingController _controller = TextEditingController();

  @override
  void initState() {
    super.initState();
    // Start polling for messages every second
    _timer = Timer.periodic(const Duration(seconds: 1), _fetchMessages);
  }

  @override
  void dispose() {
    _timer.cancel();
    super.dispose();
  }

  void _fetchMessages(Timer timer) async {
    final newMessage = await blManager.fetchMessages();

    setState(() {
      messages = newMessage;
    });
  }

  @override
  Widget build(BuildContext context) {
    const address = "10:5B:AD:8B:BC:3C";

    return MaterialApp(
      title: 'Flutter Demo',
      home: Scaffold(
        body: SafeArea(
          child: Column(
            children: [
              ElevatedButton(
                onPressed: () async {
                  await blManager.sendMessage(address, "Send message");
                },
                child: const Text("Send message to $address"),
              ),
              ElevatedButton(
                onPressed: () async {
                  var ip = "hello world";
                  await Clipboard.setData(ClipboardData(text: ip));
                },
                child: const Text("Get ip"),
              ),
              Expanded(
                child: ListView.builder(
                  reverse: true, // Show new messages at the bottom
                  itemCount: messages.length,
                  itemBuilder: (context, index) {
                    return ListTile(
                      title: Text(messages[index]),
                    );
                  },
                ),
              ),

              // Text field and button to send TCP message
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      decoration: const InputDecoration(
                        labelText: "Enter message",
                      ),
                    ),
                  ),
                  ElevatedButton(
                    onPressed: () async {
                      await blManager.sendTCP(
                        // ip: "",
                         _controller.text,
                        // port: 1234,
                      );

                      _controller.clear();
                    },
                    child: const Text("Send TCP"),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
