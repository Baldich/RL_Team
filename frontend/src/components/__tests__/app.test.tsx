import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from '../../App';
import { getSession, logout, register } from '../../auth';
import { useAllVideos } from '../../useAllVideos';
import { useVideoDetails } from '../../useVideoDetails';

jest.mock('../../auth');
jest.mock('../../useAllVideos');
jest.mock('../../useVideoDetails');
jest.mock('../../App.css', () => ({}));
const mockSession = jest.mocked(getSession);
const mockRegister = jest.mocked(register);
const mockVideos = jest.mocked(useAllVideos);
const mockVideoDetails = jest.mocked(useVideoDetails);

describe('App', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    window.history.replaceState({}, '', '/');
    mockSession.mockResolvedValue(null);
    mockVideos.mockReturnValue({
      loading: 'success',
      message: '',
      value: {
        content: [{ id: 1, title: 'video1', thumbnailPath: '0.webp' }],
        page: 0,
        size: 12,
        totalElements: 1,
        totalPages: 1,
      },
      retry: jest.fn(),
    });
    mockVideoDetails.mockReturnValue({
      status: 'idle',
      message: '',
      value: null,
      retry: jest.fn(),
    });
  });

  it('registers, updates authenticated state and navigates home', async () => {
    mockRegister.mockResolvedValue({ id: 1, email: 'visitor@example.com' });
    render(<App />);
    await screen.findByRole('list');
    fireEvent.click(screen.getByRole('link', { name: 'Register' }));
    expect(window.location.pathname).toBe('/register');
    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'visitor@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: '12345678' } });
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Signed in as visitor@example.com');
    expect(window.location.pathname).toBe('/');
    expect(screen.getByRole('listitem')).toHaveTextContent('video1');
    expect(screen.queryByRole('form', { name: 'Registration' })).not.toBeInTheDocument();
  });

  it('restores the authenticated state on page load', async () => {
    mockSession.mockResolvedValue({ id: 1, email: 'visitor@example.com' });
    render(<App />);
    await screen.findByRole('list');
    expect(screen.getByRole('status')).toHaveTextContent('Signed in as visitor@example.com');
    expect(screen.queryByRole('link', { name: 'Register' })).not.toBeInTheDocument();
  });

  it('supports opening the registration URL and browser navigation', async () => {
    window.history.replaceState({}, '', '/register');
    render(<App />);
    await screen.findByRole('form', { name: 'Registration' });
    window.history.replaceState({}, '', '/');
    fireEvent.popState(window);
    expect(screen.getByRole('list')).toBeInTheDocument();
  });

  it('opens a video detail page from the catalog and shows the player data', async () => {
    mockVideoDetails.mockReturnValue({
      status: 'success',
      message: '',
      value: {
        id: 1,
        title: 'video1',
        description: 'A sample video',
        videoPath: '0.mp4',
        thumbnailPath: '0.webp',
        comments: [
          {
            id: 'imported-0',
            author: 'viewer',
            text: 'Nice!',
            timestamp: 123,
            likeCount: 2,
            likedByCurrentUser: false,
            replies: [],
          },
        ],
      },
      retry: jest.fn(),
    });
    render(<App />);
    await screen.findByRole('link', { name: 'video1' });
    fireEvent.click(screen.getByRole('link', { name: 'video1' }));
    expect(await screen.findByRole('heading', { name: 'video1' })).toBeInTheDocument();
    expect(screen.getByText('A sample video')).toBeInTheDocument();
    expect(screen.getByText('Nice!')).toBeInTheDocument();
    expect(window.location.pathname).toBe('/videos/1');
  });

  it('shows a not-found state for an unavailable video route', async () => {
    window.history.replaceState({}, '', '/videos/999');
    mockVideoDetails.mockReturnValue({
      status: 'not-found',
      message: 'Video not found.',
      value: null,
      retry: jest.fn(),
    });
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Video not found');
  });

  it('clears private reply drafts when logging out on a video page', async () => {
    window.history.replaceState({}, '', '/videos/1');
    mockSession.mockResolvedValue({ id: 7, email: 'viewer@example.com' });
    jest.mocked(logout).mockResolvedValue();
    mockVideoDetails.mockReturnValue({
      status: 'success',
      message: '',
      value: {
        id: 1,
        title: 'Video',
        description: '',
        videoPath: '77.mp4',
        thumbnailPath: '77.webp',
        comments: [
          {
            id: 'imported-0',
            author: 'Alice',
            text: 'Original',
            timestamp: 1,
            likeCount: 4,
            likedByCurrentUser: false,
            replies: [],
          },
        ],
      },
      retry: jest.fn(),
    });
    render(<App />);
    fireEvent.click(await screen.findByRole('button', { name: 'Reply' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Reply to Alice' }), { target: { value: 'Private draft' } });
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));
    await screen.findByRole('button', { name: 'Log in to comment' });
    expect(screen.queryByRole('form', { name: 'Reply to Alice' })).not.toBeInTheDocument();
    expect(screen.queryByDisplayValue('Private draft')).not.toBeInTheDocument();
  });

  it('keeps registration available if the session lookup fails', async () => {
    mockSession.mockRejectedValue(new Error('offline'));
    render(<App />);
    await screen.findByRole('list');
    expect(screen.getByRole('link', { name: 'Register' })).toBeInTheDocument();
  });

  it.each(['loading', 'error', 'idle'] as const)('preserves the %s catalog state', async (loading) => {
    mockVideos.mockReturnValue({ loading, message: 'Catalog error', value: null, retry: jest.fn() });
    render(<App />);
    await waitFor(() => expect(screen.queryByText('Checking session...')).not.toBeInTheDocument());
    if (loading === 'error') expect(screen.getByRole('heading', { name: 'Error' })).toBeInTheDocument();
    else expect(screen.getByText('Loading...')).toBeInTheDocument();
  });
});
