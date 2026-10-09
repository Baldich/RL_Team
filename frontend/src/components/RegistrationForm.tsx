import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import { register } from '../auth';
import type { RegisteredUser, RegistrationErrors } from '../auth';

interface Props {
  onRegistered: (user: RegisteredUser) => void;
}

export default function RegistrationForm({ onRegistered }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<RegistrationErrors>({});
  const [message, setMessage] = useState('');
  const [pending, setPending] = useState(false);
  const submitting = useRef(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    const validation: RegistrationErrors = {};
    if (!/^[^\s@]+@[^\s@]+$/.test(email.trim())) {
      validation.email = 'Enter a valid email address.';
    }
    if (password.length < 8) validation.password = 'Password must contain at least 8 characters.';
    setErrors(validation);
    setMessage('');
    if (Object.keys(validation).length > 0) return;
    submitting.current = true;
    setPending(true);
    try {
      const user = await register(email, password);
      setPassword('');
      onRegistered(user);
    } catch (error: unknown) {
      if (axios.isAxiosError<{ message: string; fieldErrors: RegistrationErrors }>(error) && error.response) {
        setErrors(error.response.data.fieldErrors ?? {});
        setMessage(error.response.data.message || 'Registration failed. Please try again.');
      } else {
        setMessage('Registration failed. Please try again.');
      }
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <form className="container text-start" onSubmit={submit} noValidate aria-label="Registration">
      <h1>Create account</h1>
      {message && <p role="alert">{message}</p>}
      <div className="mb-3">
        <label className="form-label" htmlFor="registration-email">
          Email
        </label>
        <input
          id="registration-email"
          className="form-control"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          aria-invalid={Boolean(errors.email)}
          aria-describedby={errors.email ? 'email-error' : undefined}
        />
        {errors.email && (
          <p id="email-error" role="alert">
            {errors.email}
          </p>
        )}
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="registration-password">
          Password
        </label>
        <input
          id="registration-password"
          className="form-control"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          minLength={8}
          aria-invalid={Boolean(errors.password)}
          aria-describedby={errors.password ? 'password-error' : undefined}
        />
        {errors.password && (
          <p id="password-error" role="alert">
            {errors.password}
          </p>
        )}
      </div>
      <button className="btn btn-primary" type="submit" disabled={pending}>
        {pending ? 'Creating account...' : 'Create account'}
      </button>
    </form>
  );
}
