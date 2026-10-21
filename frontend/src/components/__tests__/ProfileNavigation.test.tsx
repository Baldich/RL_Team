import { fireEvent, render, screen } from '@testing-library/react';
import App from '../../App';
import { getSession, logout } from '../../auth';
import { getProfile } from '../../profile';
import { useAllVideos } from '../../useAllVideos';

jest.mock('../../auth');
jest.mock('../../profile');
jest.mock('../../useAllVideos');
jest.mock('../../App.css', () => ({}));

describe('Profile navigation', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    window.history.replaceState({}, '', '/');
    jest.mocked(getSession).mockResolvedValue({ id: 7, email: 'private@example.com' });
    jest.mocked(getProfile).mockResolvedValue({ id: 7, displayName: 'Viewer', avatarUrl: '', description: 'About' });
    jest.mocked(logout).mockResolvedValue();
    jest.mocked(useAllVideos).mockReturnValue({
      loading: 'success',
      message: '',
      value: null,
      retry: jest.fn(),
    });
  });

  it('opens the own profile from navigation and returns to the catalog', async () => {
    render(<App />);
    await screen.findByRole('heading', { name: 'Available videos' });
    fireEvent.click(screen.getByRole('link', { name: 'My profile' }));
    expect(await screen.findByRole('form', { name: 'Edit profile' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/profile');
    fireEvent.click(screen.getByRole('button', { name: 'Back to catalog' }));
    expect(await screen.findByRole('heading', { name: 'Available videos' })).toBeInTheDocument();
  });

  it('opens a public profile URL as a visitor without exposing edit controls', async () => {
    window.history.replaceState({}, '', '/users/7');
    jest.mocked(getSession).mockResolvedValue(null);
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Viewer' })).toBeInTheDocument();
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'My profile' })).not.toBeInTheDocument();
  });

  it('removes the private form immediately after logout', async () => {
    window.history.replaceState({}, '', '/profile');
    render(<App />);
    await screen.findByRole('form', { name: 'Edit profile' });
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));
    expect(await screen.findByRole('heading', { name: 'Sign in to view your profile' })).toBeInTheDocument();
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByRole('form', { name: 'Login' })).toBeInTheDocument();
    expect(window.location.pathname).toBe('/login');
  });

  it.each(['/users/abc', '/users/0', '/users/9007199254740992'])('rejects invalid profile route %s', async (path) => {
    window.history.replaceState({}, '', path);
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Profile not found');
    expect(getProfile).not.toHaveBeenCalled();
  });
});
