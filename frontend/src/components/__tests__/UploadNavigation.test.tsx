import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from '../../App';
import { getSession } from '../../auth';
import { uploadVideo } from '../../upload';
import { useVideoDetails } from '../../useVideoDetails';

jest.mock('../../auth');
jest.mock('../../upload');
jest.mock('../../useVideoDetails');
jest.mock('../../App.css', () => ({}));

describe('Upload navigation', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    window.history.replaceState({}, '', '/upload');
    jest.mocked(useVideoDetails).mockReturnValue({ value: null, status: 'loading', message: '', retry: jest.fn() });
  });
  it('waits for session initialization before redirecting anonymous users', async () => {
    let finish!: (value: null) => void;
    jest.mocked(getSession).mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        })
    );
    render(<App />);
    expect(screen.getByText('Checking session...')).toBeInTheDocument();
    expect(window.location.pathname).toBe('/upload');
    await act(async () => finish(null));
    expect(await screen.findByRole('heading', { name: /log in/i })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');
  });
  it('offers upload navigation and opens the uploaded detail page', async () => {
    jest.mocked(getSession).mockResolvedValue({ id: 7, email: 'owner@example.com' });
    jest.mocked(uploadVideo).mockResolvedValue({
      id: 42,
      title: 'Title',
      description: 'Description',
      videoPath: 'a.mp4',
      thumbnailPath: 'a.webp',
      comments: [],
    });
    render(<App />);
    expect(await screen.findByRole('link', { name: 'Upload Video' })).toHaveAttribute('href', '/upload');
    fireEvent.change(screen.getByLabelText('Video file'), { target: { files: [new File(['video'], 'clip.mp4')] } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Title' }), { target: { value: 'Title' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Description' }), { target: { value: 'Description' } });
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    await waitFor(() => expect(window.location.pathname).toBe('/videos/42'));
  });
});
