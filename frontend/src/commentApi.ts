import axios from 'axios';
import { getEnv } from './utils/Env';
import type { CommentLikeState, VideoComment } from './types/video';

async function credentials() {
  const csrf = await axios.get<{ headerName: string; token: string }>(`${getEnv().API_BASE_URL}/auth/csrf`, {
    withCredentials: true,
  });
  return { withCredentials: true, headers: { [csrf.data.headerName]: csrf.data.token } };
}

export async function submitComment(videoId: number, text: string): Promise<VideoComment> {
  const response = await axios.post<VideoComment>(
    `${getEnv().API_BASE_URL}/videos/${videoId}/comments`,
    { text },
    await credentials()
  );
  return response.data;
}

export async function submitReply(videoId: number, commentId: string, text: string): Promise<VideoComment> {
  const response = await axios.post<VideoComment>(
    `${commentUrl(videoId, commentId)}/replies`,
    { text },
    await credentials()
  );
  return response.data;
}

export async function setCommentLiked(videoId: number, commentId: string, liked: boolean): Promise<CommentLikeState> {
  const url = `${commentUrl(videoId, commentId)}/like`;
  const config = await credentials();
  const response = liked
    ? await axios.put<CommentLikeState>(url, undefined, config)
    : await axios.delete<CommentLikeState>(url, config);
  return response.data;
}

function commentUrl(videoId: number, commentId: string) {
  return `${getEnv().API_BASE_URL}/videos/${videoId}/comments/${encodeURIComponent(commentId)}`;
}
