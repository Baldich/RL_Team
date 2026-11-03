import { useRef, useState, type FormEvent } from 'react';
import axios from 'axios';
import { uploadVideo } from '../upload';

interface Props {
  onUploaded: (id: number) => void;
  onLogin: () => void;
  onBack: () => void;
}

export default function UploadVideoPage({ onUploaded, onLogin, onBack }: Props) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const submitting = useRef(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    if (!title.trim() || title.trim().length > 255 || !description.trim() || description.trim().length > 5000) {
      setError('Enter a title (1–255 characters) and description (1–5,000 characters).');
      return;
    }
    if (!file || file.size === 0 || file.size > 100 * 1024 * 1024 || !/\.mp4$/i.test(file.name)) {
      setError('Select a non-empty MP4 video of at most 100 MB.');
      return;
    }
    submitting.current = true;
    setPending(true);
    setError('');
    try {
      const created = await uploadVideo(file, title, description);
      onUploaded(created.id);
    } catch (failure: unknown) {
      if (axios.isAxiosError(failure)) {
        if (failure.response?.status === 401) {
          onLogin();
        } else if (failure.response?.status === 403) {
          setError('Your security token expired. Please try again or log in again.');
        } else if (failure.response?.status === 413) {
          setError('Video must be at most 100 MB.');
        } else {
          setError(
            failure.response?.data?.message ?? 'Upload failed. Check your connection and catalog before retrying.'
          );
        }
      } else {
        setError('Upload failed. Check your connection and catalog before retrying.');
      }
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <section className="upload-page" aria-labelledby="upload-title">
      <h1 id="upload-title">Upload Video</h1>
      <p>MP4 with H.264 video (8-bit 4:2:0), AAC audio or no audio. Maximum 100 MB (100 MiB).</p>
      <form aria-label="Upload video" onSubmit={submit} noValidate>
        {error && <p role="alert">{error}</p>}
        {pending && <p role="status">Uploading and processing video… Please wait.</p>}
        <fieldset disabled={pending}>
          <label htmlFor="upload-file">Video file</label>
          <input
            id="upload-file"
            type="file"
            accept="video/mp4,.mp4"
            required
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <label htmlFor="upload-video-title">Title</label>
          <input
            id="upload-video-title"
            required
            maxLength={255}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
          <label htmlFor="upload-description">Description</label>
          <textarea
            id="upload-description"
            required
            maxLength={5000}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <button type="submit">{pending ? 'Uploading…' : 'Upload'}</button>
        </fieldset>
      </form>
      <button type="button" disabled={pending} onClick={onBack}>
        Back to catalog
      </button>
    </section>
  );
}
