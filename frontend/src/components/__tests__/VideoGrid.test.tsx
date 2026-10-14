import { render, screen } from '@testing-library/react';
import VideoGrid from '../VideoGrid';

describe('VideoGrid', () => {
  it('renders real titles and configured thumbnail URLs', () => {
    render(
      <VideoGrid
        videos={[
          { id: 1, title: 'First video', thumbnailPath: '0.webp' },
          { id: 2, title: 'Second video', thumbnailPath: '1.webp' },
        ]}
      />
    );
    expect(screen.getByRole('list', { name: 'Videos' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    expect(screen.getByRole('heading', { name: 'First video' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'First video' })).toHaveAttribute('src', 'development/0.webp');
    expect(screen.getByRole('img', { name: 'Second video' })).toHaveAttribute('src', 'development/1.webp');
  });
});
