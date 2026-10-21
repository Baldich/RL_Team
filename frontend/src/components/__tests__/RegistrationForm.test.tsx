import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import RegistrationForm from '../RegistrationForm';
import { register } from '../../auth';

jest.mock('../../auth');
const mockRegister = jest.mocked(register);

function fill(email = 'visitor@example.com', password = '12345678') {
  fireEvent.change(screen.getByRole('textbox', { name: 'Email' }), { target: { value: email } });
  // Password inputs have no implicit ARIA role.
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
}

describe('RegistrationForm', () => {
  beforeEach(() => jest.resetAllMocks());

  it('submits valid fields and reports the authenticated user', async () => {
    const user = { id: 1, email: 'visitor@example.com' };
    mockRegister.mockResolvedValue(user);
    const onRegistered = jest.fn();
    render(<RegistrationForm onRegistered={onRegistered} />);
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(onRegistered).toHaveBeenCalledWith(user));
    expect(mockRegister).toHaveBeenCalledWith('visitor@example.com', '12345678');
    expect(screen.getByLabelText('Password')).toHaveValue('');
  });

  it('shows invalid email and short password errors without an API request', () => {
    render(<RegistrationForm onRegistered={jest.fn()} />);
    fill('invalid', '1234567');
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    expect(screen.getAllByRole('alert').map((alert) => alert.textContent)).toEqual([
      'Enter a valid email address.',
      'Password must contain at least 8 characters.',
    ]);
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('rejects missing fields', () => {
    render(<RegistrationForm onRegistered={jest.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    expect(screen.getAllByRole('alert')).toHaveLength(2);
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('displays duplicate email errors without authenticating', async () => {
    mockRegister.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 409,
        data: {
          message: 'This email is already registered.',
          fieldErrors: { email: 'This email is already registered.' },
        },
      },
    });
    const onRegistered = jest.fn();
    render(<RegistrationForm onRegistered={onRegistered} />);
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(screen.getAllByRole('alert')).toHaveLength(2));
    expect(screen.getByRole('textbox', { name: 'Email' })).toHaveAccessibleDescription(
      'This email is already registered.'
    );
    expect(onRegistered).not.toHaveBeenCalled();
  });

  it('displays server-side field validation errors', async () => {
    mockRegister.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 400,
        data: {
          message: 'Check the registration fields.',
          fieldErrors: { password: 'Password must contain at least 8 characters.' },
        },
      },
    });
    render(<RegistrationForm onRegistered={jest.fn()} />);
    fill();
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(screen.getAllByRole('alert')).toHaveLength(2));
    expect(screen.getByLabelText('Password')).toHaveAttribute('aria-invalid', 'true');
  });

  it.each([new Error('offline'), { isAxiosError: true }, { isAxiosError: true, response: { data: {} } }])(
    'shows a fallback error and allows retry: %p',
    async (error) => {
      mockRegister.mockRejectedValue(error);
      render(<RegistrationForm onRegistered={jest.fn()} />);
      fill();
      fireEvent.click(screen.getByRole('button', { name: 'Create account' }));
      expect(await screen.findByRole('alert')).toHaveTextContent('Registration failed. Please try again.');
      expect(screen.getByRole('button', { name: 'Create account' })).toBeEnabled();
    }
  );

  it('prevents repeated submissions while the request is pending', async () => {
    let finish!: (user: { id: number; email: string }) => void;
    mockRegister.mockReturnValue(
      new Promise((resolve) => {
        finish = resolve;
      })
    );
    render(<RegistrationForm onRegistered={jest.fn()} />);
    fill();
    fireEvent.submit(screen.getByRole('form', { name: 'Registration' }));
    fireEvent.submit(screen.getByRole('form', { name: 'Registration' }));
    expect(screen.getByRole('button', { name: 'Creating account...' })).toBeDisabled();
    expect(mockRegister).toHaveBeenCalledTimes(1);
    finish({ id: 1, email: 'visitor@example.com' });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Create account' })).toBeEnabled());
  });
});
