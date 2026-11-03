import axios from 'axios';
import { uploadVideo } from '../upload';

jest.mock('axios');
jest.mock('../utils/Env');

describe('uploadVideo', () => {
  it('sends FormData and session CSRF header without setting Content-Type', async () => {
    jest.mocked(axios.get).mockResolvedValue({ data: { headerName: 'X-CSRF-TOKEN', token: 'token' } });
    jest.mocked(axios.post).mockResolvedValue({ data: { id: 42 } });
    const file = new File(['video'], 'clip.mp4');
    expect(await uploadVideo(file, ' Title ', ' Description ')).toEqual({ id: 42 });
    expect(axios.get).toHaveBeenCalledWith(expect.stringContaining('/auth/csrf'), { withCredentials: true });
    const [, data, config] = jest.mocked(axios.post).mock.calls[0];
    expect(data).toBeInstanceOf(FormData);
    expect((data as FormData).get('file')).toBe(file);
    expect(config).toEqual({ withCredentials: true, headers: { 'X-CSRF-TOKEN': 'token' } });
  });
});
