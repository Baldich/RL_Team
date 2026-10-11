import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import { login } from '../auth';
import type { LoginErrors, RegisteredUser } from '../auth';

interface Props {
  onLoggedIn: (user: RegisteredUser) => void;
}

export default function LoginForm({ onLoggedIn }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<LoginErrors>({});
  const [message, setMessage] = useState('');
  const [pending, setPending] = useState(false);
  const submitting = useRef(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    const validation: LoginErrors = {};
    if (!/^[^\s@]+@[^\s@]+$/.test(email.trim())) validation.email = 'Enter a valid email address.';
    if (!password) validation.password = 'Password is required.';
    setErrors(validation);
    setMessage('');
    if (Object.keys(validation).length) return;
    submitting.current = true;
    setPending(true);
    try {
      onLoggedIn(await login(email, password));
    } catch (error: unknown) {
      if (axios.isAxiosError<{ message?: string; fieldErrors?: LoginErrors }>(error) && error.response) {
        setErrors(error.response.data.fieldErrors ?? {});
        setMessage(error.response.status === 401 ? 'Invalid email or password.' : 'Login failed. Please try again.');
      } else {
        setMessage('Login failed. Please try again.');
      }
    } finally {
      submitting.current = false;
      setPending(false);
    }
  }

  return (
    <form className="container text-start" onSubmit={submit} noValidate aria-label="Login">
      <h1>Log in</h1>
      {message && <p role="alert">{message}</p>}
      <div className="mb-3">
        <label className="form-label" htmlFor="login-email">Email</label>
        <input id="login-email" className="form-control" type="email" autoComplete="email" value={email}
          onChange={(event) => setEmail(event.target.value)} required aria-invalid={Boolean(errors.email)} />
        {errors.email && <p role="alert">{errors.email}</p>}
      </div>
      <div className="mb-3">
        <label className="form-label" htmlFor="login-password">Password</label>
        <input id="login-password" className="form-control" type="password" autoComplete="current-password"
          value={password} onChange={(event) => setPassword(event.target.value)} required aria-invalid={Boolean(errors.password)} />
        {errors.password && <p role="alert">{errors.password}</p>}
      </div>
      <button className="btn btn-primary" type="submit" disabled={pending}>
        {pending ? 'Logging in...' : 'Log in'}
      </button>
    </form>
  );
}
