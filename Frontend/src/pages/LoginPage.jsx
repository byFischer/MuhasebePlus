import { Box, Center } from "@chakra-ui/react";
import LoginForm from "../components/login/LoginForm";
import { Particles } from "@/components/ui/particles";

export default function LoginPage() {
  return (
    <Box as="main" position="relative" minH="100vh" bg="#07080b" color="white" overflow="hidden">
      <Box position="absolute" inset="0" zIndex="0">
        <Particles
          quantity={250}
          staticity={40}
          ease={40}
          size={0.5}
          color="#ffffff"
          style={{ width: "100%", height: "100%" }}
        />
      </Box>

      <Center
        position="relative"
        zIndex="10"
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
