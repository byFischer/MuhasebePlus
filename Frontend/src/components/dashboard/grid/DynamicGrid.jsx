import { Suspense } from "react";
import { Box, Flex, Text } from "@chakra-ui/react";
import { Plus } from "lucide-react";
import WidgetFrame from "./WidgetFrame";
import WidgetSkeleton from "./WidgetSkeleton";
import { getWidgetMeta } from "../widgets/registry";
import { useEditMode } from "../../../hooks/useEditMode";
import { useColorModeValue } from "../../ui/color-mode";

// Slot tanımları — colSpan: kaç sütun, minHeight: boş slot görsel yüksekliği
export const SLOT_DEFS = [
  { id: 0, colSpan: 12, minHeight: 140 }, // Tam genişlik (KPI)
  { id: 1, colSpan: 7,  minHeight: 300 }, // Sol büyük grafik
  { id: 2, colSpan: 5,  minHeight: 300 }, // Sağ küçük grafik
  { id: 3, colSpan: 4,  minHeight: 220 }, // 1/3 sol
  { id: 4, colSpan: 4,  minHeight: 220 }, // 1/3 orta
  { id: 5, colSpan: 4,  minHeight: 220 }, // 1/3 sağ
  { id: 6, colSpan: 6,  minHeight: 220 }, // 1/2 sol
  { id: 7, colSpan: 6,  minHeight: 220 }, // 1/2 sağ
];

// positionX = slot index (0–7), positionY = sayfa index (0, 1, 2...)
export const DEFAULT_WIDGETS = [
  { widgetId: "kpi",      widgetType: "KPI",           title: "KPI Kartları",    positionX: 0, positionY: 0, width: 12, height: 3 },
  { widgetId: "revenue",  widgetType: "TIME_SERIES",   title: "Gelir Trendi",    positionX: 1, positionY: 0, width: 7,  height: 5, config: { chartType: "REVENUE" } },
  { widgetId: "cashflow", widgetType: "TIME_SERIES",   title: "Nakit Akışı",     positionX: 2, positionY: 0, width: 5,  height: 5, config: { chartType: "CASHFLOW" } },
  { widgetId: "accounts", widgetType: "ACCOUNTS",      title: "Banka Hesapları", positionX: 3, positionY: 0, width: 6,  height: 4 },
  { widgetId: "activity", widgetType: "ACTIVITY_FEED", title: "Son İşlemler",    positionX: 4, positionY: 0, width: 6,  height: 4 },
];

// Eski büyük positionY değerlerini (999 gibi) sayfa 0'a normalize et
const normPage = (w) => {
  const py = w.positionY ?? 0;
  return py > 20 ? 0 : py;
};

function EmptySlot({ colSpan, minHeight, onAddWidget, borderColor }) {
  return (
    <Box
      gridColumn={`span ${colSpan}`}
      minHeight={`${minHeight}px`}
      border="2px dashed"
      borderColor={borderColor}
      borderRadius="xl"
      display="flex"
      alignItems="center"
      justifyContent="center"
      cursor="pointer"
      onClick={onAddWidget}
      transition="all 0.15s ease"
      _hover={{ borderColor: "#2f6df6", bg: "#2f6df60a" }}
    >
      <Flex direction="column" align="center" gap="2" opacity={0.45}>
        <Box
          w="9" h="9"
          borderRadius="full"
          border="1.5px dashed currentColor"
          display="flex"
          alignItems="center"
          justifyContent="center"
        >
          <Plus size={16} />
        </Box>
        <Text fontSize="xs" fontWeight="500">Widget Ekle</Text>
      </Flex>
    </Box>
  );
}

