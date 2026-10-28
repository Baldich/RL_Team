import axios from 'axios';
import { getEnv } from './utils/Env';

export async function submitComment(videoId: number, text: string): Promise<{ author: string; text: string; timestamp: number; likeCount: number }> {
  const csrf = await axios.get<{ headerName: string; token: string }>(`${getEnv().API_BASE_URL}/auth/csrf`, {
    withCredentials: true,
  });
  const response = await axios.post<{ author: string; text: string; timestamp: number; likeCount: number }>(
    `${getEnv().API_BASE_URL}/videos/${videoId}/comments`,
    { text },
    { withCredentials: true, headers: { [csrf.data.headerName]: csrf.data.token } }
  );
  return response.data;
}
