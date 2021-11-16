import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:system_alert_window/models/system_window_body.dart';
import 'package:system_alert_window/models/system_window_footer.dart';
import 'package:system_alert_window/models/system_window_header.dart';
import 'package:system_alert_window/models/system_window_margin.dart';
import 'package:system_alert_window/utils/commons.dart';
import 'package:system_alert_window/utils/constants.dart';

export 'models/system_window_body.dart';
export 'models/system_window_button.dart';
export 'models/system_window_decoration.dart';
export 'models/system_window_footer.dart';
export 'models/system_window_header.dart';
export 'models/system_window_margin.dart';
export 'models/system_window_padding.dart';
export 'models/system_window_text.dart';

import 'package:shared_preferences/shared_preferences.dart';
enum SystemWindowGravity { TOP, BOTTOM, CENTER }

enum ContentGravity { LEFT, RIGHT, CENTER }

enum ButtonPosition { TRAILING, LEADING, CENTER }

enum FontWeight { NORMAL, BOLD, ITALIC, BOLD_ITALIC }

enum SystemWindowPrefMode { DEFAULT, OVERLAY, BUBBLE }

typedef void OnClickListener(String tag, Map<String, dynamic>? payload);
const MethodChannel _channel = const MethodChannel(Constants.CHANNEL);

class SystemAlertWindow {

  static late StreamController<Map<String, dynamic>> _isolateBroadcast = StreamController();
  static Stream<Map<String, dynamic>> get broadcastReceiver => _isolateBroadcast.stream;
  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool?> checkPermissions(
      {SystemWindowPrefMode prefMode = SystemWindowPrefMode.DEFAULT}) async {
    return await _channel.invokeMethod(
        'checkPermissions', [Commons.getSystemWindowPrefMode(prefMode)]);
  }
  static Future<bool> isIsolateRunning() async {
    return await _channel.invokeMethod('isIsolateRunning');
  }
  static Future<bool> connectToRunningIsolate() async {
    return await _channel.invokeMethod('connectToRunningIsolate');
  }

  static Future<bool?> requestPermissions(
      {SystemWindowPrefMode prefMode = SystemWindowPrefMode.DEFAULT}) async {
    return await _channel.invokeMethod(
        'requestPermissions', [Commons.getSystemWindowPrefMode(prefMode)]);
  }

  static Future<Map<String, dynamic>?>sendEventFromFlutterToIsolate (String id, Map<String, dynamic>  payload) async{
    print("Invoking sendEventFromFlutterToIsolate with ${id} and payload ${payload} ");
    final ans = await _channel.invokeMethod(
        'sendEventFromFlutterToIsolate', [id, payload]);
    return Map<String, dynamic>.from(ans);
  }
  static Future<bool?> registerOnClickListener(
      OnClickListener callBackFunction,) async {
    final mainIsolateEntryPointHandle =
        PluginUtilities.getCallbackHandle(mainIsolateEntryPoint)!;
    final callBack = PluginUtilities.getCallbackHandle(callBackFunction)!;
    SharedPreferences prefs = await SharedPreferences.getInstance();
    await prefs.setInt('callBackFunction', callBack.toRawHandle());
    _channel.setMethodCallHandler((MethodCall call) async {
      print("Got setMethodCallHandler in flutter ${call.method}");
      // if(call.method == "isolateBroadcast"){
      //   print("Adding 0 event to stream from isolateBroadcast ${call} ");
      //   dynamic arguments = call.arguments;
      //   print("Adding 0-1 event to stream from isolateBroadcast ${arguments} ");
      //   final payload = arguments[0];
      //   print("Adding  1 event to stream from isolateBroadcast ");
      //   _isolateBroadcast.add(payload);
      //   return null;
      // }
      switch (call.method) {
        case "isolateBroadcast":
          dynamic arguments = call.arguments;
          // final payload = arguments[0];
          print("Adding event to stream from isolateBroadcast ${arguments}");
          _isolateBroadcast.add(Map<String, dynamic>.from(arguments));
      }
      return null;
    });
    await _channel.invokeMethod("startIsolate",
        <dynamic>[mainIsolateEntryPointHandle.toRawHandle()]);
    return true;
  }

