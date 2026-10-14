export interface VideoSummary {
  id: number;
  title: string;
  thumbnailPath: string;
}

export interface VideoComment {
  author: string;
  text: string;
  timestamp: number;
  likeCount: number;
}

export interface VideoPage {
  content: VideoSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface VideoDetails {
  id: number;
  title: string;
  description: string;
  videoPath: string;
  thumbnailPath: string;
  comments: VideoComment[];
}
