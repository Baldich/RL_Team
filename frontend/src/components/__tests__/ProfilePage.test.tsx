import { act, fireEvent, render, screen } from '@testing-library/react';
import ProfilePage from '../ProfilePage';
import { getProfile, updateProfile } from '../../profile';
import type { Profile } from '../../profile';

jest.mock('../../profile');
const load = jest.mocked(getProfile);
const save = jest.mocked(updateProfile);
const user = { id: 7, email: 'private@example.com' };
const profile: Profile = {
  id: 7,
  displayName: 'Viewer',
  avatarUrl: 'https://example.com/a.png',
  description: 'About me',
};
const back = jest.fn();
const login = jest.fn();

function page(profileId: number | null = null, signedIn: typeof user | null = user) {
  return <ProfilePage profileId={profileId} user={signedIn} onBack={back} onLogin={login} />;
}

describe('ProfilePage', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    load.mockResolvedValue(profile);
  });

  it('loads the owner profile and pre-fills the editable public fields', async () => {
    render(page());
    expect(screen.getByRole('status')).toHaveTextContent('Loading profile...');
    expect(await screen.findByRole('heading', { name: 'Viewer' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: "Viewer's avatar" })).toHaveAttribute('src', profile.avatarUrl);
    expect(screen.getByRole('textbox', { name: 'Display name' })).toHaveValue('Viewer');
    expect(screen.getByRole('textbox', { name: 'Avatar URL' })).toHaveValue(profile.avatarUrl);
    expect(screen.getByRole('textbox', { name: 'Description' })).toHaveValue('About me');
    expect(screen.queryByText(user.email)).not.toBeInTheDocument();
    expect(load).toHaveBeenCalledWith(null);
    fireEvent.click(screen.getByRole('button', { name: 'Back to catalog' }));
    expect(back).toHaveBeenCalledTimes(1);
  });

  it('persists edits and immediately displays the canonical saved response', async () => {
    save.mockResolvedValue({ ...profile, displayName: 'Updated', description: 'New description' });
    render(page());
    await screen.findByRole('form', { name: 'Edit profile' });
    fireEvent.change(screen.getByRole('textbox', { name: 'Display name' }), { target: { value: ' Updated ' } });
    fireEvent.change(screen.getByRole('textbox', { name: 'Avatar URL' }), {
      target: { value: 'https://example.com/b.png' },
    });
    fireEvent.change(screen.getByRole('textbox', { name: 'Description' }), { target: { value: 'New description' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Profile saved.');
    expect(screen.getByRole('heading', { name: 'Updated' })).toBeInTheDocument();
    expect(screen.getByText('New description', { selector: 'p' })).toBeInTheDocument();
    expect(save).toHaveBeenCalledWith({
      displayName: ' Updated ',
      avatarUrl: 'https://example.com/b.png',
      description: 'New description',
    });
    expect(screen.getByRole('textbox', { name: 'Display name' })).toHaveValue('Updated');
  });

  it('blocks duplicate submissions and disables fields while saving', async () => {
    let finish!: (value: Profile) => void;
    save.mockReturnValue(new Promise((resolve) => (finish = resolve)));
    render(page());
    const form = await screen.findByRole('form', { name: 'Edit profile' });
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(screen.getByRole('button', { name: 'Saving...' })).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Description' })).toBeDisabled();
    expect(save).toHaveBeenCalledTimes(1);
    await act(async () => finish(profile));
    expect(screen.getByRole('button', { name: 'Save profile' })).toBeEnabled();
  });

  it.each([null, { id: 8, email: 'other@example.com' }])(
    'shows another profile without edit controls for %p',
    async (viewer) => {
      render(page(7, viewer));
      expect(await screen.findByRole('heading', { name: 'Viewer' })).toBeInTheDocument();
      expect(screen.getByText('About me')).toBeInTheDocument();
      expect(screen.queryByRole('form')).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Save profile' })).not.toBeInTheDocument();
      expect(load).toHaveBeenCalledWith(7);
    }
  );

  it('lets an owner edit their own public profile route', async () => {
    render(page(7));
    expect(await screen.findByRole('form', { name: 'Edit profile' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Public profile' })).toHaveAttribute('href', '/users/7');
  });

  it('shows defaults for an existing account with no profile information', async () => {
    load.mockResolvedValue({ id: 7, displayName: '', avatarUrl: '', description: '' });
    render(page());
    expect(await screen.findByRole('heading', { name: 'User 7' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Default avatar' })).toBeInTheDocument();
    expect(screen.getByText('No description yet.')).toBeInTheDocument();
  });

  it('prompts anonymous visitors on the private route to sign in without loading data', async () => {
    render(page(null, null));
    expect(await screen.findByRole('heading', { name: 'Sign in to view your profile' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(login).toHaveBeenCalledTimes(1);
    expect(load).not.toHaveBeenCalled();
  });

  it('shows not found for a missing public profile', async () => {
    load.mockRejectedValue({ isAxiosError: true, response: { status: 404 } });
    render(page(99, null));
    expect(await screen.findByRole('alert')).toHaveTextContent('Profile not found');
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Back to catalog' }));
    expect(back).toHaveBeenCalled();
  });

  it('offers retry after a load failure', async () => {
    load.mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(profile);
    render(page());
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load profile');
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('heading', { name: 'Viewer' })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('requires signing in again when the session expires during load', async () => {
    load.mockRejectedValue({ isAxiosError: true, response: { status: 401 } });
    render(page());
    expect(await screen.findByRole('heading', { name: 'Sign in to view your profile' })).toBeInTheDocument();
  });

  it('shows field validation and preserves the previously saved profile after a rejected update', async () => {
    save.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          fieldErrors: {
            displayName: 'Name too long',
            avatarUrl: 'Invalid avatar URL',
            description: 'Description too long',
          },
        },
      },
    });
    render(page());
    await screen.findByRole('form', { name: 'Edit profile' });
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }));
    await screen.findByText('Invalid avatar URL');
    expect(screen.getByRole('textbox', { name: 'Display name' })).toHaveAccessibleDescription('Name too long');
    expect(screen.getByRole('textbox', { name: 'Avatar URL' })).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('textbox', { name: 'Description' })).toHaveAccessibleDescription('Description too long');
    expect(screen.getByRole('heading', { name: 'Viewer' })).toBeInTheDocument();
  });

  it.each([new Error('offline'), { isAxiosError: true }, { isAxiosError: true, response: { status: 403, data: {} } }])(
    'allows retry after an update error: %p',
    async (error) => {
      save.mockRejectedValueOnce(error).mockResolvedValueOnce(profile);
      render(page());
      await screen.findByRole('form', { name: 'Edit profile' });
      fireEvent.click(screen.getByRole('button', { name: 'Save profile' }));
      expect(await screen.findByRole('alert')).toHaveTextContent('Unable to save profile. Please try again.');
      fireEvent.click(screen.getByRole('button', { name: 'Save profile' }));
      expect(await screen.findByRole('status')).toHaveTextContent('Profile saved.');
    }
  );

  it('removes edit controls when the server reports an expired session while saving', async () => {
    save.mockRejectedValue({ isAxiosError: true, response: { status: 401 } });
    render(page());
    await screen.findByRole('form', { name: 'Edit profile' });
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }));
    expect(await screen.findByRole('heading', { name: 'Sign in to view your profile' })).toBeInTheDocument();
    expect(screen.queryByRole('form')).not.toBeInTheDocument();
  });

  it('ignores a stale load after switching public profiles', async () => {
    let finish!: (value: Profile) => void;
    load.mockReturnValueOnce(new Promise((resolve) => (finish = resolve)));
    const { rerender } = render(page(7, null));
    load.mockResolvedValueOnce({ ...profile, id: 8, displayName: 'Next viewer' });
    rerender(page(8, null));
    await screen.findByRole('heading', { name: 'Next viewer' });
    await act(async () => finish(profile));
    expect(screen.getByRole('heading', { name: 'Next viewer' })).toBeInTheDocument();
  });

  it('ignores a load failure after unmount', async () => {
    let fail!: (error: Error) => void;
    load.mockReturnValue(new Promise((_resolve, reject) => (fail = reject)));
    const { unmount } = render(page());
    unmount();
    await act(async () => fail(new Error('offline')));
  });

  it.each([true, false])('ignores a pending save after unmount (success=%p)', async (success) => {
    let finish!: (value: Profile) => void;
    let fail!: (error: Error) => void;
    save.mockReturnValue(
      new Promise((resolve, reject) => {
        finish = resolve;
        fail = reject;
      })
    );
    const { unmount } = render(page());
    await screen.findByRole('form', { name: 'Edit profile' });
    fireEvent.click(screen.getByRole('button', { name: 'Save profile' }));
    unmount();
    await act(async () => {
      if (success) finish(profile);
      else fail(new Error('offline'));
    });
  });
});
