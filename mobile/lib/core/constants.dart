/// Base URL for the Mwanga Wallet API.
///
/// Override at build time with:
///   flutter run --dart-define=API_BASE_URL=http://192.168.1.x:8080
///
/// Defaults:
///   Android emulator → 10.0.2.2  (maps to host machine localhost)
///   iOS simulator    → 127.0.0.1
///   Physical device  → use your machine's LAN IP
const String kApiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080',
);