  static Future<bool?> showSystemWindow(
      {required SystemWindowHeader header,
      SystemWindowBody? body,
      SystemWindowFooter? footer,
      SystemWindowMargin? margin,
      SystemWindowGravity gravity = SystemWindowGravity.CENTER,
      int? width,
      int? height,
      String notificationTitle = "Title",
      String notificationBody = "Body",
      SystemWindowPrefMode prefMode = SystemWindowPrefMode.DEFAULT}) async {
    assert(header != null);
    final Map<String, dynamic> params = <String, dynamic>{
      'header': header.getMap(),
      'body': body?.getMap(),
      'footer': footer?.getMap(),
      'margin': margin?.getMap(),
      'gravity': Commons.getWindowGravity(gravity),
      'width': width ?? Constants.MATCH_PARENT,
      'height': height ?? Constants.WRAP_CONTENT
    };
    return await _channel.invokeMethod('showSystemWindow', [
      notificationTitle,
      notificationBody,
      params,
      Commons.getSystemWindowPrefMode(prefMode)
    ]);
  }

  static Future<bool?> updateSystemWindow(
      {required SystemWindowHeader header,
      SystemWindowBody? body,
      SystemWindowFooter? footer,
      SystemWindowMargin? margin,
      SystemWindowGravity gravity = SystemWindowGravity.CENTER,
      int? width,
      int? height,
      String notificationTitle = "Title",
      String notificationBody = "Body",
      SystemWindowPrefMode prefMode = SystemWindowPrefMode.DEFAULT}) async {
    assert(header != null);
    final Map<String, dynamic> params = <String, dynamic>{
      'header': header.getMap(),
      'body': body?.getMap(),
      'footer': footer?.getMap(),
      'margin': margin?.getMap(),
      'gravity': Commons.getWindowGravity(gravity),
      'width': width ?? Constants.MATCH_PARENT,
      'height': height ?? Constants.WRAP_CONTENT
    };
    return await _channel.invokeMethod('updateSystemWindow', [
      notificationTitle,
      notificationBody,
      params,
      Commons.getSystemWindowPrefMode(prefMode)
    ]);
  }

  static Future<bool?> closeSystemWindow(
      {SystemWindowPrefMode prefMode = SystemWindowPrefMode.DEFAULT}) async {
    return await _channel.invokeMethod(
        'closeSystemWindow', [Commons.getSystemWindowPrefMode(prefMode)]);
  }
}

class SystemAlertWindowFromIsolate {

  static Future<bool> broadcastFromIsolate(Map<String, dynamic> payload) async {
    return await _channel.invokeMethod(
        'broadcastFromIsolate', [payload]);
  }

}

int? callBackHandle;
void mainIsolateEntryPoint() {
  // 1. Initialize MethodChannel used to communicate with the platform portion of the plugin
  const MethodChannel _backgroundChannel =
      const MethodChannel(Constants.BACKGROUND_CHANNEL);
  // 2. Setup internal state needed for MethodChannels.
  WidgetsFlutterBinding.ensureInitialized();

  // 3. Listen for background events from the platform portion of the plugin.
  _backgroundChannel.setMethodCallHandler((MethodCall call) async {
    final args = call.arguments;
    print("FlutterIsolate _backgroundChannel in flutter Method channel received:  ${call.method} with $args");

    if (callBackHandle == null){
      print("D1");
      try{
        SharedPreferences prefs = await SharedPreferences.getInstance();

        print("D2");
        callBackHandle = prefs.getInt('callBackFunction')!;
      }catch(e){
        print("D1ER");
        print(e);
      }
    }
    print("D3");

    // 3.1. Retrieve callback instance for handle.
    final Function callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(callBackHandle!))!;
    print("D4");

    assert(callback != null);
    print("D5");

    final type = args[0];
    print("D6");
    final payload = args[1];
    print("calling callback with id: $type and payload: $payload");

    return callback(type, Map<String, dynamic>.from(payload ?? {}));
  });
}
