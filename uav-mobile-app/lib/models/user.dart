class UserModel {
  const UserModel({
    required this.id,
    required this.username,
    this.email,
    this.fullName,
    required this.role,
    this.enabled = true,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int? ?? 0,
      username: json['username'] as String? ?? '',
      email: json['email'] as String?,
      fullName: json['full_name'] as String?,
      role: json['role'] as String? ?? 'USER',
      enabled: json['enabled'] as bool? ?? true,
    );
  }
  final int id;
  final String username;
  final String? email;
  final String? fullName;
  final String role;
  final bool enabled;

  Map<String, dynamic> toJson() => {
        'id': id,
        'username': username,
        'email': email,
        'full_name': fullName,
        'role': role,
        'enabled': enabled,
      };

  bool get isAdmin => role == 'ADMIN';

  UserModel copyWith({
    int? id,
    String? username,
    String? email,
    String? fullName,
    String? role,
    bool? enabled,
  }) {
    return UserModel(
      id: id ?? this.id,
      username: username ?? this.username,
      email: email ?? this.email,
      fullName: fullName ?? this.fullName,
      role: role ?? this.role,
      enabled: enabled ?? this.enabled,
    );
  }
}

class LoginResponse {
  const LoginResponse({
    required this.token,
    this.refreshToken,
    required this.user,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      token: json['token'] as String? ?? '',
      refreshToken: json['refresh_token'] as String?,
      user: UserModel.fromJson(json['user'] as Map<String, dynamic>? ?? {}),
    );
  }
  final String token;
  final String? refreshToken;
  final UserModel user;
}
