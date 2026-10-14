import { getEnv } from '../utils/Env';
import type { VideoSummary } from '../types/video';

interface Props {
  videos: VideoSummary[];
  onOpenVideo: (videoId: number) => void;
}

const VideoGrid = ({ videos, onOpenVideo }: Props) => (
  <ul className="video-grid" aria-label="Videos">
    {videos.map((video) => (
      <li className="video-card" key={video.id}>
        <a
          href={`/videos/${video.id}`}
          aria-label={video.title}
          onClick={(event) => {
            event.preventDefault();
            onOpenVideo(video.id);
          }}
        >
          <img src={getEnv().MEDIA_BASE_URL + '/' + video.thumbnailPath} alt={video.title} loading="lazy" />
          <h2>{video.title}</h2>
        </a>
      </li>
    ))}
  </ul>
);

export default VideoGrid;
