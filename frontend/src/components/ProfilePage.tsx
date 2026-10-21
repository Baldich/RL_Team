import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import type { RegisteredUser } from '../auth';
import { getProfile, updateProfile } from '../profile';
import type { Profile, ProfileErrors, ProfileFields } from '../profile';

interface Props {
  profileId: number | null;
  user: RegisteredUser | null;
  onLogin: () => void;
  onBack: () => void;
}

const emptyFields: ProfileFields = { displayName: '', avatarUrl: '', description: '' };

export default function ProfilePage({ profileId, user, onLogin, onBack }: Props) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [fields, setFields] = useState<ProfileFields>(emptyFields);
  const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'not-found' | 'unauthorized'>('loading');
  const [attempt, setAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [errors, setErrors] = useState<ProfileErrors>({});
  const submitting = useRef(false);
  const mounted = useRef(false);
  const userId = user?.id;

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    let active = true;
    if (profileId === null && userId === undefined) {
      setStatus('unauthorized');
      return;
    }
    setStatus('loading');
    setMessage('');
    getProfile(profileId)
      .then((loaded) => {
        if (!active) return;
        setProfile(loaded);
        setFields({ displayName: loaded.displayName, avatarUrl: loaded.avatarUrl, description: loaded.description });
        setStatus('success');
      })
      .catch((error: unknown) => {
        if (!active) return;
        const code = axios.isAxiosError(error) ? error.response?.status : undefined;
        setStatus(code === 404 ? 'not-found' : code === 401 ? 'unauthorized' : 'error');
      });
    return () => {
      active = false;
    };
  }, [profileId, userId, attempt]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    submitting.current = true;
    setSaving(true);
    setErrors({});
    setMessage('');
    try {
      const updated = await updateProfile(fields);
      if (!mounted.current) return;
      setProfile(updated);
      setFields({ displayName: updated.displayName, avatarUrl: updated.avatarUrl, description: updated.description });
      setMessage('Profile saved.');
    } catch (error: unknown) {
      if (!mounted.current) return;
      if (axios.isAxiosError<{ fieldErrors?: ProfileErrors }>(error)) {
        if (error.response?.status === 401) {
          setStatus('unauthorized');
          return;
        }
        setErrors(error.response?.data.fieldErrors ?? {});
      }
      setMessage('Unable to save profile. Please try again.');
    } finally {
      submitting.current = false;
      if (mounted.current) setSaving(false);
    }
  }

  if (status === 'loading') return <p role="status">Loading profile...</p>;

  if (status === 'unauthorized') {
    return (
      <section aria-label="Sign in required">
        <h1>Sign in to view your profile</h1>
        <button type="button" onClick={onLogin}>
          Log in
        </button>
      </section>
    );
  }

  if (status === 'not-found' || status === 'error' || !profile) {
    return (
      <section role="alert">
        <h1>{status === 'not-found' ? 'Profile not found' : 'Unable to load profile'}</h1>
        {status === 'error' && (
          <button type="button" onClick={() => setAttempt((current) => current + 1)}>
            Retry
          </button>
        )}
        <button type="button" onClick={onBack}>
          Back to catalog
        </button>
      </section>
    );
  }

  const owner = userId === profile.id;
  const name = profile.displayName || `User ${profile.id}`;

  return (
    <article className="profile-page">
      <button type="button" onClick={onBack}>
        Back to catalog
      </button>
      <h1>{name}</h1>
      {profile.avatarUrl ? (
        <img className="profile-avatar" src={profile.avatarUrl} alt={`${name}'s avatar`} referrerPolicy="no-referrer" />
      ) : (
        <span className="profile-avatar profile-avatar--placeholder" role="img" aria-label="Default avatar">
          {name.slice(0, 1).toUpperCase()}
        </span>
      )}
      <h2>About</h2>
      <p className="profile-description">{profile.description || 'No description yet.'}</p>
      <p>
        <a href={`/users/${profile.id}`}>Public profile</a>
      </p>
      {owner && (
        <form aria-label="Edit profile" onSubmit={save}>
          <h2>Edit profile</h2>
          {message && <p role={message === 'Profile saved.' ? 'status' : 'alert'}>{message}</p>}
          <fieldset disabled={saving}>
            <div className="mb-3">
              <label htmlFor="profile-name">Display name</label>
              <input
                id="profile-name"
                className="form-control"
                value={fields.displayName}
                maxLength={80}
                onChange={(event) => setFields({ ...fields, displayName: event.target.value })}
                aria-invalid={Boolean(errors.displayName)}
                aria-describedby={errors.displayName ? 'profile-name-error' : undefined}
              />
              {errors.displayName && (
                <p id="profile-name-error" role="alert">
                  {errors.displayName}
                </p>
              )}
            </div>
            <div className="mb-3">
              <label htmlFor="profile-avatar">Avatar URL</label>
              <input
                id="profile-avatar"
                className="form-control"
                type="url"
                value={fields.avatarUrl}
                maxLength={2048}
                onChange={(event) => setFields({ ...fields, avatarUrl: event.target.value })}
                aria-invalid={Boolean(errors.avatarUrl)}
                aria-describedby={errors.avatarUrl ? 'profile-avatar-help profile-avatar-error' : 'profile-avatar-help'}
              />
              <p id="profile-avatar-help">Use an HTTP or HTTPS image URL, or leave it empty for the default avatar.</p>
              {errors.avatarUrl && (
                <p id="profile-avatar-error" role="alert">
                  {errors.avatarUrl}
                </p>
              )}
            </div>
            <div className="mb-3">
              <label htmlFor="profile-description">Description</label>
              <textarea
                id="profile-description"
                className="form-control"
                value={fields.description}
                maxLength={1000}
                rows={4}
                onChange={(event) => setFields({ ...fields, description: event.target.value })}
                aria-invalid={Boolean(errors.description)}
                aria-describedby={errors.description ? 'profile-description-error' : undefined}
              />
              {errors.description && (
                <p id="profile-description-error" role="alert">
                  {errors.description}
                </p>
              )}
            </div>
            <button type="submit">{saving ? 'Saving...' : 'Save profile'}</button>
          </fieldset>
        </form>
      )}
    </article>
  );
}
