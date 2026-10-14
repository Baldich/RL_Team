import { getEnv } from '../utils/Env';
import type { VideoSummary } from '../types/video';

const VideoGrid = ({ videos }: { videos: VideoSummary[] }) => (
  <ul className="video-grid" aria-label="Videos">
    {videos.map((video) => (
      <li className="video-card" key={video.id}>
        <img src={getEnv().MEDIA_BASE_URL + '/' + video.thumbnailPath} alt={video.title} loading="lazy" />
        <h2>{video.title}</h2>
      </li>
    ))}
  </ul>
);

export default VideoGrid;
