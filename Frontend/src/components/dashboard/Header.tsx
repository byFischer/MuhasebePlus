import { Flex, Text, Box, Input } from "@chakra-ui/react"
import { Search, Bell } from "lucide-react"
import { ColorModeButton } from "@/components/ui/color-mode"
import { useColorModeValue } from "@/components/ui/color-mode"

export function Header() {
  const bg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const inputBg = useColorModeValue("#f4f4f5", "#1a1a1a")
  const iconColor = useColorModeValue("#a1a1aa", "#71717a")

  return (
    <Flex
      as="header"
      align="center"
      justify="space-between"
      px="6"
      py="3"
      bg={bg}
      borderBottom="1px solid"
      borderColor={borderColor}
      minH="56px"
      flexShrink={0}
    >
      <Box>
        <Text fontSize="sm" fontWeight="600" color={textPrimary}>
          Gösterge Paneli
        </Text>
        <Text fontSize="xs" color={textMuted}>
          30 Mart 2026, Pazartesi
        </Text>
      </Box>

      <Flex align="center" gap="3">
        {/* Search */}
        <Flex
          align="center"
          gap="2"
          bg={inputBg}
          px="3"
          py="1.5"
          borderRadius="md"
          width="220px"
        >
          <Search size={14} color={iconColor} />
          <Input
            placeholder="Ara..."
            fontSize="sm"
            color={textPrimary}
            _placeholder={{ color: textMuted }}
            height="auto"
            p="0"
            border="none"
            outline="none"
            bg="transparent"
            _focus={{ outline: "none", boxShadow: "none" }}
          />
        </Flex>

        {/* Notification */}
        <Flex
          align="center"
          justify="center"
          width="36px"
          height="36px"
          borderRadius="md"
          cursor="pointer"
          position="relative"
          transition="all 0.15s ease"
          _hover={{ bg: inputBg }}
        >
          <Bell size={16} color={iconColor} />
          <Box
            position="absolute"
            top="8px"
            right="8px"
            width="6px"
            height="6px"
            borderRadius="full"
            bg="#dc2626"
          />
        </Flex>

        {/* Theme Toggle */}
        <ColorModeButton
          borderRadius="md"
          size="sm"
          color={iconColor}
          _hover={{ bg: inputBg }}
        />

        {/* Avatar */}
        <Flex
          align="center"
          justify="center"
          width="32px"
          height="32px"
          borderRadius="full"
          bg={useColorModeValue("#09090b", "#fafafa")}
          color={useColorModeValue("#fafafa", "#09090b")}
          fontSize="xs"
          fontWeight="600"
          cursor="pointer"
          flexShrink={0}
        >
          EZ
        </Flex>
      </Flex>
    </Flex>
  )
}
