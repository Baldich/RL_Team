import { useEffect, useState, type FormEvent } from 'react';
import { getEnv } from '../utils/Env';
import { useVideoDetails } from '../useVideoDetails';
import { submitComment } from '../commentApi';

interface Props {
  videoId: number;
  onBack: () => void;
  user?: { email: string; id: number } | null;
  onLoginRequest?: () => void;
}

function formatTimestamp(timestamp: number) {
  return new Date(timestamp * 1000).toLocaleString();
}

export default function VideoDetailPage({ videoId, onBack, user = null, onLoginRequest = () => {} }: Props) {
  const { message, retry, status, value } = useVideoDetails(videoId);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const [localComments, setLocalComments] = useState(value?.comments ?? []);

  useEffect(() => {
    setLocalComments(value?.comments ?? []);
  }, [value?.comments]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextText = draft.trim();
    if (!nextText) {
      setError('Comment cannot be empty.');
      return;
    }
    if (!user) {
      onLoginRequest();
      return;
    }
    setPending(true);
    setError('');
    try {
      const newComment = await submitComment(videoId, nextText);
      setLocalComments((current) => [...current, newComment]);
      setDraft('');
      retry();
    } catch (requestError: unknown) {
      if (requestError && typeof requestError === 'object' && 'response' in requestError) {
        const axiosError = requestError as { response?: { status?: number; data?: { message?: string } } };
        if (axiosError.response?.status === 401) {
          onLoginRequest();
          return;
        }
        setError(axiosError.response?.data?.message ?? 'Unable to post your comment.');
      } else {
        setError('Unable to post your comment.');
      }
    } finally {
      setPending(false);
    }
  }

  if (status === 'loading' || status === 'idle') {
    return <p role="status">Loading...</p>;
  }

  if (status === 'not-found') {
    return (
      <section role="alert" aria-labelledby="video-not-found-title">
        <h1 id="video-not-found-title">Video not found</h1>
        <p>{message}</p>
        <button type="button" onClick={onBack}>
          Back to catalog
        </button>
      </section>
    );
  }

  if (status === 'error' || !value) {
    return (
      <section role="alert" aria-labelledby="video-error-title">
        <h1 id="video-error-title">Unable to load video</h1>
        <p>{message}</p>
        <button type="button" onClick={retry}>
          Retry
        </button>
        <button type="button" onClick={onBack}>
          Back to catalog
        </button>
      </section>
    );
  }

  return (
    <article className="video-detail">
      <button type="button" onClick={onBack}>
        Back to catalog
      </button>
      <h1>{value.title}</h1>
      <div className="video-detail__layout">
        <video aria-label="Video player" controls autoPlay src={`${getEnv().MEDIA_BASE_URL}/${value.videoPath}`} />
        <section aria-labelledby="video-description-title">
          <h2 id="video-description-title">Description</h2>
          <p>{value.description || 'No description available.'}</p>
        </section>
        <section aria-labelledby="video-comments-title">
          <h2 id="video-comments-title">Comments</h2>
          <form aria-label="Comment form" onSubmit={handleSubmit} noValidate>
            <label htmlFor="comment-text">Add a comment</label>
            <textarea
              id="comment-text"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              disabled={pending}
              aria-invalid={Boolean(error)}
            />
            {error && <p role="alert">{error}</p>}
            {user ? (
              <button type="submit" disabled={pending}>{pending ? 'Posting...' : 'Post comment'}</button>
            ) : (
              <button type="button" onClick={onLoginRequest}>Log in to comment</button>
            )}
          </form>
          {localComments.length > 0 ? (
            <ul aria-label="Comments">
              {localComments.map((comment, index) => (
                <li key={`${comment.timestamp}-${index}`}>
                  <p>{comment.text}</p>
                  <p>
                    {comment.author} · {formatTimestamp(comment.timestamp)} · {comment.likeCount} likes
                  </p>
                </li>
              ))}
            </ul>
          ) : (
            <p>No comments yet.</p>
          )}
        </section>
      </div>
    </article>
  );
}
