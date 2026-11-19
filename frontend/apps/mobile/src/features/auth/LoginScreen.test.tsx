import { ApiError, type ApiClient } from '@schoolos/api-client';
import { act, screen, userEvent } from '@testing-library/react-native';
import { fakeApi, renderMobile } from '../../test/renderMobile';

/** Spec 0003, AC-3 and AC-10 (every login screen state) — mobile. */
describe('LoginScreen (mobile)', () => {
  async function openLogin(overrides: Partial<ApiClient> = {}) {
    const login = jest.fn(async () => undefined);
    await renderMobile({ api: fakeApi({ login, ...overrides }) });
    await screen.findByRole('heading', { name: 'Giriş yap' });
    return { login: (overrides.login as jest.Mock | undefined) ?? login };
  }

  const identifierField = () => screen.getByLabelText('E-posta veya telefon');
  const passwordField = () => screen.getByLabelText('Şifre');
  const submitButton = () => screen.getByRole('button', { name: /Giriş yap/ });

  it('starts empty, password hidden, button enabled', async () => {
    await openLogin();

    expect(identifierField()).toHaveDisplayValue('');
    expect(passwordField()).toHaveProp('secureTextEntry', true);
    expect(submitButton()).toBeEnabled();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('shows field errors and sends nothing when fields are empty', async () => {
    const user = userEvent.setup();
    const { login } = await openLogin();

    await user.press(submitButton());

    expect(screen.getAllByText('Bu alan zorunlu')).toHaveLength(2);
    expect(identifierField()).toHaveProp('accessibilityHint', 'Bu alan zorunlu');
    expect(login).not.toHaveBeenCalled();
  });

  it.each(['532123456', '0532abc4567'])('rejects %s before sending', async (bad) => {
    const user = userEvent.setup();
    const { login } = await openLogin();

    await user.type(identifierField(), bad);
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.press(submitButton());

    expect(screen.getByText('Geçerli bir e-posta veya telefon girin')).toBeOnTheScreen();
    expect(login).not.toHaveBeenCalled();
  });

  it('disables the button while submitting and does not submit twice', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => undefined;
    const login = jest.fn(() => new Promise<void>((resolve) => (finish = resolve)));
    await openLogin({ login });

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.press(submitButton());

    expect(submitButton()).toBeDisabled();
    expect(submitButton()).toHaveAccessibleName('Giriş yapılıyor…');
    await user.press(submitButton());
    expect(login).toHaveBeenCalledTimes(1);
    await act(async () => finish());
  });

  it('refused login: generic message, identifier kept, password cleared', async () => {
    const user = userEvent.setup();
    await openLogin({
      login: async () => {
        throw new ApiError(401, 'IDENTITY_INVALID_CREDENTIALS');
      },
    });

    await user.type(identifierField(), '0532 123 45 67');
    await user.type(passwordField(), 'Yanlis-Sifre');
    await user.press(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent('E-posta/telefon veya şifre hatalı');
    expect(identifierField()).toHaveDisplayValue('0532 123 45 67');
    expect(passwordField()).toHaveDisplayValue('');
  });

  it.each([
    ['the server cannot be reached', new ApiError(undefined, undefined)],
    ['the server does not answer in 10 s', new ApiError(undefined, 'TIMEOUT')],
  ])('network error when %s: message, identifier kept, password cleared', async (_case, error) => {
    const user = userEvent.setup();
    await openLogin({
      login: async () => {
        throw error;
      },
    });

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.press(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent('Sunucuya ulaşılamıyor, tekrar deneyin');
    expect(identifierField()).toHaveDisplayValue('veli@ornek.test');
    expect(passwordField()).toHaveDisplayValue('');
  });

  it('too many attempts: its own message, identifier kept, password cleared (A-3; review 1, L-1)', async () => {
    const user = userEvent.setup();
    await openLogin({
      login: async () => {
        throw new ApiError(429, 'IDENTITY_RATE_LIMITED');
      },
    });

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.press(submitButton());

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Çok fazla deneme yapıldı, lütfen biraz sonra tekrar deneyin',
    );
    expect(identifierField()).toHaveDisplayValue('veli@ornek.test');
    expect(passwordField()).toHaveDisplayValue('');
  });

  it('the keyboard "go" on the password submits', async () => {
    const user = userEvent.setup();
    const { login } = await openLogin();

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026', { submitEditing: true });

    expect(login).toHaveBeenCalledWith('veli@ornek.test', 'Ornek-Sifre-2026');
  });

  it('the show-password switch is labelled and reveals the password', async () => {
    const user = userEvent.setup();
    await openLogin();

    await user.press(screen.getByRole('checkbox', { name: 'Şifreyi göster' }));

    expect(passwordField()).toHaveProp('secureTextEntry', false);
    expect(screen.getByRole('checkbox', { name: 'Şifreyi göster' })).toBeChecked();
  });

  it('successful login lands on the home screen', async () => {
    const user = userEvent.setup();
    await openLogin();

    await user.type(identifierField(), 'veli@ornek.test');
    await user.type(passwordField(), 'Ornek-Sifre-2026');
    await user.press(submitButton());

    expect(await screen.findByText('Hoş geldiniz, Ayşe Yılmaz')).toBeOnTheScreen();
  });
});
