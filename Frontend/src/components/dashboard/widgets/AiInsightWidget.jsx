import { Box, Text } from "@chakra-ui/react";
import { Sparkles } from "lucide-react";
import { useColorModeValue } from "../../ui/color-mode";

export default function AiInsightWidget({ widgetType }) {
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  return (
    <Box p="4" minH="100px" display="flex" flexDirection="column" alignItems="center" justifyContent="center" gap="3">
      <Sparkles size={28} color="#2f6df6" />
      <Text fontSize="sm" color={textMuted} textAlign="center">
        AI özelliği yakında aktif edilecek.
        {widgetType === "CONVERSATIONAL" && " Finansal verileriniz hakkında sorular sorabileceksiniz."}
        {widgetType === "DAILY_BRIEF" && " Her gün günlük finansal özet burada görünecek."}
      </Text>
    </Box>
  );
}
