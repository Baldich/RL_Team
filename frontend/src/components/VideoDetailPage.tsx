import { getEnv } from '../utils/Env';
import { useVideoDetails } from '../useVideoDetails';

interface Props {
  videoId: number;
  onBack: () => void;
}

function formatTimestamp(timestamp: number) {
  return new Date(timestamp * 1000).toLocaleString();
}

export default function VideoDetailPage({ videoId, onBack }: Props) {
  const { message, retry, status, value } = useVideoDetails(videoId);

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
          {value.comments.length > 0 ? (
            <ul aria-label="Comments">
              {value.comments.map((comment, index) => (
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
