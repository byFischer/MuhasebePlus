import { Box, Flex, Text, VStack, Icon } from "@chakra-ui/react"
import {
  LayoutDashboard,
  Receipt,
  ArrowLeftRight,
  FileText,
  PieChart,
  Settings,
  Wallet,
  Users,
  Copy,
  Package,
  Bell,
  ScrollText,
} from "lucide-react"
import { useColorModeValue } from "@/components/ui/color-mode"

const navItems = [
  { icon: LayoutDashboard, label: "Gösterge Paneli", active: true },
  { icon: Users,           label: "Müşteriler",       active: false },
  { icon: Receipt,         label: "Faturalar",         active: false },
  { icon: ArrowLeftRight,  label: "İşlemler",          active: false },
  { icon: Copy,            label: "İşlem Şablonları",  active: false },
  { icon: Wallet,          label: "Hesaplar",          active: false },
  { icon: Package,         label: "Stok",              active: false },
  { icon: PieChart,        label: "Raporlar",          active: false },
  { icon: Bell,            label: "Bildirimler",       active: false },
  { icon: FileText,        label: "Beyannameler",      active: false },
  { icon: ScrollText,      label: "Sistem Logları",    active: false },
]

export function Sidebar() {
  const bg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const activeBg = useColorModeValue("#f4f4f5", "#1a1a1a")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const hoverBg = useColorModeValue("#f4f4f5", "#1a1a1a")

  return (
    <Box
      as="aside"
      width="240px"
      minW="240px"
      height="100dvh"
      bg={bg}
      borderRight="1px solid"
      borderColor={borderColor}
      display="flex"
      flexDirection="column"
      overflow="hidden"
    >
      {/* Logo */}
      <Flex
        align="center"
        gap="3"
        px="5"
        py="5"
        borderBottom="1px solid"
        borderColor={borderColor}
      >
        <Box>
          <svg
            width="28"
            height="28"
            viewBox="0 0 28 28"
            fill="none"
            aria-label="Ledger logo"
          >
            <rect
              x="2"
              y="2"
              width="24"
              height="24"
              rx="4"
              stroke="currentColor"
              strokeWidth="2"
            />
            <path
              d="M8 9h12M8 14h8M8 19h10"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
            />
          </svg>
        </Box>
        <Text
          fontSize="md"
          fontWeight="700"
          letterSpacing="-0.02em"
          color={textPrimary}
        >
          Ledger
        </Text>
      </Flex>

      {/* Navigation */}
      <VStack gap="1" px="3" py="4" flex="1" align="stretch">
        {navItems.map((item) => (
          <Flex
            key={item.label}
            align="center"
            gap="3"
            px="3"
            py="2"
            borderRadius="md"
            cursor="pointer"
            bg={item.active ? activeBg : "transparent"}
            color={item.active ? textPrimary : textMuted}
            fontWeight={item.active ? "600" : "400"}
            fontSize="sm"
            transition="all 0.15s ease"
            _hover={{
              bg: hoverBg,
              color: textPrimary,
            }}
          >
            <Icon asChild boxSize="4" flexShrink={0}>
              <item.icon />
            </Icon>
            <Text>{item.label}</Text>
          </Flex>
        ))}
      </VStack>

      {/* Settings at bottom */}
      <Box px="3" pb="4" borderTop="1px solid" borderColor={borderColor} pt="3">
        <Flex
          align="center"
          gap="3"
          px="3"
          py="2"
          borderRadius="md"
          cursor="pointer"
          color={textMuted}
          fontSize="sm"
          transition="all 0.15s ease"
          _hover={{
            bg: hoverBg,
            color: textPrimary,
          }}
        >
          <Icon asChild boxSize="4" flexShrink={0}>
            <Settings />
          </Icon>
          <Text>Ayarlar</Text>
        </Flex>
      </Box>
    </Box>
  )
}
