import axios from 'axios';
import { getSession, login, logout, register } from '../../auth';

jest.mock('axios');
const mockAxios = jest.mocked(axios);

describe('registration API', () => {
  beforeEach(() => jest.resetAllMocks());

  it('normalizes email and sends the CSRF token and session credentials', async () => {
    mockAxios.get.mockResolvedValue({ data: { headerName: 'X-CSRF-TOKEN', token: 'csrf' } });
    const user = { id: 1, email: 'visitor@example.com' };
    mockAxios.post.mockResolvedValue({ data: user });
    expect(await register(' Visitor@EXAMPLE.com ', '12345678')).toEqual(user);
    expect(mockAxios.get).toHaveBeenCalledWith('other/auth/csrf', { withCredentials: true });
    expect(mockAxios.post).toHaveBeenCalledWith(
      'other/auth/register',
      { email: 'visitor@example.com', password: '12345678' },
      { withCredentials: true, headers: { 'X-CSRF-TOKEN': 'csrf' } }
    );
  });

  it('restores an authenticated session', async () => {
    const user = { id: 1, email: 'visitor@example.com' };
    mockAxios.get.mockResolvedValue({ status: 200, data: user });
    expect(await getSession()).toEqual(user);
    expect(mockAxios.get).toHaveBeenCalledWith('other/auth/session', { withCredentials: true });
  });

  it('returns no user for an anonymous session', async () => {
    mockAxios.get.mockResolvedValue({ status: 204 });
    expect(await getSession()).toBeNull();
  });

  it('logs in with a normalized email and CSRF token', async () => {
    mockAxios.get.mockResolvedValue({ data: { headerName: 'X-CSRF-TOKEN', token: 'csrf' } });
    mockAxios.post.mockResolvedValue({ data: { id: 1, email: 'visitor@example.com' } });
    await expect(login(' Visitor@EXAMPLE.com ', '12345678')).resolves.toEqual({ id: 1, email: 'visitor@example.com' });
    expect(mockAxios.post).toHaveBeenCalledWith(
      'other/auth/login',
      { email: 'visitor@example.com', password: '12345678' },
      { withCredentials: true, headers: { 'X-CSRF-TOKEN': 'csrf' } }
    );
  });

  it('logs out with CSRF protection and credentials', async () => {
    mockAxios.get.mockResolvedValue({ data: { headerName: 'X-CSRF-TOKEN', token: 'csrf' } });
    mockAxios.post.mockResolvedValue({});
    await logout();
    expect(mockAxios.post).toHaveBeenCalledWith('other/auth/logout', undefined, {
      withCredentials: true,
      headers: { 'X-CSRF-TOKEN': 'csrf' },
    });
  });
});
