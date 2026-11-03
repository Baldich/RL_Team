import axios from 'axios';
import { getEnv } from './utils/Env';
import type { VideoDetails } from './types/video';

export async function uploadVideo(file: File, title: string, description: string): Promise<VideoDetails> {
  const base = getEnv().API_BASE_URL;
  const csrf = await axios.get<{ headerName: string; token: string }>(`${base}/auth/csrf`, {
    withCredentials: true,
  });
  const data = new FormData();
  data.append('file', file);
  data.append(
    'metadata',
    new Blob([JSON.stringify({ title: title.trim(), description: description.trim() })], { type: 'application/json' })
  );
  const response = await axios.post<VideoDetails>(`${base}/videos`, data, {
    withCredentials: true,
    headers: { [csrf.data.headerName]: csrf.data.token },
  });
  return response.data;
}
