import { ArrowLeftIcon, EnvelopeClosedIcon } from "@radix-ui/react-icons";
import { useState } from "react";
import {
  Box,
  Button,
  Center,
  Field,
  Heading,
  HStack,
  Input,
  Link,
  Text,
  VStack,
} from "@chakra-ui/react";
import { Link as RouterLink } from "react-router-dom";
import authService from "../services/authService";

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

export default function PasswordResetPage() {
  const [email, setEmail] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");
    setIsSubmitting(true);

    try {
      const response = await authService.sendPasswordResetTestMail(email);
      setSuccessMessage(response?.message || "Test email sent.");
    } catch (error) {
      setErrorMessage(error.message || "Test email could not be sent.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Box as="main" minH="100vh" bg="#07080b" color="white">
      <Center
        minH="100vh"
        px={{ base: "5", md: "8" }}
        pt={{ base: "12", md: "20" }}
        pb="12"
        alignItems="flex-start"
      >
        <VStack as="form" gap="6" w="full" maxW="384px" onSubmit={handleSubmit}>
          <VStack gap="4" textAlign="center">
            <HStack gap="2" justify="center">
              <Box
                boxSize="26px"
                borderRadius="full"
                bg="#2563eb"
                display="grid"
                placeItems="center"
              >
                <EnvelopeClosedIcon />
              </Box>
              <Text color="#f1f5f9" fontWeight="700" fontSize="16px">
                Muhasebe+
              </Text>
            </HStack>
            <VStack gap="2">
              <Heading color="#f8fafc" fontSize="30px" lineHeight="1.1" fontWeight="700">
                Reset password
              </Heading>
              <Text color="#9ca3af" fontSize="16px">
                Enter your email address to receive a JavaMailSender test email.
              </Text>
            </VStack>
          </VStack>

          <Field.Root gap="2">
            <Field.Label htmlFor="reset-email-field" color="#f5f7fb" fontSize="13px" fontWeight="700">
              Email
            </Field.Label>
            <Input
              id="reset-email-field"
              name="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              {...inputStyles}
            />
          </Field.Root>

          {errorMessage && (
            <Text color="#f87171" fontSize="13px" alignSelf="flex-start">
              {errorMessage}
            </Text>
          )}

          {successMessage && (
            <Text color="#4ade80" fontSize="13px" alignSelf="flex-start">
              {successMessage}
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
            Send test email
          </Button>

          <Link asChild color="#7bb2ff" fontSize="14px" fontWeight="700">
            <RouterLink to="/login">
              <HStack gap="2">
                <ArrowLeftIcon />
                <Text>Back to sign in</Text>
              </HStack>
            </RouterLink>
          </Link>
        </VStack>
      </Center>
    </Box>
  );
}
