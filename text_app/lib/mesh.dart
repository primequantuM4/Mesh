
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';

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
