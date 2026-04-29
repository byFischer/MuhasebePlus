import { Flex, Button, Text, Badge, Spinner } from "@chakra-ui/react";
import { Pencil, Check, X, RotateCcw, Plus, Sparkles } from "lucide-react";
import { useEditMode } from "../../../hooks/useEditMode";
import { useColorModeValue } from "../../ui/color-mode";

export default function DashboardToolbar({ layoutName, onSave, onReset, onAddWidget, onAiStudio, isSaving }) {
  const { isEditMode, isDirty, enterEdit, exitEdit } = useEditMode();
  const borderColor = useColorModeValue("#e4e4e7", "#262626");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  const handleSave = async () => {
    await onSave?.();
    exitEdit();
  };

  const handleCancel = () => {
    onReset?.();
    exitEdit();
  };

  return (
    <Flex
      align="center"
      justify="space-between"
      px="4"
      py="2"
      borderBottom="1px solid"
      borderColor={borderColor}
      flexShrink={0}
      gap="3"
      flexWrap="wrap"
    >
      <Flex align="center" gap="2">
        <Text fontSize="sm" fontWeight="600" color={useColorModeValue("#09090b", "#fafafa")}>
          {layoutName ?? "Ana Dashboard"}
        </Text>
        {isEditMode && (
          <Badge colorScheme="blue" fontSize="10px" px="2" py="0.5" borderRadius="full">
            Düzenleme Modu
          </Badge>
        )}
      </Flex>

      <Flex gap="2" align="center">
        {!isEditMode ? (
          <>
            <Button
              size="xs"
              variant="outline"
              borderColor={borderColor}
              color={textMuted}
              _hover={{ borderColor: "#2f6df6", color: "#2f6df6" }}
              onClick={enterEdit}
              gap="1"
            >
              <Pencil size={12} />
              Düzenle
            </Button>
            <Button
              size="xs"
              variant="outline"
              borderColor={borderColor}
              color={textMuted}
              _hover={{ borderColor: "#2f6df6", color: "#2f6df6" }}
              onClick={onAddWidget}
              gap="1"
            >
              <Plus size={12} />
              Widget Ekle
            </Button>
            <Button
              size="xs"
              bg="#2f6df611"
              color="#2f6df6"
              border="1px solid"
              borderColor="#2f6df633"
              _hover={{ bg: "#2f6df622" }}
              onClick={onAiStudio}
              gap="1"
            >
              <Sparkles size={12} />
              AI Studio
            </Button>
          </>
        ) : (
          <>
            <Button
              size="xs"
              variant="ghost"
              color={textMuted}
              onClick={handleCancel}
              gap="1"
            >
              <X size={12} />
              Vazgeç
            </Button>
            <Button
              size="xs"
              bg="#2f6df6"
              color="white"
              _hover={{ bg: "#2760dc" }}
              onClick={handleSave}
              loading={isSaving}
              disabled={!isDirty}
              gap="1"
            >
              {isSaving ? <Spinner size="xs" /> : <Check size={12} />}
              Kaydet
            </Button>
          </>
        )}
      </Flex>
    </Flex>
  );
}
