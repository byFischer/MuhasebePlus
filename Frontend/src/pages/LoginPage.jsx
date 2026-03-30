import { Card, Flex } from "@radix-ui/themes";
import LoginBrandPanel from "../components/login/LoginBrandPanel";
import LoginForm from "../components/login/LoginForm";

export default function LoginPage() {
  return (
    <main className="page-shell">
      <Card className="login-frame" size="1">
        <Flex className="login-layout">
          <LoginForm />
          <LoginBrandPanel />
        </Flex>
      </Card>
    </main>
  );
}
