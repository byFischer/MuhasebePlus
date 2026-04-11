import { Theme } from "@radix-ui/themes";
import DashboardPage from "../pages/DashboardPage";

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
      <DashboardPage />
    </Theme>
  );
}
