import { fireEvent, render, screen } from '@testing-library/react';
import App from '../../App';
import { getSession, login, logout } from '../../auth';
import { useAllVideos } from '../../useAllVideos';

jest.mock('../../auth');
jest.mock('../../useAllVideos');
jest.mock('../../App.css', () => ({}));
const videos = jest.mocked(useAllVideos);
const session = jest.mocked(getSession);
const retry = jest.fn();
const content = [{ id: 1, title: 'First', thumbnailPath: '0.webp' }];

describe('Catalog', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    window.history.replaceState({}, '', '/');
    session.mockResolvedValue(null);
    videos.mockImplementation((page = 0) => ({
      loading: 'success',
      message: '',
      retry,
      value: {
        content: page === 0 ? content : [{ id: 2, title: 'Next video', thumbnailPath: '1.webp' }],
        page,
        size: 12,
        totalElements: 13,
        totalPages: 2,
      },
    }));
  });

  it('lets a visitor browse forward and back with correct boundaries', async () => {
    render(<App />);
    await screen.findByRole('heading', { name: 'First' });
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('heading', { name: 'Next video' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'First' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    expect(videos).toHaveBeenLastCalledWith(1);
    fireEvent.click(screen.getByRole('button', { name: 'Previous' }));
    expect(screen.getByRole('heading', { name: 'First' })).toBeInTheDocument();
  });

  it('shows loading and disables pagination during requests', async () => {
    videos.mockReturnValue({ loading: 'loading', message: '', value: null, retry });
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Available videos' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Loading...');
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
  });

  it('shows an empty catalog without an error', async () => {
    videos.mockReturnValue({
      loading: 'success',
      message: '',
      value: { content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 },
      retry,
    });
    render(<App />);
    expect(await screen.findByText('No videos available.')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('shows a recoverable error', async () => {
    videos.mockReturnValue({ loading: 'error', message: 'Unable to load videos.', value: null, retry });
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load videos.');
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(retry).toHaveBeenCalledTimes(1);
  });

  it('continues browsing after a signed-in user logs out', async () => {
    session.mockResolvedValue({ id: 1, email: 'user@example.com' });
    jest.mocked(logout).mockResolvedValue();
    render(<App />);
    await screen.findByRole('heading', { name: 'First' });
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));
    await screen.findByRole('link', { name: 'Log in' });
    expect(screen.getByRole('heading', { name: 'First' })).toBeInTheDocument();
  });

  it('returns to the real catalog after login', async () => {
    window.history.replaceState({}, '', '/login');
    jest.mocked(login).mockResolvedValue({ id: 1, email: 'user@example.com' });
    render(<App />);
    await screen.findByRole('form', { name: 'Login' });
    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: '12345678' } });
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByRole('heading', { name: 'First' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/');
  });
});
