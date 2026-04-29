import { VStack, Text, Button } from "@chakra-ui/react";
import { LayoutDashboard, Plus } from "lucide-react";
import { useColorModeValue } from "../../ui/color-mode";

export default function EmptyDashboard({ onAddWidget }) {
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  return (
    <VStack gap="4" py="16" justify="center" align="center">
      <LayoutDashboard size={40} color={textMuted} />
      <VStack gap="1">
        <Text fontSize="lg" fontWeight="600" color={useColorModeValue("#09090b", "#fafafa")}>
          Dashboard boş
        </Text>
        <Text fontSize="sm" color={textMuted} textAlign="center" maxW="300px">
          Widget kataloğundan ekleyerek dashboardunuzu kişiselleştirin.
        </Text>
      </VStack>
      <Button
        size="sm"
        bg="#2f6df6"
        color="white"
        _hover={{ bg: "#2760dc" }}
        onClick={onAddWidget}
      >
        <Plus size={14} />
        Widget Ekle
      </Button>
    </VStack>
  );
}
