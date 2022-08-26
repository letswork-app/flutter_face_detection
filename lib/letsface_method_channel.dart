import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'letsface_platform_interface.dart';

/// An implementation of [LetsfacePlatform] that uses method channels.
class MethodChannelLetsface extends LetsfacePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('letsface');

  @override
  Future<String?> detectFace() async {
    final imgBase64 = await methodChannel.invokeMethod<String>('detectFace');
    return imgBase64;
  }
}
