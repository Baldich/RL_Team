import { useEffect, useState } from 'react';
import axios from 'axios';
import { getEnv } from './utils/Env';
import type { VideoDetails } from './types/video';

type Status = 'idle' | 'loading' | 'success' | 'not-found' | 'error';

export function useVideoDetails(videoId: number | null) {
  const [value, setValue] = useState<VideoDetails | null>(null);
  const [message, setMessage] = useState('');
  const [status, setStatus] = useState<Status>(videoId === null ? 'idle' : 'loading');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (videoId === null) {
      setValue(null);
      setMessage('');
      setStatus('idle');
      return;
    }
    let active = true;
    const controller = new AbortController();
    setStatus('loading');
    setMessage('');
    axios
      .get<VideoDetails>(`${getEnv().API_BASE_URL}/videos/${videoId}`, { signal: controller.signal })
      .then((response) => {
        if (active) {
          setValue(response.data);
          setStatus('success');
        }
      })
      .catch((error: unknown) => {
        if (!active) return;
        if (isNotFoundError(error)) {
          setValue(null);
          setStatus('not-found');
          setMessage('Video not found.');
          return;
        }
        setValue(null);
        setStatus('error');
        setMessage('Unable to load video details. Please try again.');
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [videoId, attempt]);

  return {
    value,
    message,
    status,
    retry: () => setAttempt((current) => current + 1),
  };
}

function isNotFoundError(error: unknown) {
  if (axios.isAxiosError(error) && error.response?.status === 404) {
    return true;
  }
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { status?: number } }).response;
    return response?.status === 404;
  }
  return false;
}
