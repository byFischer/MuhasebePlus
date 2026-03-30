import { EnvelopeClosedIcon, LockClosedIcon } from "@radix-ui/react-icons";
import { Box, Button, Flex, Heading, Link, Text, TextField } from "@radix-ui/themes";

export default function LoginForm() {
  return (
    <Box className="left-panel">
      <Box className="brand-chip" aria-hidden />

      <Box className="intro-copy">
        <Heading as="h1" className="welcome-title">
          Welcome
        </Heading>
        <Text as="p" className="welcome-text">
          Bring your own entertain at one place
        </Text>
      </Box>

      <form className="login-form" onSubmit={(event) => event.preventDefault()}>
        <Box className="field-group">
          <Text as="label" htmlFor="email-field" className="field-label">
            Your Email
          </Text>
          <TextField.Root
            id="email-field"
            name="email"
            type="email"
            size="3"
            variant="classic"
            placeholder="you@example.com"
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
            Password
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
            Login
          </Button>
          <Link href="#" size="2" className="forgot-link">
            Forgot Password?
          </Link>
        </Flex>

        <Text as="p" size="2" className="signup-text">
          Don&apos;t have an account?{" "}
          <Link href="#" className="signup-link">
            Sign Up
          </Link>
        </Text>
      </form>
    </Box>
  );
}
