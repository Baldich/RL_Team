import { useEffect, useState, type FormEvent } from 'react';
import { getEnv } from '../utils/Env';
import { useVideoDetails } from '../useVideoDetails';
import CommentSection from './CommentSection';
import { updateVideoMetadata } from '../video';
import type { VideoMetadataFields } from '../video';

interface Props {
  videoId: number;
  onBack: () => void;
  user?: { email: string; id: number } | null;
  onLoginRequest?: () => void;
}

export default function VideoDetailPage({ videoId, onBack, user = null, onLoginRequest = () => {} }: Props) {
  const { message, retry, status, value } = useVideoDetails(videoId);
  const [savingMetadata, setSavingMetadata] = useState(false);
  const [metadataError, setMetadataError] = useState('');
  const [metadataMessage, setMetadataMessage] = useState('');
  const [metadata, setMetadata] = useState<VideoMetadataFields>({ description: '', category: '', tags: [] });

  useEffect(() => {
    if (value) {
      setMetadata({ description: value.description, category: value.category ?? '', tags: value.tags ?? [] });
    }
  }, [value]);

  async function saveMetadata(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingMetadata(true);
    setMetadataError('');
    setMetadataMessage('');
    try {
      const updated = await updateVideoMetadata(videoId, metadata);
      setMetadata({ description: updated.description, category: updated.category, tags: updated.tags });
      setMetadataMessage('Video metadata saved.');
      retry();
    } catch (requestError: unknown) {
      const status =
        requestError && typeof requestError === 'object' && 'response' in requestError
          ? (requestError as { response?: { status?: number } }).response?.status
          : undefined;
      setMetadataError(status === 403 ? 'Only the video owner can edit this video.' : 'Unable to save video metadata.');
    } finally {
      setSavingMetadata(false);
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
          {value.ownerId != null && user?.id === value.ownerId && (
            <form aria-label="Edit video metadata" onSubmit={saveMetadata}>
              <h2>Edit metadata</h2>
              {metadataMessage && <p role="status">{metadataMessage}</p>}
              {metadataError && <p role="alert">{metadataError}</p>}
              <fieldset disabled={savingMetadata}>
                <label htmlFor="video-description">Description</label>
                <textarea
                  id="video-description"
                  value={metadata.description}
                  onChange={(event) => setMetadata({ ...metadata, description: event.target.value })}
                />
                <label htmlFor="video-category">Category</label>
                <input
                  id="video-category"
                  value={metadata.category}
                  onChange={(event) => setMetadata({ ...metadata, category: event.target.value })}
                />
                <label htmlFor="video-tags">Tags</label>
                <input
                  id="video-tags"
                  value={metadata.tags.join(', ')}
                  onChange={(event) =>
                    setMetadata({ ...metadata, tags: event.target.value.split(',').map((tag) => tag.trim()) })
                  }
                />
                <button type="submit">{savingMetadata ? 'Saving...' : 'Save metadata'}</button>
              </fieldset>
            </form>
          )}
        </section>
        <CommentSection
          key={`${videoId}:${user?.id ?? 'visitor'}`}
          videoId={videoId}
          comments={value.comments}
          authenticated={user != null}
          onLoginRequest={onLoginRequest}
        />
      </div>
    </article>
  );
}
