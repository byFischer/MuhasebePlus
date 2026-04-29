import { useState } from "react";
import {
  Dialog, Box, Flex, Text, Textarea, Button, VStack, SimpleGrid, Badge, Spinner,
} from "@chakra-ui/react";
import { Sparkles, X, Plus, RefreshCw } from "lucide-react";
import { useColorModeValue } from "../../ui/color-mode";
import { dashboardService } from "../../../services/dashboardService";
import { useAddWidget } from "../../../hooks/useDashboardLayout";

const EXAMPLE_PROMPTS = [
  "Bu ay en çok harcadığım 5 kategoriyi pasta grafik yap",
  "Son 6 ayın aylık net kârını alan grafik göster",
  "Vadesi geçmiş faturaları olan müşterileri liste yap",
  "Bu ay gelir ve gider karşılaştırması",
  "En çok ciro yapan ilk 10 müşteri",
  "Son 30 günde olağandışı büyük işlemler",
];

export default function AiWidgetStudio({ open, onClose, layoutId, nextSlotIndex = 0, pageIndex = 0, onAdded }) {
  const [prompt, setPrompt] = useState("");
  const [generating, setGenerating] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const borderColor = useColorModeValue("#e4e4e7", "#262626");
  const textPrimary = useColorModeValue("#09090b", "#fafafa");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");
  const bg = useColorModeValue("#ffffff", "#111111");
  const codeBg = useColorModeValue("#f4f4f5", "#1a1a1a");

  const { mutateAsync: addWidget, isPending: isAdding } = useAddWidget();

  const handleGenerate = async () => {
    if (!prompt.trim()) return;
    setGenerating(true);
    setResult(null);
    setError(null);
    try {
      const data = await dashboardService.generateAiWidget(prompt);
      setResult(data);
    } catch (e) {
      setError(e?.response?.data?.error ?? e?.message ?? "AI servisi şu anda kullanılamıyor. Gemini API anahtarını kontrol edin.");
    } finally {
      setGenerating(false);
    }
  };

  const handleAddToDashboard = async () => {
    if (!result || !layoutId) return;
    const config = result.config ?? result;
    await addWidget({
      layoutId,
      dto: {
        widgetType: config.type ?? "TIME_SERIES",
        title: config.title ?? "AI Widget",
        positionX: nextSlotIndex,
        positionY: pageIndex,
        width: 8,
        height: 4,
        config: JSON.stringify(config),
      },
    });
    onAdded?.(pageIndex);
    onClose();
    setResult(null);
    setPrompt("");
  };

  const handleClose = () => {
    onClose();
    setResult(null);
    setError(null);
    setPrompt("");
  };

  return (
    <Dialog.Root open={open} onOpenChange={(e) => !e.open && handleClose()} size="xl">
      <Dialog.Backdrop />
      <Dialog.Positioner>
        <Dialog.Content bg={bg} border="1px solid" borderColor={borderColor} borderRadius="xl" maxW="700px" w="full">
          <Dialog.Header borderBottom="1px solid" borderColor={borderColor} pb="3">
            <Flex align="center" justify="space-between">
              <Flex align="center" gap="2">
                <Sparkles size={18} color="#8b5cf6" />
                <Dialog.Title fontSize="sm" fontWeight="700" color={textPrimary}>
                  AI Widget Studio
                </Dialog.Title>
              </Flex>
              <Dialog.CloseTrigger asChild>
                <Button size="xs" variant="ghost" color={textMuted} onClick={handleClose}>
                  <X size={14} />
                </Button>
              </Dialog.CloseTrigger>
            </Flex>
          </Dialog.Header>

          <Dialog.Body p="4">
            <VStack gap="4" align="stretch">
              <Box>
                <Text fontSize="xs" fontWeight="600" color={textMuted} mb="2" textTransform="uppercase" letterSpacing="0.05em">
                  Ne tür bir widget oluşturmak istiyorsunuz?
                </Text>
                <Textarea
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  placeholder="Örnek: Son 3 ayda en çok satan 10 ürünü tablo olarak göster..."
                  rows={3}
                  borderColor={borderColor}
                  _focus={{ borderColor: "#8b5cf6", boxShadow: "0 0 0 1px #8b5cf6" }}
                  fontSize="sm"
                />
              </Box>

              <Box>
                <Text fontSize="xs" fontWeight="600" color={textMuted} mb="2">
                  Örnek sorgular
                </Text>
                <SimpleGrid columns={{ base: 1, md: 2 }} gap="2">
                  {EXAMPLE_PROMPTS.map((p) => (
                    <Box
                      key={p}
                      px="3"
                      py="2"
                      bg={codeBg}
                      borderRadius="md"
                      cursor="pointer"
                      onClick={() => setPrompt(p)}
                      _hover={{ opacity: 0.8 }}
                      transition="opacity 0.15s"
                    >
                      <Text fontSize="xs" color={textPrimary}>{p}</Text>
                    </Box>
                  ))}
                </SimpleGrid>
              </Box>

              <Button
                bg="#8b5cf6"
                color="white"
                _hover={{ bg: "#7c3aed" }}
                onClick={handleGenerate}
                loading={generating}
                disabled={!prompt.trim()}
              >
                {generating ? <Spinner size="xs" /> : <Sparkles size={14} />}
                {generating ? "Üretiliyor..." : "Widget Üret"}
              </Button>

              {error && (
                <Box p="3" bg="#f871711a" border="1px solid" borderColor="#f87171" borderRadius="lg">
                  <Text fontSize="sm" color="#f87171">{error}</Text>
                </Box>
              )}

              {result && (
                <Box>
                  <Flex align="center" justify="space-between" mb="2">
                    <Text fontSize="xs" fontWeight="600" color={textMuted} textTransform="uppercase">
                      Üretilen Widget Konfigürasyonu
                    </Text>
                    <Badge bg="#4ade8022" color="#4ade80" fontSize="10px" px="2" borderRadius="full">
                      Hazır
                    </Badge>
                  </Flex>
                  <Box
                    bg={codeBg}
                    borderRadius="lg"
                    p="3"
                    maxH="180px"
                    overflow="auto"
                    border="1px solid"
                    borderColor={borderColor}
                  >
                    <pre style={{ fontSize: "11px", color: textMuted, whiteSpace: "pre-wrap" }}>
                      {JSON.stringify(result, null, 2)}
                    </pre>
                  </Box>
                  <Flex gap="2" mt="3">
                    <Button
                      flex="1"
                      bg="#2f6df6"
                      color="white"
                      _hover={{ bg: "#2760dc" }}
                      onClick={handleAddToDashboard}
                      loading={isAdding}
                      disabled={!layoutId}
                    >
                      <Plus size={14} />
                      Dashboard&apos;a Ekle
                    </Button>
                    <Button
                      variant="outline"
                      borderColor={borderColor}
                      color={textMuted}
                      onClick={handleGenerate}
                    >
                      <RefreshCw size={14} />
                      Yeniden Dene
                    </Button>
                  </Flex>
                </Box>
              )}
            </VStack>
          </Dialog.Body>
        </Dialog.Content>
      </Dialog.Positioner>
    </Dialog.Root>
  );
}
