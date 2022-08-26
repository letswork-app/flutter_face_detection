import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'letsface_method_channel.dart';

abstract class LetsfacePlatform extends PlatformInterface {
  /// Constructs a LetsfacePlatform.
  LetsfacePlatform() : super(token: _token);

  static final Object _token = Object();

  static LetsfacePlatform _instance = MethodChannelLetsface();

  /// The default instance of [LetsfacePlatform] to use.
  ///
  /// Defaults to [MethodChannelLetsface].
  static LetsfacePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [LetsfacePlatform] when
  /// they register themselves.
  static set instance(LetsfacePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> detectFace() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
