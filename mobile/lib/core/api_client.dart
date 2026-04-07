import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'constants.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  const ApiException(this.statusCode, this.message);

  @override
  String toString() => message;
}

class ApiClient {
  static Future<String?> _token() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('accessToken');
  }

  static Future<Map<String, String>> _headers({bool auth = true}) async {
    final h = {'Content-Type': 'application/json; charset=utf-8'};
    if (auth) {
      final token = await _token();
      if (token != null) h['Authorization'] = 'Bearer $token';
    }
    return h;
  }

  static Future<dynamic> _handleResponse(http.Response res) async {
    final body = json.decode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
    if (res.statusCode >= 400) {
      throw ApiException(
        res.statusCode,
        body['message'] as String? ?? 'Request failed (${res.statusCode})',
      );
    }
    return body['data'];
  }

  static Uri _uri(String path) => Uri.parse('$kApiBaseUrl$path');

  static Future<dynamic> get(String path, {bool auth = true}) async {
    final res = await http.get(_uri(path), headers: await _headers(auth: auth));
    return _handleResponse(res);
  }

  static Future<dynamic> post(
    String path,
    Map<String, dynamic> body, {
    bool auth = true,
  }) async {
    final res = await http.post(
      _uri(path),
      headers: await _headers(auth: auth),
      body: json.encode(body),
    );
    return _handleResponse(res);
  }

  static Future<dynamic> patch(
    String path, [
    Map<String, dynamic>? body,
  ]) async {
    final res = await http.patch(
      _uri(path),
      headers: await _headers(),
      body: body != null ? json.encode(body) : null,
    );
    return _handleResponse(res);
  }
}
