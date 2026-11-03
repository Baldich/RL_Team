import axios from 'axios';
import { getEnv } from './utils/Env';

export interface VideoMetadataFields {
  description: string;
  category: string;
  tags: string[];
}

export interface UpdatedVideoMetadata extends VideoMetadataFields {
  id: number;
  ownerId: number | null;
}

export async function updateVideoMetadata(videoId: number, fields: VideoMetadataFields): Promise<UpdatedVideoMetadata> {
  const base = getEnv().API_BASE_URL;
  const csrf = await axios.get<{ headerName: string; token: string }>(`${base}/auth/csrf`, {
    withCredentials: true,
  });
  const response = await axios.put<UpdatedVideoMetadata>(`${base}/videos/${videoId}`, fields, {
    withCredentials: true,
    headers: { [csrf.data.headerName]: csrf.data.token },
  });
  return response.data;
}
