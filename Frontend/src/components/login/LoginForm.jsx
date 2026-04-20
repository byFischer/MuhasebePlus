import {
  EnvelopeClosedIcon,
  LockClosedIcon,
  MobileIcon,
  PersonIcon,
} from "@radix-ui/react-icons";
import { useState } from "react";
import { Box, Button, Flex, Heading, Link, Text, TextField } from "@radix-ui/themes";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";


export default function LoginForm() {
  const [isRegisterView, setIsRegisterView] = useState(false);
  const [loginData, setLoginData] = useState({
    email: "",
    password: "",
  });
  const [registerData, setRegisterData] = useState({
    firstName: "",
    lastName: "",
    phoneNumber: "",
    email: "",
    password: "",
    passwordRepeat: "",
    birthDate:""
  });
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const navigate = useNavigate();
  const { loginUser, registerUser} = useAuth();

  const openRegisterView = (event) => {
    event.preventDefault();
    setErrorMessage("");
    setIsRegisterView(true);
  };

  const openLoginView = (event) => {
    event.preventDefault();
    setErrorMessage("");
    setIsRegisterView(false);
  };

  const handleLoginSubmit = async (event) => {
  event.preventDefault();

  setErrorMessage("");
  setIsSubmitting(true);

  try {
    await loginUser({
      email: loginData.email,
      password: loginData.password,
    });

    navigate("/dashboard");
  } catch (error) {
    setErrorMessage(error.message || "Giriş başarısız");
  } finally {
    setIsSubmitting(false);
  }
};

  const handleRegisterSubmit = async (event) => {
  event.preventDefault();

  if (registerData.password !== registerData.passwordRepeat) {
    setErrorMessage("Şifreler eşleşmiyor");
    return;
  }

  setErrorMessage("");
  setIsSubmitting(true);

  try {
    await registerUser({
      firstName: registerData.firstName,
      lastName: registerData.lastName,
      email: registerData.email,
      password: registerData.password,
      phoneNumber: registerData.phoneNumber,
      birthDate: registerData.birthDate,
    });

    navigate("/dashboard");
  } catch (error) {
    setErrorMessage(error.message || "Kayıt başarısız");
  } finally {
    setIsSubmitting(false);
  }
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
        <form className="login-form register-form" onSubmit={handleRegisterSubmit}>
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
              value={registerData.firstName}
              onChange={(e) =>
                setRegisterData({ ...registerData, firstName: e.target.value })
              }
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
              value={registerData.lastName}
              onChange={(e) =>
                setRegisterData({ ...registerData, lastName: e.target.value })
              }
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
              value={registerData.phoneNumber}
              onChange={(e) =>
                setRegisterData({ ...registerData, phoneNumber: e.target.value })
              }
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
              value={registerData.email}
              onChange={(e) =>
                setRegisterData({ ...registerData, email: e.target.value })
              }
            >
              <TextField.Slot>
                <EnvelopeClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          <Box className="field-group">
            <Text as="label" htmlFor="birthdate-field" className="field-label">
              Doğum Tarihi
            </Text>
            <TextField.Root
              id="birthdate-field"
              name="birthDate"
              type="date"
              size="3"
              variant="classic"
              required
              className="login-field"
              value={registerData.birthDate}
              onChange={(e) =>
                setRegisterData({ ...registerData, birthDate: e.target.value })
              }
            />
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
              value={registerData.password}
              onChange={(e) =>
                setRegisterData({ ...registerData, password: e.target.value })
              }
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
              value={registerData.passwordRepeat}
              onChange={(e) =>
                setRegisterData({ ...registerData, passwordRepeat: e.target.value })
              }
            >
              <TextField.Slot>
                <LockClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          {errorMessage && (
            <Text as="p" size="2" color="red" className="form-error">
              {errorMessage}
            </Text>
          )}

          <Flex className="actions-row">
            <Button
              type="submit"
              size="3"
              className="login-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Gönderiliyor..." : "Kayıt Ol"}
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
        <form className="login-form" onSubmit={handleLoginSubmit}>
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
              value = {loginData.email}
              onChange ={(e) =>
                setLoginData({...loginData, email: e.target.value})
              }
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
              value = {loginData.password}
              onChange ={(e) =>
                setLoginData({...loginData, password: e.target.value})
              }
            >
              <TextField.Slot>
                <LockClosedIcon className="field-icon" />
              </TextField.Slot>
            </TextField.Root>
          </Box>

          {errorMessage && (
            <Text as="p" size="2" color="red" className="form-error">
              {errorMessage}
            </Text>
          )}

          <Flex className="actions-row">
            <Button
              type="submit"
              size="3"
              className="login-button"
              disabled={isSubmitting}
            >
              {isSubmitting ? "Gönderiliyor..." : "Giriş Yap"}
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
