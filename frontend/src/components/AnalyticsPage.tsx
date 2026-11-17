import { useEffect, useState } from 'react';
import { getAnalytics, type Analytics } from '../analytics';

interface Props {
  onOpenVideo: (videoId: number) => void;
  onBack: () => void;
}

export default function AnalyticsPage({ onOpenVideo, onBack }: Props) {
  const [data, setData] = useState<Analytics | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');

  useEffect(() => {
    let active = true;
    getAnalytics()
      .then((result) => {
        if (active) {
          setData(result);
          setStatus('success');
        }
      })
      .catch(() => {
        if (active) setStatus('error');
      });
    return () => {
      active = false;
    };
  }, []);

  if (status === 'loading') return <p role="status">Loading analytics...</p>;
  if (status === 'error') {
    return (
      <section role="alert">
        <h1>Unable to load analytics</h1>
        <p>Please try again later.</p>
        <button type="button" onClick={onBack}>
          Back to catalog
        </button>
      </section>
    );
  }
  if (!data || data.videos.length === 0) {
    return (
      <section className="analytics-page">
        <button type="button" onClick={onBack}>
          Back to catalog
        </button>
        <h1>My analytics</h1>
        <p>You have not uploaded any videos yet.</p>
      </section>
    );
  }

  return (
    <section className="analytics-page" aria-labelledby="analytics-title">
      <button type="button" onClick={onBack}>
        Back to catalog
      </button>
      <h1 id="analytics-title">My analytics</h1>
      <dl className="analytics-summary">
        <div>
          <dt>Views</dt>
          <dd>{data.totalViews}</dd>
        </div>
        <div>
          <dt>Comments</dt>
          <dd>{data.totalComments}</dd>
        </div>
        <div>
          <dt>Likes</dt>
          <dd>{data.totalLikes}</dd>
        </div>
      </dl>
      <h2>Videos</h2>
      <table>
        <caption>Performance of my videos</caption>
        <thead>
          <tr>
            <th>Title</th>
            <th>Views</th>
            <th>Comments</th>
            <th>Likes</th>
          </tr>
        </thead>
        <tbody>
          {data.videos.map((video) => (
            <tr key={video.id}>
              <th scope="row">
                <button type="button" onClick={() => onOpenVideo(video.id)}>
                  {video.title}
                </button>
              </th>
              <td>{video.views}</td>
              <td>{video.comments}</td>
              <td>{video.likes}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
