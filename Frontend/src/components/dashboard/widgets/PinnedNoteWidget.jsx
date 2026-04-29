import { useState } from "react";
import { Box, Textarea, Text } from "@chakra-ui/react";
import { useColorModeValue } from "../../ui/color-mode";

const STORAGE_KEY = "dashboard_pinned_note";

export default function PinnedNoteWidget() {
  const [note, setNote] = useState(() => localStorage.getItem(STORAGE_KEY) ?? "");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  const handleChange = (e) => {
    setNote(e.target.value);
    localStorage.setItem(STORAGE_KEY, e.target.value);
  };

  return (
    <Box p="3" h="100%" display="flex" flexDirection="column" gap="2">
      <Text fontSize="xs" color={textMuted}>Buraya not yapıştır...</Text>
      <Textarea
        value={note}
        onChange={handleChange}
        placeholder="Notlarınızı buraya yazın..."
        border="none"
        outline="none"
        resize="none"
        flex="1"
        fontSize="sm"
        color={useColorModeValue("#09090b", "#fafafa")}
        bg="transparent"
        p="0"
        _focus={{ boxShadow: "none" }}
        _placeholder={{ color: textMuted }}
      />
    </Box>
  );
}
