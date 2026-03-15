import client from './client';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const login = (data: LoginRequest) =>
  client.post<AuthResponse>('/api/v1/auth/login', data).then((r) => r.data);

export const register = (data: RegisterRequest) =>
  client.post<AuthResponse>('/api/v1/auth/register', data).then((r) => r.data);
