import axios from 'axios';
import { getEnv } from './utils/Env';

export interface ProfileFields {
  displayName: string;
  avatarUrl: string;
  description: string;
}

export interface Profile extends ProfileFields {
  id: number;
}

export type ProfileErrors = Partial<Record<keyof ProfileFields, string>>;

export async function getProfile(id: number | null): Promise<Profile> {
  const path = id === null ? '/profile' : `/users/${id}/profile`;
  const response = await axios.get<Profile>(`${getEnv().API_BASE_URL}${path}`, { withCredentials: true });
  return response.data;
}

export async function updateProfile(fields: ProfileFields): Promise<Profile> {
  const base = getEnv().API_BASE_URL;
  const csrf = await axios.get<{ headerName: string; token: string }>(`${base}/auth/csrf`, {
    withCredentials: true,
  });
  const response = await axios.put<Profile>(`${base}/profile`, fields, {
    withCredentials: true,
    headers: { [csrf.data.headerName]: csrf.data.token },
  });
  return response.data;
}
