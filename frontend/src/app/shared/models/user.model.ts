export interface User {
  id: string;
  username: string;
  token: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  username: string;
  userid: string;
  accessToken: string;
}
