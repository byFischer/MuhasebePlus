import { Box, Center } from "@chakra-ui/react";
import LoginForm from "../components/login/LoginForm";

export default function LoginPage() {
  return (
    <Box as="main" minH="100vh" bg="#07080b" color="white">
      <Center
        minH="100vh"
        px={{ base: "5", md: "8" }}
        pt={{ base: "12", md: "20" }}
        pb="12"
        alignItems="flex-start"
      >
        <Box w="full" maxW="384px">
          <LoginForm />
        </Box>
      </Center>
    </Box>
  );
}
