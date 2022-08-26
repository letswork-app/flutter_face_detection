import 'letsface_platform_interface.dart';

class Letsface {
  Future<String?> detectFace() {
    return LetsfacePlatform.instance.detectFace();
  }
}
