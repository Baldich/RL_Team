import axios from 'axios';
import { getEnv } from './utils/Env';

export interface RegisteredUser {
  id: number;
  email: string;
}

export interface RegistrationErrors {
  email?: string;
  password?: string;
}
export type LoginErrors = RegistrationErrors;

const authUrl = `${getEnv().API_BASE_URL}/auth`;

export async function getSession(): Promise<RegisteredUser | null> {
  const response = await axios.get<RegisteredUser>(`${authUrl}/session`, { withCredentials: true });
  return response.status === 204 ? null : response.data;
}

export async function register(email: string, password: string): Promise<RegisteredUser> {
  const csrf = await axios.get<{ headerName: string; token: string }>(`${authUrl}/csrf`, {
    withCredentials: true,
  });
  const response = await axios.post<RegisteredUser>(
    `${authUrl}/register`,
    { email: email.trim().toLowerCase(), password },
    { withCredentials: true, headers: { [csrf.data.headerName]: csrf.data.token } }
  );
  return response.data;
}

export async function login(email: string, password: string): Promise<RegisteredUser> {
  const csrf = await axios.get<{ headerName: string; token: string }>(`${authUrl}/csrf`, {
    withCredentials: true,
  });
  const response = await axios.post<RegisteredUser>(
    `${authUrl}/login`,
    { email: email.trim().toLowerCase(), password },
    { withCredentials: true, headers: { [csrf.data.headerName]: csrf.data.token } }
  );
  return response.data;
}

export async function logout(): Promise<void> {
  const csrf = await axios.get<{ headerName: string; token: string }>(`${authUrl}/csrf`, {
    withCredentials: true,
  });
  await axios.post(`${authUrl}/logout`, undefined, {
    withCredentials: true,
    headers: { [csrf.data.headerName]: csrf.data.token },
  });
}