export default function DynamicGrid({
  widgets,
  layoutId,
  onRemoveWidget,
  onAddWidget,
  currentPage = 0,
  onPageChange,
  onAddPage,
  totalPages: totalPagesProp,
}) {
  const { isEditMode } = useEditMode();
  const emptyBorderColor = useColorModeValue("#d4d4d8", "#3f3f46");
  const pageDotBg    = useColorModeValue("#f4f4f5",  "#1a1a1a");
  const pageDotColor = useColorModeValue("#71717a",  "#71717a");
  const textMuted    = useColorModeValue("#71717a",  "#a1a1aa");

  const source = widgets ?? [];

  // Geçerli sayfadaki widget'lar
  const pageWidgets = source.filter((w) => normPage(w) === currentPage);
  const sorted = [...pageWidgets].sort((a, b) => (a.positionX ?? 0) - (b.positionX ?? 0));
  const slotMap = {};
  sorted.forEach((w, i) => { if (i < SLOT_DEFS.length) slotMap[i] = w; });

  // Toplam sayfa sayısı (prop override veya widget datasından)
  const allPages = source.map(normPage);
  const maxPage = allPages.length > 0 ? Math.max(...allPages) : 0;
  const totalPages = Math.max(totalPagesProp ?? 1, maxPage + 1);

  return (
    <Box>
      {/* Widget Grid */}
      <Box display="grid" gridTemplateColumns="repeat(12, 1fr)" gap="4" pt="4">
        {SLOT_DEFS.map((slot) => {
          const widget = slotMap[slot.id];

          if (!widget) {
            return (
              <EmptySlot
                key={`empty-${slot.id}`}
                colSpan={slot.colSpan}
                minHeight={slot.minHeight}
                onAddWidget={onAddWidget}
                borderColor={emptyBorderColor}
              />
            );
          }

          const { component: WidgetComponent, title } = getWidgetMeta(widget.widgetType);
          const config =
            typeof widget.config === "string"
              ? (() => { try { return JSON.parse(widget.config); } catch { return {}; } })()
              : (widget.config ?? {});

          return (
            <Box key={String(widget.widgetId)} gridColumn={`span ${slot.colSpan}`}>
              <WidgetFrame
                title={widget.title || title}
                isEditMode={isEditMode}
                onRemove={
                  isEditMode && layoutId
                    ? () => onRemoveWidget?.(widget.widgetId)
                    : undefined
                }
              >
                <Suspense fallback={<WidgetSkeleton />}>
                  <WidgetComponent
                    widgetId={layoutId ? widget.widgetId : undefined}
                    layoutId={layoutId}
                    config={config}
                    widgetType={widget.widgetType}
                  />
                </Suspense>
              </WidgetFrame>
            </Box>
          );
        })}
      </Box>

      {/* Sayfalama */}
      <Flex justify="center" align="center" gap="2" pt="8" pb="4">
        {Array.from({ length: totalPages }, (_, i) => (
          <Box
            key={i}
            as="button"
            w="28px"
            h="28px"
            borderRadius="full"
            bg={i === currentPage ? "#2f6df6" : pageDotBg}
            border="1px solid"
            borderColor={i === currentPage ? "#2f6df6" : emptyBorderColor}
            display="flex"
            alignItems="center"
            justifyContent="center"
            cursor="pointer"
            onClick={() => onPageChange?.(i)}
            transition="all 0.15s ease"
            _hover={{ borderColor: "#2f6df6" }}
            fontSize="11px"
            fontWeight="600"
            color={i === currentPage ? "white" : pageDotColor}
            fontFamily="inherit"
          >
            {i + 1}
          </Box>
        ))}

        {/* Yeni sayfa ekle */}
        <Box
          as="button"
          w="28px"
          h="28px"
          borderRadius="full"
          bg="transparent"
          border="1.5px dashed"
          borderColor={emptyBorderColor}
          display="flex"
          alignItems="center"
          justifyContent="center"
          cursor="pointer"
          onClick={onAddPage}
          transition="all 0.15s ease"
          _hover={{ borderColor: "#2f6df6" }}
          title="Yeni sayfa ekle"
          fontFamily="inherit"
        >
          <Plus size={13} color={textMuted} />
        </Box>
      </Flex>
    </Box>
  );
}
