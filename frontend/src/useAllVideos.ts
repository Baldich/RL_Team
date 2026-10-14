import { useEffect, useState } from 'react';
import axios from 'axios';
import { getEnv } from './utils/Env';
import type { VideoPage } from './types/video';

export function useAllVideos(page = 0) {
  const [value, setValue] = useState<VideoPage | null>(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState<'loading' | 'success' | 'error' | 'idle'>('loading');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    setLoading('loading');
    setMessage('');
    axios
      .get<VideoPage>(getEnv().API_BASE_URL + '/videos', {
        params: { page, size: 12 },
        signal: controller.signal,
      })
      .then((response) => {
        if (active) {
          setValue(response.data);
          setLoading('success');
        }
      })
      .catch(() => {
        if (active) {
          setMessage('Unable to load videos. Please try again.');
          setLoading('error');
        }
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [page, attempt]);

  return { value, message, loading, retry: () => setAttempt((current) => current + 1) };
}
