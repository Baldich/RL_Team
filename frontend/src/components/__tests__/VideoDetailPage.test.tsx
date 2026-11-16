import { fireEvent, render, screen, within } from '@testing-library/react';
import VideoDetailPage from '../VideoDetailPage';
import { useVideoDetails } from '../../useVideoDetails';
import { setCommentLiked, submitReply } from '../../commentApi';

jest.mock('../../useVideoDetails');
jest.mock('../../commentApi');
const details = jest.mocked(useVideoDetails);
const retry = jest.fn();
const original = {
  id: 'imported-0',
  author: 'Alice',
  text: 'Original comment',
  timestamp: 123,
  likeCount: 4,
  likedByCurrentUser: false,
  replies: [],
};

describe('VideoDetailPage discussion', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    details.mockReturnValue({
      status: 'success',
      message: '',
      retry,
      value: {
        id: 1,
        title: 'Video',
        description: 'Description',
        videoPath: '77.mp4',
        thumbnailPath: '77.webp',
        comments: [original],
      },
    });
  });

  it('keeps the player mounted while posting replies and changing likes', async () => {
    jest.mocked(submitReply).mockResolvedValue({ ...original, id: 'local-7', author: 'Bob', text: 'Reply' });
    jest.mocked(setCommentLiked).mockResolvedValue({ likeCount: 5, likedByCurrentUser: true });
    render(<VideoDetailPage videoId={1} user={{ id: 7, email: 'bob@example.com' }} onBack={jest.fn()} />);
    const player = screen.getByLabelText('Video player');
    fireEvent.click(screen.getByRole('button', { name: 'Reply' }));
    const form = screen.getByRole('form', { name: 'Reply to Alice' });
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: 'Reply' } });
    fireEvent.submit(form);
    await screen.findByRole('list', { name: 'Replies to Alice' });
    const alice = within(screen.getByRole('article', { name: 'Comment by Alice' }));
    fireEvent.click(alice.getByRole('button', { name: 'Like' }));
    await alice.findByRole('button', { name: 'Unlike' });
    expect(screen.getByLabelText('Video player')).toBe(player);
    expect(retry).not.toHaveBeenCalled();
  });
});
