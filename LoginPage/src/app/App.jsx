import { Theme } from "@radix-ui/themes";
import LoginPage from "../pages/LoginPage";

export default function App() {
  return (
    <Theme
      appearance="dark"
      accentColor="crimson"
      grayColor="gray"
      radius="large"
      panelBackground="solid"
      hasBackground={false}
    >
      <LoginPage />
    </Theme>
  );
}
