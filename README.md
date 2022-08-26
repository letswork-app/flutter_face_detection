# Plugin Flutter Face Detection

Detectar face em modo nativo e retorna imagem em Base64

## Example
import 'package:flutter_face_detection/letsface.dart';
Letsface _letsfacePlugin = Letsface();
String? base64image = await _letsfacePlugin.detectFace();

