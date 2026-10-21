import axios from 'axios';
import { getProfile, updateProfile } from '../profile';

jest.mock('axios');
const api = jest.mocked(axios);
const fields = { displayName: 'Viewer', avatarUrl: 'https://example.com/a.png', description: 'About' };

describe('Profile API', () => {
  beforeEach(() => jest.resetAllMocks());

  it('loads the current user through the authenticated endpoint', async () => {
    api.get.mockResolvedValue({ data: { id: 7, ...fields } });
    expect(await getProfile(null)).toEqual({ id: 7, ...fields });
    expect(api.get).toHaveBeenCalledWith('other/profile', { withCredentials: true });
  });

  it('loads another public profile by id', async () => {
    api.get.mockResolvedValue({ data: { id: 8, ...fields } });
    expect(await getProfile(8)).toEqual({ id: 8, ...fields });
    expect(api.get).toHaveBeenCalledWith('other/users/8/profile', { withCredentials: true });
  });

  it('saves public fields with a fresh CSRF token and session cookie', async () => {
    api.get.mockResolvedValue({ data: { headerName: 'X-CSRF-TOKEN', token: 'csrf' } });
    api.put.mockResolvedValue({ data: { id: 7, ...fields } });
    expect(await updateProfile(fields)).toEqual({ id: 7, ...fields });
    expect(api.get).toHaveBeenCalledWith('other/auth/csrf', { withCredentials: true });
    expect(api.put).toHaveBeenCalledWith('other/profile', fields, {
      withCredentials: true,
      headers: { 'X-CSRF-TOKEN': 'csrf' },
    });
  });

  it('does not send an update when obtaining CSRF fails', async () => {
    api.get.mockRejectedValue(new Error('offline'));
    await expect(updateProfile(fields)).rejects.toThrow('offline');
    expect(api.put).not.toHaveBeenCalled();
  });
});
