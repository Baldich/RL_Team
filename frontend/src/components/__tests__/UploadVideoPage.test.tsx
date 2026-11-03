import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import UploadVideoPage from '../UploadVideoPage';
import { uploadVideo } from '../../upload';
import axios from 'axios';

jest.mock('../../upload');
jest.mock('axios');
const upload = jest.mocked(uploadVideo);
const uploaded = jest.fn();
const login = jest.fn();
const back = jest.fn();

function fill(file = new File(['video'], 'clip.mp4', { type: 'video/mp4' })) {
  fireEvent.change(screen.getByLabelText('Video file'), { target: { files: [file] } });
  fireEvent.change(screen.getByRole('textbox', { name: 'Title' }), { target: { value: 'Title' } });
  fireEvent.change(screen.getByRole('textbox', { name: 'Description' }), { target: { value: 'Description' } });
}

describe('UploadVideoPage', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    render(<UploadVideoPage onUploaded={uploaded} onLogin={login} onBack={back} />);
  });
  it('validates required fields and file format and size', async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a title');
    fill(new File(['bad'], 'clip.webm'));
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    expect(screen.getByRole('alert')).toHaveTextContent('MP4');
    const huge = new File(['x'], 'clip.mp4');
    Object.defineProperty(huge, 'size', { value: 100 * 1024 * 1024 + 1 });
    fill(huge);
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    expect(upload).not.toHaveBeenCalled();
  });
  it('shows processing, blocks duplicate submissions and navigates on success', async () => {
    let finish!: (value: Awaited<ReturnType<typeof uploadVideo>>) => void;
    upload.mockImplementation(
      () =>
        new Promise((resolve) => {
          finish = resolve;
        })
    );
    fill();
    fireEvent.submit(screen.getByRole('form', { name: 'Upload video' }));
    fireEvent.submit(screen.getByRole('form', { name: 'Upload video' }));
    expect(upload).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent('processing');
    expect(screen.getByRole('button', { name: 'Uploading…' })).toBeDisabled();
    await act(async () =>
      finish({
        id: 42,
        title: 'Title',
        description: 'Description',
        videoPath: 'a.mp4',
        thumbnailPath: 'a.webp',
        comments: [],
      })
    );
    expect(uploaded).toHaveBeenCalledWith(42);
  });
  it.each([401, 403, 413, 415, 500])('handles HTTP %s without losing metadata', async (status) => {
    jest.mocked(axios.isAxiosError).mockReturnValue(true);
    upload.mockRejectedValue({ response: { status, data: { message: 'Unsupported video' } } });
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Upload' })).toBeEnabled());
    if (status === 401) expect(login).toHaveBeenCalled();
    else expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Title' })).toHaveValue('Title');
  });
  it('handles network failures and supports returning to catalog', async () => {
    upload.mockRejectedValue(new Error('offline'));
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('connection');
    fireEvent.click(screen.getByRole('button', { name: 'Back to catalog' }));
    expect(back).toHaveBeenCalled();
  });
});
