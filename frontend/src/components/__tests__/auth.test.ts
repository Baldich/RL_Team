import axios from 'axios';
import { getSession, register } from '../../auth';

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
});
