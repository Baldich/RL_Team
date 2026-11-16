import { act, fireEvent, render, screen, within } from '@testing-library/react';
import CommentSection from '../CommentSection';
import { setCommentLiked, submitComment, submitReply } from '../../commentApi';
import type { CommentLikeState, VideoComment } from '../../types/video';

jest.mock('../../commentApi');
const like = jest.mocked(setCommentLiked);
const post = jest.mocked(submitComment);
const reply = jest.mocked(submitReply);
const login = jest.fn();

const original: VideoComment = {
  id: 'imported-0',
  author: 'Alice',
  text: 'Original comment',
  timestamp: 123,
  likeCount: 4,
  likedByCurrentUser: false,
  replies: [],
};
const response: VideoComment = {
  ...original,
  id: 'local-7',
  author: 'Bob',
  text: 'A reply',
  likeCount: 0,
};

function page(comments = [original], authenticated = true) {
  return <CommentSection videoId={1} comments={comments} authenticated={authenticated} onLoginRequest={login} />;
}

function originalArticle() {
  return within(screen.getByRole('article', { name: 'Comment by Alice' }));
}

function openReply() {
  fireEvent.click(originalArticle().getByRole('button', { name: 'Reply' }));
  return screen.getByRole('form', { name: 'Reply to Alice' });
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<T>((done, fail) => {
    resolve = done;
    reject = fail;
  });
  return { promise, resolve, reject };
}

