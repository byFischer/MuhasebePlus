import { Box, Flex, Text, IconButton } from "@chakra-ui/react";
import { GripVertical, X, RefreshCw } from "lucide-react";
import { useColorModeValue } from "../../ui/color-mode";

export default function WidgetFrame({ title, children, isEditMode, onRemove, onRefresh }) {
  const cardBg = useColorModeValue("#ffffff", "#111111");
  const borderColor = useColorModeValue("#e4e4e7", "#262626");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  return (
    <Box
      bg={cardBg}
      border="1px solid"
      borderColor={isEditMode ? "#2f6df6" : borderColor}
      borderRadius="lg"
      overflow="hidden"
      display="flex"
      flexDirection="column"
      transition="border-color 0.15s ease, box-shadow 0.15s ease"
      boxShadow={isEditMode ? "0 0 0 1px #2f6df633" : "none"}
      position="relative"
      cursor={isEditMode ? "grab" : "default"}
      userSelect={isEditMode ? "none" : "auto"}
    >
      {/* Header */}
      <Flex
        px="4"
        py="2.5"
        align="center"
        justify="space-between"
        borderBottom="1px solid"
        borderColor={borderColor}
        flexShrink={0}
      >
        <Flex align="center" gap="2">
          {isEditMode && <GripVertical size={14} color={textMuted} />}
          <Text fontSize="xs" fontWeight="600" color={textMuted} textTransform="uppercase" letterSpacing="0.05em">
            {title}
          </Text>
        </Flex>
        {isEditMode && (
          <Flex gap="1" className="no-drag">
            {onRefresh && (
              <IconButton
                size="xs"
                variant="ghost"
                aria-label="Yenile"
                color={textMuted}
                onClick={onRefresh}
                _hover={{ bg: "transparent", color: "#f5f7fb" }}
              >
                <RefreshCw size={12} />
              </IconButton>
            )}
            {onRemove && (
              <IconButton
                size="xs"
                variant="ghost"
                aria-label="Kaldır"
                color="#f87171"
                onClick={onRemove}
                _hover={{ bg: "transparent", color: "#ef4444" }}
              >
                <X size={12} />
              </IconButton>
            )}
          </Flex>
        )}
      </Flex>

      {/* Content */}
      <Box overflow="visible" p="0">
        {children}
      </Box>
    </Box>
  );
}
