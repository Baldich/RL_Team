import { fireEvent, render, screen } from '@testing-library/react';
import LoginForm from '../LoginForm';
import { login } from '../../auth';

jest.mock('../../auth');
jest.mock('../../App.css', () => ({}));

const mockLogin = jest.mocked(login);

describe('LoginForm', () => {
  beforeEach(() => jest.resetAllMocks());

  it('validates required fields without calling the API', () => {
    render(<LoginForm onLoggedIn={jest.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(screen.getByText('Enter a valid email address.')).toBeInTheDocument();
    expect(screen.getByText('Password is required.')).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('logs in and reports the authenticated user', async () => {
    const onLoggedIn = jest.fn();
    mockLogin.mockResolvedValue({ id: 1, email: 'visitor@example.com' });
    render(<LoginForm onLoggedIn={onLoggedIn} />);
    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'visitor@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: '12345678' } });
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByRole('button', { name: 'Log in' })).toBeInTheDocument();
    expect(onLoggedIn).toHaveBeenCalledWith({ id: 1, email: 'visitor@example.com' });
  });

  it('shows a clear error when login fails', async () => {
    mockLogin.mockRejectedValue(new Error('offline'));
    render(<LoginForm onLoggedIn={jest.fn()} />);
    fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: 'visitor@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: '12345678' } });
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Login failed. Please try again.');
  });
});