describe('CommentSection', () => {
  beforeEach(() => jest.resetAllMocks());

  it('displays persisted nested replies and the current user like state', () => {
    render(page([{ ...original, likedByCurrentUser: true, replies: [response] }]));
    expect(originalArticle().getByRole('button', { name: 'Unlike' })).toHaveAttribute('aria-pressed', 'true');
    const replies = screen.getByRole('list', { name: 'Replies to Alice' });
    expect(within(replies).getByText('A reply')).toBeInTheDocument();
    expect(within(replies).getByRole('button', { name: 'Like' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('posts a trimmed reply beneath its original comment and closes the form', async () => {
    reply.mockResolvedValue(response);
    render(page());
    const form = openReply();
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: ' A reply ' } });
    fireEvent.submit(form);
    const replies = await screen.findByRole('list', { name: 'Replies to Alice' });
    expect(within(replies).getByText('A reply')).toBeInTheDocument();
    expect(reply).toHaveBeenCalledWith(1, 'imported-0', 'A reply');
    expect(screen.queryByRole('form', { name: 'Reply to Alice' })).not.toBeInTheDocument();
  });

  it('can reply to a reply and update that nested comment independently', async () => {
    const nested = { ...response, id: 'local-8', author: 'Carol', text: 'Nested answer' };
    reply.mockResolvedValue(nested);
    like.mockResolvedValue({ likeCount: 1, likedByCurrentUser: true });
    render(page([{ ...original, replies: [response] }]));
    const bob = within(screen.getByRole('article', { name: 'Comment by Bob' }));
    fireEvent.click(bob.getByRole('button', { name: 'Reply' }));
    const form = screen.getByRole('form', { name: 'Reply to Bob' });
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: 'Nested answer' } });
    fireEvent.submit(form);
    expect(await screen.findByRole('list', { name: 'Replies to Bob' })).toHaveTextContent('Nested answer');
    fireEvent.click(bob.getByRole('button', { name: 'Like' }));
    expect(await bob.findByRole('button', { name: 'Unlike' })).toHaveAttribute('aria-pressed', 'true');
    expect(originalArticle().getByText('4 likes')).toBeInTheDocument();
    expect(like).toHaveBeenCalledWith(1, 'local-7', true);
  });

  it('uses confirmed server counts for like and unlike', async () => {
    like.mockResolvedValueOnce({ likeCount: 6, likedByCurrentUser: true });
    like.mockResolvedValueOnce({ likeCount: 5, likedByCurrentUser: false });
    render(page());
    fireEvent.click(originalArticle().getByRole('button', { name: 'Like' }));
    expect(await originalArticle().findByRole('button', { name: 'Unlike' })).toHaveAttribute('aria-pressed', 'true');
    expect(originalArticle().getByText('6 likes')).toBeInTheDocument();
    fireEvent.click(originalArticle().getByRole('button', { name: 'Unlike' }));
    expect(await originalArticle().findByRole('button', { name: 'Like' })).toHaveAttribute('aria-pressed', 'false');
    expect(originalArticle().getByText('5 likes')).toBeInTheDocument();
    expect(like).toHaveBeenLastCalledWith(1, 'imported-0', false);
  });

  it('blocks repeated likes while the request is pending', async () => {
    const pending = deferred<CommentLikeState>();
    like.mockReturnValue(pending.promise);
    render(page());
    const button = originalArticle().getByRole('button', { name: 'Like' });
    fireEvent.click(button);
    fireEvent.click(button);
    expect(like).toHaveBeenCalledTimes(1);
    expect(originalArticle().getByRole('button', { name: 'Saving like...' })).toBeDisabled();
    await act(async () => pending.resolve({ likeCount: 5, likedByCurrentUser: true }));
    expect(originalArticle().getByRole('button', { name: 'Unlike' })).toBeEnabled();
  });

  it('keeps counts unchanged on a failed like and allows retry', async () => {
    like.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({ likeCount: 5, likedByCurrentUser: true });
    render(page());
    fireEvent.click(originalArticle().getByRole('button', { name: 'Like' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to update your like');
    expect(originalArticle().getByText('4 likes')).toBeInTheDocument();
    fireEvent.click(originalArticle().getByRole('button', { name: 'Like' }));
    expect(await originalArticle().findByRole('button', { name: 'Unlike' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('prompts visitors to log in for comments, replies and likes without calling mutation APIs', () => {
    render(page([original], false));
    fireEvent.click(screen.getByRole('button', { name: 'Log in to comment' }));
    fireEvent.submit(screen.getByRole('form', { name: 'Add a comment' }));
    fireEvent.click(originalArticle().getByRole('button', { name: 'Reply' }));
    fireEvent.click(originalArticle().getByRole('button', { name: 'Like' }));
    expect(login).toHaveBeenCalledTimes(4);
    expect(post).not.toHaveBeenCalled();
    expect(reply).not.toHaveBeenCalled();
    expect(like).not.toHaveBeenCalled();
  });

  it('rejects blank and oversized input without posting and supports cancelling a reply', () => {
    render(page());
    const form = openReply();
    fireEvent.submit(form);
    expect(screen.getByRole('alert')).toHaveTextContent('1–5000 characters');
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: 'x'.repeat(5001) } });
    fireEvent.submit(form);
    expect(reply).not.toHaveBeenCalled();
    fireEvent.click(originalArticle().getByRole('button', { name: 'Cancel reply' }));
    expect(screen.queryByRole('form', { name: 'Reply to Alice' })).not.toBeInTheDocument();
  });

  it('prevents double reply submission and preserves the draft after a network failure', async () => {
    const pending = deferred<VideoComment>();
    reply.mockReturnValue(pending.promise);
    render(page());
    const form = openReply();
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: 'Keep my draft' } });
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(reply).toHaveBeenCalledTimes(1);
    expect(within(form).getByRole('textbox')).toBeDisabled();
    await act(async () => pending.reject(new Error('offline')));
    expect(screen.getByRole('alert')).toHaveTextContent('Unable to post your comment');
    expect(within(form).getByRole('textbox')).toHaveValue('Keep my draft');
    expect(within(form).getByRole('button', { name: 'Post reply' })).toBeEnabled();
  });

  it.each([401, 403])('handles an HTTP %s reply failure without losing the draft', async (status) => {
    reply.mockRejectedValue({ response: { status } });
    render(page());
    const form = openReply();
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: 'My reply' } });
    fireEvent.submit(form);
    expect(await screen.findByRole('alert')).toHaveTextContent(status === 403 ? 'security token' : 'Unable to post');
    expect(within(form).getByRole('textbox')).toHaveValue('My reply');
    expect(login).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
  });

  it('prompts login when a like reports an expired session', async () => {
    like.mockRejectedValue({ response: { status: 401 } });
    render(page());
    fireEvent.click(originalArticle().getByRole('button', { name: 'Like' }));
    await screen.findByRole('alert');
    expect(login).toHaveBeenCalledTimes(1);
  });

  it('posts root comments and clears the input only after success', async () => {
    post.mockResolvedValue({ ...response, text: 'New root' });
    render(page([]));
    expect(screen.getByText('No comments yet.')).toBeInTheDocument();
    const form = screen.getByRole('form', { name: 'Add a comment' });
    fireEvent.change(within(form).getByRole('textbox'), { target: { value: ' New root ' } });
    fireEvent.submit(form);
    expect(await screen.findByText('New root')).toBeInTheDocument();
    expect(within(form).getByRole('textbox')).toHaveValue('');
    expect(post).toHaveBeenCalledWith(1, 'New root');
  });

  it('shows refreshed persisted discussion data after a reload', () => {
    const { rerender } = render(page());
    rerender(page([{ ...original, likeCount: 5, likedByCurrentUser: true, replies: [response] }]));
    expect(originalArticle().getByRole('button', { name: 'Unlike' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Replies to Alice' })).toHaveTextContent('A reply');
  });

  it.each(['reply', 'like', 'comment'] as const)('ignores a late %s result after leaving the page', async (action) => {
    const pending = deferred<VideoComment & CommentLikeState>();
    reply.mockReturnValue(pending.promise);
    like.mockReturnValue(pending.promise);
    post.mockReturnValue(pending.promise);
    const { unmount } = render(page());
    if (action === 'like') {
      fireEvent.click(originalArticle().getByRole('button', { name: 'Like' }));
    } else {
      const form = action === 'reply' ? openReply() : screen.getByRole('form', { name: 'Add a comment' });
      fireEvent.change(within(form).getByRole('textbox'), { target: { value: 'Posted text' } });
      fireEvent.submit(form);
    }
    unmount();
    await act(async () => pending.resolve(response));
  });
});
