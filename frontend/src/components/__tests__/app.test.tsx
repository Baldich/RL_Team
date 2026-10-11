import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from '../../App';
import { getSession, register } from '../../auth';
import { useAllVideos } from '../../useAllVideos';

jest.mock('../../auth');
jest.mock('../../useAllVideos');
jest.mock('../../App.css', () => ({}));
const mockSession = jest.mocked(getSession);
const mockRegister = jest.mocked(register);
const mockVideos = jest.mocked(useAllVideos);

describe('App', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    window.history.replaceState({}, '', '/');
    mockSession.mockResolvedValue(null);
    mockVideos.mockReturnValue({ loading: 'success', message: '', value: ['video1'] });
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
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
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

  it('keeps registration available if the session lookup fails', async () => {
    mockSession.mockRejectedValue(new Error('offline'));
    render(<App />);
    await screen.findByRole('list');
    expect(screen.getByRole('link', { name: 'Register' })).toBeInTheDocument();
  });

  it.each(['loading', 'error', 'idle'] as const)('preserves the %s catalog state', async (loading) => {
    mockVideos.mockReturnValue({ loading, message: 'Catalog error', value: [] });
    render(<App />);
    await waitFor(() => expect(screen.queryByText('Checking session...')).not.toBeInTheDocument());
    if (loading === 'error') expect(screen.getByRole('heading', { name: 'Error' })).toBeInTheDocument();
    else expect(screen.getByText(loading === 'loading' ? 'Loading...' : 'Idle...')).toBeInTheDocument();
  });
});
