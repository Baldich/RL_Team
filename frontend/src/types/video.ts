export interface VideoSummary {
  id: number;
  title: string;
  thumbnailPath: string;
}

export interface VideoPage {
  content: VideoSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
