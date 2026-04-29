import { SimpleGrid, Box, Text, VStack } from "@chakra-ui/react";
import { useNavigate } from "react-router-dom";
import { FileText, Users, ArrowLeftRight, Package } from "lucide-react";
import { useColorModeValue } from "../../ui/color-mode";

const ACTIONS = [
  { label: "Yeni Fatura", icon: FileText, path: "/dashboard/invoices/new", color: "#2f6df6" },
  { label: "Müşteri Ekle", icon: Users, path: "/dashboard/customers/new", color: "#10b981" },
  { label: "İşlem Gir", icon: ArrowLeftRight, path: "/dashboard/transactions/new", color: "#f59e0b" },
  { label: "Ürün Ekle", icon: Package, path: "/dashboard/products/new", color: "#8b5cf6" },
];

export default function QuickActionsWidget() {
  const navigate = useNavigate();
  const cardBg = useColorModeValue("#f4f4f5", "#1a1a1a");
  const textPrimary = useColorModeValue("#09090b", "#fafafa");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  return (
    <SimpleGrid columns={2} gap="3" p="4">
      {ACTIONS.map((action) => (
        <Box
          key={action.label}
          bg={cardBg}
          borderRadius="lg"
          p="3"
          cursor="pointer"
          onClick={() => navigate(action.path)}
          _hover={{ opacity: 0.8 }}
          transition="opacity 0.15s"
        >
          <VStack gap="2" align="center">
            <Box p="2" borderRadius="md" bg={action.color + "22"}>
              <action.icon size={18} color={action.color} />
            </Box>
            <Text fontSize="xs" fontWeight="600" color={textPrimary} textAlign="center">
              {action.label}
            </Text>
          </VStack>
        </Box>
      ))}
    </SimpleGrid>
  );
}
