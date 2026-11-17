import axios from 'axios';
import { getEnv } from './utils/Env';

export interface AnalyticsVideo {
  id: number;
  title: string;
  views: number;
  comments: number;
  likes: number;
}

export interface Analytics {
  videos: AnalyticsVideo[];
  totalViews: number;
  totalComments: number;
  totalLikes: number;
}

export async function getAnalytics(): Promise<Analytics> {
  const response = await axios.get<Analytics>(`${getEnv().API_BASE_URL}/analytics`, {
    withCredentials: true,
  });
  return response.data;
}
