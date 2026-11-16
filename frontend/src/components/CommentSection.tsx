import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { setCommentLiked, submitComment, submitReply } from '../commentApi';
import type { VideoComment } from '../types/video';

interface Props {
  videoId: number;
  comments: VideoComment[];
  authenticated: boolean;
  onLoginRequest: () => void;
}

export default function CommentSection({ videoId, comments, authenticated, onLoginRequest }: Props) {
  const [threads, setThreads] = useState(comments);
  const mounted = useMounted();

  useEffect(() => setThreads(comments), [comments]);

  function update(id: string, change: (comment: VideoComment) => VideoComment) {
    setThreads((current) => updateThread(current, id, change));
  }

  return (
    <section className="comment-section" aria-labelledby="video-comments-title">
      <h2 id="video-comments-title">Comments</h2>
      <CommentForm
        label="Add a comment"
        submitLabel="Post comment"
        onLoginRequest={authenticated ? undefined : onLoginRequest}
        onSubmit={async (text) => {
          const created = await submitComment(videoId, text);
          if (mounted.current) setThreads((current) => [...current, created]);
        }}
        onUnauthorized={onLoginRequest}
      />
      {threads.length ? (
        <ul className="comment-list" aria-label="Comments">
          {threads.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              videoId={videoId}
              authenticated={authenticated}
              onLoginRequest={onLoginRequest}
              onUpdate={update}
            />
          ))}
        </ul>
      ) : (
        <p>No comments yet.</p>
      )}
    </section>
  );
}

interface ItemProps extends Omit<Props, 'comments'> {
  comment: VideoComment;
  onUpdate: (id: string, change: (comment: VideoComment) => VideoComment) => void;
}

function CommentItem({ comment, videoId, authenticated, onLoginRequest, onUpdate }: ItemProps) {
  const [replyOpen, setReplyOpen] = useState(false);
  const [liking, setLiking] = useState(false);
  const [error, setError] = useState('');
  const likingNow = useRef(false);
  const mounted = useMounted();

  async function toggleLike() {
    if (!authenticated) {
      onLoginRequest();
      return;
    }
    if (likingNow.current) return;
    likingNow.current = true;
    setLiking(true);
    setError('');
    try {
      const state = await setCommentLiked(videoId, comment.id, !comment.likedByCurrentUser);
      if (mounted.current) onUpdate(comment.id, (current) => ({ ...current, ...state }));
    } catch (failure: unknown) {
      if (mounted.current) {
        if (httpStatus(failure) === 401) onLoginRequest();
        setError('Unable to update your like. Please try again.');
      }
    } finally {
      likingNow.current = false;
      if (mounted.current) setLiking(false);
    }
  }

  return (
    <li>
      <article className="comment" aria-label={`Comment by ${comment.author}`}>
        <p className="comment__text">{comment.text}</p>
        <p className="comment__author">
          {comment.author} · {new Date(comment.timestamp * 1000).toLocaleString()}
        </p>
        <div className="comment__actions">
          <span aria-live="polite">{comment.likeCount} likes</span>
          <button type="button" aria-pressed={comment.likedByCurrentUser} disabled={liking} onClick={toggleLike}>
            {liking ? 'Saving like...' : comment.likedByCurrentUser ? 'Unlike' : 'Like'}
          </button>
          <button
            type="button"
            aria-expanded={replyOpen}
            onClick={() => {
              if (authenticated) setReplyOpen((open) => !open);
              else onLoginRequest();
            }}
          >
            {replyOpen ? 'Cancel reply' : 'Reply'}
          </button>
        </div>
        {error && <p role="alert">{error}</p>}
      </article>
      {replyOpen && (
        <CommentForm
          label={`Reply to ${comment.author}`}
          submitLabel="Post reply"
          onUnauthorized={onLoginRequest}
          onSubmit={async (text) => {
            const created = await submitReply(videoId, comment.id, text);
            if (mounted.current) {
              onUpdate(comment.id, (current) => ({ ...current, replies: [...current.replies, created] }));
              setReplyOpen(false);
            }
          }}
        />
      )}
      {comment.replies.length > 0 && (
        <ul className="comment-list" aria-label={`Replies to ${comment.author}`}>
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              videoId={videoId}
              authenticated={authenticated}
              onLoginRequest={onLoginRequest}
              onUpdate={onUpdate}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

interface FormProps {
  label: string;
  submitLabel: string;
  onSubmit: (text: string) => Promise<void>;
  onUnauthorized: () => void;
  onLoginRequest?: () => void;
}

function CommentForm({ label, submitLabel, onSubmit, onUnauthorized, onLoginRequest }: FormProps) {
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);
  const submitting = useRef(false);
  const mounted = useMounted();
  const id = useId();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (onLoginRequest) {
      onLoginRequest();
      return;
    }
    if (submitting.current) return;
    const text = draft.trim();
    if (!text || text.length > 5000) {
      setError('Enter a comment of 1–5000 characters.');
      return;
    }
    submitting.current = true;
    setPending(true);
    setError('');
    try {
      await onSubmit(text);
      if (mounted.current) setDraft('');
    } catch (failure: unknown) {
      if (mounted.current) {
        if (httpStatus(failure) === 401) onUnauthorized();
        setError(
          httpStatus(failure) === 403
            ? 'Your security token expired. Please try again or log in again.'
            : 'Unable to post your comment. Please try again.'
        );
      }
    } finally {
      submitting.current = false;
      if (mounted.current) setPending(false);
    }
  }

  return (
    <form className="comment-form" aria-label={label} onSubmit={submit} noValidate>
      <label htmlFor={id}>{label}</label>
      <textarea
        id={id}
        value={draft}
        maxLength={5000}
        rows={3}
        disabled={pending}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => setDraft(event.target.value)}
      />
      {error && (
        <p id={`${id}-error`} role="alert">
          {error}
        </p>
      )}
      {onLoginRequest ? (
        <button type="button" onClick={onLoginRequest}>
          Log in to comment
        </button>
      ) : (
        <button type="submit" disabled={pending}>
          {pending ? 'Posting...' : submitLabel}
        </button>
      )}
    </form>
  );
}

function updateThread(
  comments: VideoComment[],
  id: string,
  change: (comment: VideoComment) => VideoComment
): VideoComment[] {
  return comments.map((comment) =>
    comment.id === id ? change(comment) : { ...comment, replies: updateThread(comment.replies, id, change) }
  );
}

function httpStatus(error: unknown) {
  return (error as { response?: { status?: number } } | null)?.response?.status;
}

function useMounted() {
  const mounted = useRef(false);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  return mounted;
}
