import { Box, Skeleton } from "@chakra-ui/react";
import { useColorModeValue } from "../../ui/color-mode";

export default function WidgetSkeleton() {
  const borderColor = useColorModeValue("#e4e4e7", "#262626");
  return (
    <Box
      h="100%"
      border="1px solid"
      borderColor={borderColor}
      borderRadius="lg"
      overflow="hidden"
    >
      <Skeleton h="100%" />
    </Box>
  );
}
