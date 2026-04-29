import { useState } from "react";
import {
  Drawer, Box, Flex, Text, Button, VStack, Badge, Input,
} from "@chakra-ui/react";
import { X, Plus, Search, AlertCircle } from "lucide-react";
import { useColorModeValue } from "../../ui/color-mode";
import { useQuery } from "@tanstack/react-query";
import { dashboardService } from "../../../services/dashboardService";
import { useAddWidget } from "../../../hooks/useDashboardLayout";
import { toaster } from "../../ui/toaster";

const CATEGORY_COLORS = {
  Finansal: "#2f6df6",
  Operasyonel: "#10b981",
  Akıllı: "#f59e0b",
  AI: "#8b5cf6",
  Mikro: "#ec4899",
  Görsel: "#06b6d4",
};

export default function WidgetCatalogDrawer({ open, onClose, layoutId, nextSlotIndex = 0, pageIndex = 0, onAdded }) {
  const [search, setSearch] = useState("");
  const [activeCategory, setActiveCategory] = useState("Tümü");
  const [addingType, setAddingType] = useState(null);
  const borderColor = useColorModeValue("#e4e4e7", "#262626");
  const cardBg = useColorModeValue("#f4f4f5", "#1a1a1a");
  const textPrimary = useColorModeValue("#09090b", "#fafafa");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");
  const bg = useColorModeValue("#ffffff", "#111111");

  const { data: catalog = [] } = useQuery({
    queryKey: ["widget-catalog"],
    queryFn: dashboardService.getWidgetCatalog,
  });

  const { mutateAsync: addWidget } = useAddWidget();

  const categories = ["Tümü", ...new Set(catalog.map((w) => w.category))];

  const filtered = catalog.filter((w) => {
    const matchSearch = !search || w.name.toLowerCase().includes(search.toLowerCase()) ||
      w.description.toLowerCase().includes(search.toLowerCase());
    const matchCat = activeCategory === "Tümü" || w.category === activeCategory;
    return matchSearch && matchCat;
  });

  const handleAdd = async (widget) => {
    if (!layoutId) {
      toaster.create({ title: "Dashboard yükleniyor", description: "Lütfen sayfanın yüklenmesini bekleyin.", type: "error", duration: 3000 });
      return;
    }
    setAddingType(widget.type);
    try {
      await addWidget({
        layoutId,
        dto: {
          widgetType: widget.type,
          title: widget.name,
          positionX: nextSlotIndex,
          positionY: pageIndex,
          width: widget.defaultWidth,
          height: widget.defaultHeight,
          config: JSON.stringify({ dateRange: "THIS_MONTH" }),
        },
      });
      onAdded?.(pageIndex);
      onClose();
    } catch {
      // error toast already shown by useAddWidget onError
    } finally {
      setAddingType(null);
    }
  };

  return (
    <Drawer.Root open={open} onOpenChange={(e) => !e.open && onClose()} placement="end" size="sm">
      <Drawer.Backdrop />
      <Drawer.Positioner>
        <Drawer.Content bg={bg} borderLeft="1px solid" borderColor={borderColor}>
          <Drawer.Header borderBottom="1px solid" borderColor={borderColor} pb="3">
            <Flex align="center" justify="space-between">
              <Drawer.Title fontSize="sm" fontWeight="700" color={textPrimary}>
                Widget Kataloğu
              </Drawer.Title>
              <Drawer.CloseTrigger asChild>
                <Button size="xs" variant="ghost" color={textMuted} onClick={onClose}>
                  <X size={14} />
                </Button>
              </Drawer.CloseTrigger>
            </Flex>

            {!layoutId && (
              <Flex align="center" gap="2" mt="2" p="2" bg="#f871711a" borderRadius="md">
                <AlertCircle size={13} color="#f87171" />
                <Text fontSize="xs" color="#f87171">Dashboard bağlantısı kurulamadı. Sayfayı yenileyin.</Text>
              </Flex>
            )}

            <Box mt="3" position="relative">
              <Search size={14} style={{ position: "absolute", left: 10, top: "50%", transform: "translateY(-50%)", color: textMuted }} />
              <Input
                pl="8"
                size="sm"
                placeholder="Widget ara..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                borderColor={borderColor}
                _focus={{ borderColor: "#2f6df6" }}
              />
            </Box>

            <Flex gap="2" mt="3" wrap="wrap">
              {categories.map((cat) => (
                <Button
                  key={cat}
                  size="xs"
                  variant={activeCategory === cat ? "solid" : "outline"}
                  bg={activeCategory === cat ? "#2f6df6" : "transparent"}
                  color={activeCategory === cat ? "white" : textMuted}
                  borderColor={borderColor}
                  onClick={() => setActiveCategory(cat)}
                  _hover={{ borderColor: "#2f6df6" }}
                >
                  {cat}
                </Button>
              ))}
            </Flex>
          </Drawer.Header>

          <Drawer.Body overflowY="auto" p="3">
            <VStack gap="2" align="stretch">
              {filtered.map((widget) => (
                <Flex
                  key={widget.type}
                  align="center"
                  justify="space-between"
                  bg={cardBg}
                  borderRadius="lg"
                  p="3"
                  gap="3"
                >
                  <VStack align="start" gap="1" flex="1" minW="0">
                    <Flex align="center" gap="2">
                      <Text fontSize="sm" fontWeight="600" color={textPrimary}>
                        {widget.name}
                      </Text>
                      <Badge
                        fontSize="9px"
                        px="1.5"
                        py="0"
                        borderRadius="full"
                        bg={(CATEGORY_COLORS[widget.category] ?? "#6b7280") + "22"}
                        color={CATEGORY_COLORS[widget.category] ?? "#6b7280"}
                      >
                        {widget.category}
                      </Badge>
                    </Flex>
                    <Text fontSize="xs" color={textMuted} lineClamp={2}>
                      {widget.description}
                    </Text>
                  </VStack>
                  <Button
                    size="xs"
                    bg={layoutId ? "#2f6df6" : "#3f3f46"}
                    color="white"
                    _hover={{ bg: layoutId ? "#2760dc" : "#3f3f46" }}
                    onClick={() => handleAdd(widget)}
                    loading={addingType === widget.type}
                    flexShrink={0}
                  >
                    <Plus size={12} />
                  </Button>
                </Flex>
              ))}
              {filtered.length === 0 && (
                <Text fontSize="sm" color={textMuted} textAlign="center" py="6">
                  Widget bulunamadı.
                </Text>
              )}
            </VStack>
          </Drawer.Body>
        </Drawer.Content>
      </Drawer.Positioner>
    </Drawer.Root>
  );
}
