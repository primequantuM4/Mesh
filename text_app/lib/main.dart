import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'mesh.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final MeshManager meshManager = MeshManager();
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
    final newMessage = await meshManager.fetchMessages();

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
                  await meshManager.sendMessage(
                    address,
                    "Hello bluetooth device!",
                  );
                },
                child: const Text("Send message to Bluetooth socket"),
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
                      await meshManager.sendTCP(
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
