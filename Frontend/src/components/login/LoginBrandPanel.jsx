import { Box } from "@radix-ui/themes";

export default function LoginBrandPanel() {
  return (
    <Box className="right-panel">
      <Box className="brand-mark" aria-hidden>
        <span className="brand-mark-m">M</span>
        <span className="brand-mark-plus">+</span>
      </Box>
    </Box>
  );
}
