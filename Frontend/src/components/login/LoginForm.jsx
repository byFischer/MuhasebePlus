import {
  EnvelopeClosedIcon,
  LockClosedIcon,
  MobileIcon,
  PersonIcon,
} from "@radix-ui/react-icons";
import { useState } from "react";
import { Box, Button, Flex, Heading, Link, Text, TextField } from "@radix-ui/themes";

export default function LoginForm() {
  const [isRegisterView, setIsRegisterView] = useState(false);

  const openRegisterView = (event) => {
    event.preventDefault();
    setIsRegisterView(true);
  };

  const openLoginView = (event) => {
    event.preventDefault();
    setIsRegisterView(false);
  };

  return (
    <Box className="left-panel" key={isRegisterView ? "register" : "login"}>
      <Box className="brand-chip" aria-hidden />

      <Box className="intro-copy">
        <Heading as="h1" className="welcome-title">
          {isRegisterView ? "Kayıt Olun" : "Hoş Geldiniz"}
        </Heading>
        <Text as="p" className="welcome-text">
          {isRegisterView
            ? "Yeni hesabınızı oluşturun ve tüm muhasebe işlemlerinizi tek yerden yönetin"
            : "Tüm muhasebe işlemlerinizi tek bir yerde yönetin"}
        </Text>
      </Box>

      {isRegisterView ? (
        <form className="login-form register-form" onSubmit={(event) => event.preventDefault()}>
          <Box className="field-group">
            <Text as="label" htmlFor="first-name-field" className="field-label">
              İsim
            </Text>
            <TextField.Root
              id="first-name-field"
              name="firstName"
              type="text"
              size="3"
              variant="classic"
              placeholder="Ahmet"
              required
              className="login-field"
            >
              <TextField.Slot>
                <PersonIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="last-name-field" className="field-label">
              Soyisim
            </Text>
            <TextField.Root
              id="last-name-field"
              name="lastName"
              type="text"
              size="3"
              variant="classic"
              placeholder="Yılmaz"
              required
              className="login-field"
            >
              <TextField.Slot>
                <PersonIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="phone-field" className="field-label">
              Telefon Numarası
            </Text>
            <TextField.Root
              id="phone-field"
              name="phoneNumber"
              type="tel"
              size="3"
              variant="classic"
              placeholder="05XX XXX XX XX"
              required
              className="login-field"
            >
              <TextField.Slot>
                <MobileIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="register-email-field" className="field-label">
              E-posta
            </Text>
            <TextField.Root
              id="register-email-field"
              name="email"
              type="email"
              size="3"
              variant="classic"
              placeholder="ornek@email.com"
              required
              className="login-field"
            >
              <TextField.Slot>
                <EnvelopeClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="register-password-field" className="field-label">
              Şifre
            </Text>
            <TextField.Root
              id="register-password-field"
              name="password"
              type="password"
              size="3"
              variant="classic"
              placeholder="********"
              required
              className="login-field"
            >
              <TextField.Slot>
                <LockClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="password-repeat-field" className="field-label">
              Şifre Tekrarı
            </Text>
            <TextField.Root
              id="password-repeat-field"
              name="passwordRepeat"
              type="password"
              size="3"
              variant="classic"
              placeholder="********"
              required
              className="login-field"
            >
              <TextField.Slot>
                <LockClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Flex className="actions-row">
            <Button type="submit" size="3" className="login-button">
              Kayıt Ol
            </Button>
            <Link href="#" size="2" className="forgot-link" onClick={openLoginView}>
              Giriş Ekranına Dön
            </Link>
          </Flex>

          <Text as="p" size="2" className="signup-text">
            Zaten hesabınız var mı?{" "}
            <Link href="#" className="signup-link" onClick={openLoginView}>
              Giriş Yap
            </Link>
          </Text>
        </form>
      ) : (
        <form className="login-form" onSubmit={(event) => event.preventDefault()}>
          <Box className="field-group">
            <Text as="label" htmlFor="email-field" className="field-label">
              E-posta Adresiniz
            </Text>
            <TextField.Root
              id="email-field"
              name="email"
              type="email"
              size="3"
              variant="classic"
              placeholder="ornek@email.com"
              required
              className="login-field"
            >
              <TextField.Slot>
                <EnvelopeClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="password-field" className="field-label">
              Şifre
            </Text>
            <TextField.Root
              id="password-field"
              name="password"
              type="password"
              size="3"
              variant="classic"
              placeholder="********"
              required
              className="login-field"
            >
              <TextField.Slot>
                <LockClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Flex className="actions-row">
            <Button type="submit" size="3" className="login-button">
              Giriş Yap
            </Button>
            <Link href="#" size="2" className="forgot-link">
              Şifremi Unuttum?
            </Link>
          </Flex>

          <Text as="p" size="2" className="signup-text">
            Hesabınız yok mu?{" "}
            <Link href="#" className="signup-link" onClick={openRegisterView}>
              Kayıt Ol
            </Link>
          </Text>
        </form>
      )}
    </Box>
  );
}
