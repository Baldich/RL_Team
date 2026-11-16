import axios from 'axios';
import { setCommentLiked, submitComment, submitReply } from '../commentApi';

jest.mock('axios');
const api = jest.mocked(axios);
const config = { withCredentials: true, headers: { 'X-CSRF-TOKEN': 'token' } };

describe('Comment API', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    api.get.mockResolvedValue({ data: { headerName: 'X-CSRF-TOKEN', token: 'token' } });
  });

  it('posts root comments and replies with session credentials and fresh CSRF tokens', async () => {
    api.post.mockResolvedValue({ data: { id: 'local-7' } });
    expect(await submitComment(1, 'Comment')).toEqual({ id: 'local-7' });
    expect(api.post).toHaveBeenCalledWith('other/videos/1/comments', { text: 'Comment' }, config);
    await submitReply(1, 'imported-0', 'Reply');
    expect(api.post).toHaveBeenLastCalledWith('other/videos/1/comments/imported-0/replies', { text: 'Reply' }, config);
    expect(api.get).toHaveBeenCalledTimes(2);
    expect(api.get).toHaveBeenCalledWith('other/auth/csrf', { withCredentials: true });
  });

  it('uses idempotent PUT and DELETE requests for the requested like state', async () => {
    api.put.mockResolvedValue({ data: { likeCount: 5, likedByCurrentUser: true } });
    api.delete.mockResolvedValue({ data: { likeCount: 4, likedByCurrentUser: false } });
    expect(await setCommentLiked(1, 'local-7', true)).toEqual({ likeCount: 5, likedByCurrentUser: true });
    expect(api.put).toHaveBeenCalledWith('other/videos/1/comments/local-7/like', undefined, config);
    expect(await setCommentLiked(1, 'local-7', false)).toEqual({ likeCount: 4, likedByCurrentUser: false });
    expect(api.delete).toHaveBeenCalledWith('other/videos/1/comments/local-7/like', config);
  });

  it('encodes comment references and sends no mutation if obtaining CSRF fails', async () => {
    api.post.mockResolvedValue({ data: {} });
    await submitReply(1, 'bad/reference', 'Reply');
    expect(api.post).toHaveBeenCalledWith('other/videos/1/comments/bad%2Freference/replies', { text: 'Reply' }, config);
    api.post.mockClear();
    api.get.mockRejectedValue(new Error('offline'));
    await expect(submitComment(1, 'Comment')).rejects.toThrow('offline');
    await expect(submitReply(1, 'local-1', 'Reply')).rejects.toThrow('offline');
    await expect(setCommentLiked(1, 'local-1', true)).rejects.toThrow('offline');
    expect(api.post).not.toHaveBeenCalled();
    expect(api.put).not.toHaveBeenCalled();
    expect(api.delete).not.toHaveBeenCalled();
  });
});
