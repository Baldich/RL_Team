import { act, renderHook, waitFor } from '@testing-library/react';
import axios from 'axios';
import { useAllVideos } from '../useAllVideos';
import type { VideoPage } from '../types/video';

jest.mock('axios');
const get = jest.mocked(axios.get);
const page: VideoPage = {
  content: [{ id: 1, title: 'Video', thumbnailPath: '0.webp' }],
  page: 0,
  size: 12,
  totalElements: 13,
  totalPages: 2,
};

describe('useAllVideos', () => {
  beforeEach(() => jest.resetAllMocks());

  it('loads a requested page using the configured API', async () => {
    get.mockResolvedValue({ data: page });
    const { result } = renderHook(() => useAllVideos());
    expect(result.current.loading).toBe('loading');
    await waitFor(() => expect(result.current.loading).toBe('success'));
    expect(result.current.value).toEqual(page);
    expect(get).toHaveBeenCalledWith(
      'other/videos',
      expect.objectContaining({ params: { page: 0, size: 12 }, signal: expect.any(AbortSignal) })
    );
  });

  it('shows errors and retries the same page', async () => {
    get.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ data: { ...page, content: [] } });
    const { result } = renderHook(() => useAllVideos(1));
    await waitFor(() => expect(result.current.loading).toBe('error'));
    expect(result.current.message).toContain('Unable to load videos');
    act(() => result.current.retry());
    await waitFor(() => expect(result.current.loading).toBe('success'));
    expect(result.current.value?.content).toEqual([]);
    expect(get).toHaveBeenLastCalledWith('other/videos', expect.objectContaining({ params: { page: 1, size: 12 } }));
  });

  it('ignores stale responses and aborts requests on page changes', async () => {
    let resolveFirst!: (value: { data: VideoPage }) => void;
    get.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveFirst = resolve;
        })
    );
    get.mockResolvedValueOnce({
      data: { ...page, page: 1, content: [{ id: 2, title: 'Next', thumbnailPath: '1.webp' }] },
    });
    const { result, rerender, unmount } = renderHook(({ number }) => useAllVideos(number), {
      initialProps: { number: 0 },
    });
    const firstSignal = get.mock.calls[0][1]?.signal;
    rerender({ number: 1 });
    await waitFor(() => expect(result.current.value?.page).toBe(1));
    expect(firstSignal?.aborted).toBe(true);
    await act(async () => resolveFirst({ data: page }));
    expect(result.current.value?.page).toBe(1);
    const lastSignal = get.mock.calls[1][1]?.signal;
    unmount();
    expect(lastSignal?.aborted).toBe(true);
  });

  it('ignores a rejected request after unmount', async () => {
    let reject!: (error: Error) => void;
    get.mockImplementationOnce(
      () =>
        new Promise((_resolve, fail) => {
          reject = fail;
        })
    );
    const { unmount } = renderHook(() => useAllVideos());
    unmount();
    await act(async () => reject(new Error('cancelled')));
  });
});
