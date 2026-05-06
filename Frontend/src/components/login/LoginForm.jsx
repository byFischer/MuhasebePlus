import { CheckIcon, EyeOpenIcon } from "@radix-ui/react-icons";
import { useState } from "react";
import {
  Box,
  Button,
  Checkbox,
  Field,
  Heading,
  HStack,
  IconButton,
  Input,
  InputGroup,
  Link,
  SimpleGrid,
  Span,
  Text,
  VStack,
} from "@chakra-ui/react";
import { Link as RouterLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const inputStyles = {
  h: "40px",
  borderRadius: "3px",
  borderColor: "#26282d",
  bg: "transparent",
  color: "#f5f7fb",
  fontSize: "14px",
  _hover: { borderColor: "#303540" },
  _focus: {
    borderColor: "#2f6df6",
    boxShadow: "0 0 0 1px #2f6df6",
  },
  _placeholder: { color: "#6f7785" },
};

import logoImage from "../../icons/image.png";

function Logo() {
  return (
    <Box w="180px" mx="auto">
      <img src={logoImage} alt="Muhasebe Plus Logo" style={{ width: "100%", height: "auto" }} />
    </Box>
  );
}

function GoogleMark() {
  return (
    <Span color="#8ab4f8" fontSize="22px" fontWeight="700" lineHeight="1">
      G
    </Span>
  );
}

function FormField({ id, label, children }) {
  return (
    <Field.Root gap="2">
      <Field.Label htmlFor={id} color="#f5f7fb" fontSize="13px" fontWeight="700">
        {label}
      </Field.Label>
      {children}
    </Field.Root>
  );
}

export default function LoginForm() {
  const [isRegisterView, setIsRegisterView] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
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
    birthDate: "",
    companyName: "",
    companyTaxNumber: "",
  });
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const navigate = useNavigate();
  const { loginUser, registerUser } = useAuth();

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
        companyName: registerData.companyName,
        companyTaxNumber: registerData.companyTaxNumber,
      });

      navigate("/dashboard");
    } catch (error) {
      setErrorMessage(error.message || "Kayıt başarısız");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isRegisterView) {
    return (
      <VStack as="form" gap="5" w="full" onSubmit={handleRegisterSubmit}>
        <VStack gap="3" textAlign="center">
          <Logo />
          <Heading color="#f8fafc" fontSize="30px" lineHeight="1.1" fontWeight="700">
            Create account
          </Heading>
          <Text color="#9ca3af" fontSize="16px">
            Muhasebe+'ya üye olun ve finansal kontrolü elinize alın
          </Text>
        </VStack>

        <SimpleGrid columns={{ base: 1, md: 2 }} gap="4" w="full">
          <FormField id="first-name-field" label="First name">
            <Input
              id="first-name-field"
              name="firstName"
              required
              value={registerData.firstName}
              onChange={(e) =>
                setRegisterData({ ...registerData, firstName: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>

          <FormField id="last-name-field" label="Last name">
            <Input
              id="last-name-field"
              name="lastName"
              required
              value={registerData.lastName}
              onChange={(e) =>
                setRegisterData({ ...registerData, lastName: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>
        </SimpleGrid>

        <FormField id="register-email-field" label="Email">
          <Input
            id="register-email-field"
            name="email"
            type="email"
            required
            value={registerData.email}
            onChange={(e) => setRegisterData({ ...registerData, email: e.target.value })}
            {...inputStyles}
          />
        </FormField>

        <SimpleGrid columns={{ base: 1, md: 2 }} gap="4" w="full">
          <FormField id="phone-field" label="Phone">
            <Input
              id="phone-field"
              name="phoneNumber"
              type="tel"
              required
              value={registerData.phoneNumber}
              onChange={(e) =>
                setRegisterData({ ...registerData, phoneNumber: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>

          <FormField id="birthdate-field" label="Birth date">
            <Input
              id="birthdate-field"
              name="birthDate"
              type="date"
              required
              value={registerData.birthDate}
              onChange={(e) =>
                setRegisterData({ ...registerData, birthDate: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>
        </SimpleGrid>

        <SimpleGrid columns={{ base: 1, md: 2 }} gap="4" w="full">
          <FormField id="company-name-field" label="Company">
            <Input
              id="company-name-field"
              name="companyName"
              required
              value={registerData.companyName}
              onChange={(e) =>
                setRegisterData({ ...registerData, companyName: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>

          <FormField id="company-tax-field" label="Tax number">
            <Input
              id="company-tax-field"
              name="companyTaxNumber"
              required
              value={registerData.companyTaxNumber}
              onChange={(e) =>
                setRegisterData({
                  ...registerData,
                  companyTaxNumber: e.target.value,
                })
              }
              {...inputStyles}
            />
          </FormField>
        </SimpleGrid>

        <SimpleGrid columns={{ base: 1, md: 2 }} gap="4" w="full">
          <FormField id="register-password-field" label="Password">
            <Input
              id="register-password-field"
              name="password"
              type="password"
              required
              value={registerData.password}
              onChange={(e) =>
                setRegisterData({ ...registerData, password: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>

          <FormField id="password-repeat-field" label="Repeat password">
            <Input
              id="password-repeat-field"
              name="passwordRepeat"
              type="password"
              required
              value={registerData.passwordRepeat}
              onChange={(e) =>
                setRegisterData({ ...registerData, passwordRepeat: e.target.value })
              }
              {...inputStyles}
            />
          </FormField>
        </SimpleGrid>

        {errorMessage && (
          <Text color="#f87171" fontSize="13px" alignSelf="flex-start">
            {errorMessage}
          </Text>
        )}

        <Button
          type="submit"
          w="full"
          h="40px"
          borderRadius="4px"
          bg="#2f6df6"
          color="white"
          fontWeight="700"
          loading={isSubmitting}
          _hover={{ bg: "#2760dc" }}
        >
          Sign up
        </Button>

        <Text color="#9ca3af" fontSize="14px">
          Already have an account?{" "}
          <Link href="#" color="#7bb2ff" fontWeight="700" onClick={openLoginView}>
            Sign in
          </Link>
        </Text>
      </VStack>
    );
  }

  return (
    <VStack as="form" gap="6" w="full" onSubmit={handleLoginSubmit}>
      <VStack gap="4" textAlign="center">
        <Logo />
        <VStack gap="2">
          <Heading color="#f8fafc" fontSize="30px" lineHeight="1.1" fontWeight="700">
            Hoş geldiniz
          </Heading>
          <Text color="#9ca3af" fontSize="16px">
            Muhasebe+ ile finansal yönetiminizi kolaylaştırın
          </Text>
        </VStack>
      </VStack>

      <VStack gap="5" w="full">
        <FormField id="email-field" label="Email">
          <Input
            id="email-field"
            name="email"
            type="email"
            required
            value={loginData.email}
            onChange={(e) => setLoginData({ ...loginData, email: e.target.value })}
            {...inputStyles}
          />
        </FormField>

        <FormField id="password-field" label="Password">
          <InputGroup
            endElement={
              <IconButton
                aria-label={showPassword ? "Hide password" : "Show password"}
                size="xs"
                variant="ghost"
                color="#88a0c5"
                minW="auto"
                h="24px"
                onClick={() => setShowPassword((current) => !current)}
                _hover={{ bg: "transparent", color: "#b5c9ef" }}
              >
                <EyeOpenIcon />
              </IconButton>
            }
            endElementProps={{ pe: "3" }}
          >
            <Input
              id="password-field"
              name="password"
              type={showPassword ? "text" : "password"}
              required
              value={loginData.password}
              onChange={(e) =>
                setLoginData({ ...loginData, password: e.target.value })
              }
              {...inputStyles}
            />
          </InputGroup>
        </FormField>
      </VStack>

      <HStack justify="space-between" w="full">
        <Checkbox.Root defaultChecked>
          <Checkbox.HiddenInput />
          <Checkbox.Control
            boxSize="18px"
            borderRadius="3px"
            borderColor="#2f6df6"
            bg="#2f6df6"
          >
            <Checkbox.Indicator color="white">
              <CheckIcon />
            </Checkbox.Indicator>
          </Checkbox.Control>
          <Checkbox.Label color="#f5f7fb" fontSize="14px" fontWeight="700">
            Remember me
          </Checkbox.Label>
        </Checkbox.Root>

        <Link asChild color="#7bb2ff" fontSize="14px" fontWeight="700">
          <RouterLink to="/forgot-password">Forgot password</RouterLink>
        </Link>
      </HStack>

      {errorMessage && (
        <Text color="#f87171" fontSize="13px" alignSelf="flex-start">
          {errorMessage}
        </Text>
      )}

      <VStack gap="4" w="full">
        <Button
          type="submit"
          w="full"
          h="40px"
          borderRadius="4px"
          bg="#2f6df6"
          color="white"
          fontWeight="700"
          loading={isSubmitting}
          _hover={{ bg: "#2760dc" }}
        >
          Sign in
        </Button>

        <Button
          type="button"
          w="full"
          h="40px"
          borderRadius="3px"
          border="1px solid"
          borderColor="#1f4aa3"
          bg="transparent"
          color="#f5f7fb"
          fontWeight="700"
          _hover={{ bg: "rgba(47, 109, 246, 0.08)" }}
        >
          <HStack gap="2">
            <GoogleMark />
            <Span>Sign in with Google</Span>
          </HStack>
        </Button>
      </VStack>

      <Text color="#9ca3af" fontSize="14px" pt="3">
        Don&apos;t have an account?{" "}
        <Link href="#" color="#7bb2ff" fontWeight="700" onClick={openRegisterView}>
          Sign up
        </Link>
      </Text>
    </VStack>
  );
}
