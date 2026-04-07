import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AuthUser {
  final String id;
  final String email;
  final String fullName;
  final String role;

  const AuthUser({
    required this.id,
    required this.email,
    required this.fullName,
    required this.role,
  });

  bool get isAdmin => role == 'ADMIN';

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
        id: json['id'] as String,
        email: json['email'] as String,
        fullName: json['fullName'] as String,
        role: json['role'] as String,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'email': email,
        'fullName': fullName,
        'role': role,
      };
}

class AuthNotifier extends Notifier<AuthUser?> {
  @override
  AuthUser? build() => null;

  // Restores persisted session on cold start.
  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString('user');
    final token = prefs.getString('accessToken');
    if (stored != null && token != null) {
      state = AuthUser.fromJson(json.decode(stored) as Map<String, dynamic>);
    }
  }

  Future<void> login({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String email,
    required String fullName,
    required String role,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('accessToken', accessToken);
    await prefs.setString('refreshToken', refreshToken);

    final user = AuthUser(id: userId, email: email, fullName: fullName, role: role);
    await prefs.setString('user', json.encode(user.toJson()));
    state = user;
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('accessToken');
    await prefs.remove('refreshToken');
    await prefs.remove('user');
    state = null;
  }
}

final authProvider = NotifierProvider<AuthNotifier, AuthUser?>(() => AuthNotifier());
