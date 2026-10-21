import { act, renderHook, waitFor } from '@testing-library/react';
import axios from 'axios';
import { useVideoDetails } from '../useVideoDetails';
import type { VideoDetails } from '../types/video';

jest.mock('axios');
const get = jest.mocked(axios.get);

const details: VideoDetails = {
  id: 1,
  title: 'Video',
  description: 'Description',
  videoPath: '0.mp4',
  thumbnailPath: '0.webp',
  comments: [],
};

describe('useVideoDetails', () => {
  beforeEach(() => jest.resetAllMocks());

  it('loads a video detail by id', async () => {
    get.mockResolvedValue({ data: details });
    const { result } = renderHook(() => useVideoDetails(1));
    expect(result.current.status).toBe('loading');
    await waitFor(() => expect(result.current.status).toBe('success'));
    expect(result.current.value).toEqual(details);
    expect(get).toHaveBeenCalledWith('other/videos/1', expect.objectContaining({ signal: expect.any(AbortSignal) }));
  });

  it('marks missing videos as not found', async () => {
    get.mockRejectedValue({ isAxiosError: true, response: { status: 404 } });
    const { result } = renderHook(() => useVideoDetails(99));
    await waitFor(() => expect(result.current.status).toBe('not-found'));
    expect(result.current.message).toBe('Video not found.');
  });

  it('aborts the pending request on unmount', async () => {
    let reject!: (error: Error) => void;
    get.mockImplementationOnce(
      () =>
        new Promise((_resolve, fail) => {
          reject = fail;
        })
    );
    const { unmount } = renderHook(() => useVideoDetails(1));
    unmount();
    await act(async () => reject(new Error('cancelled')));
  });
});
