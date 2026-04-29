import { useState, useEffect, useCallback } from "react";
import { Box, Flex, Skeleton, Text, Button } from "@chakra-ui/react";
import { Sidebar } from "../components/dashboard/Sidebar";
import { Header } from "../components/dashboard/Header";
import { useColorModeValue } from "../components/ui/color-mode";
import { useDashboardLayout, useSaveLayout, useDeleteWidget } from "../hooks/useDashboardLayout";
import { useEditMode } from "../hooks/useEditMode";
import DynamicGrid, { SLOT_DEFS } from "../components/dashboard/grid/DynamicGrid";
import DashboardToolbar from "../components/dashboard/controls/DashboardToolbar";
import WidgetCatalogDrawer from "../components/dashboard/catalog/WidgetCatalogDrawer";
import AiWidgetStudio from "../components/dashboard/ai-studio/AiWidgetStudio";

const MAX_SLOTS = SLOT_DEFS.length; // 8
const normPage = (w) => { const py = w.positionY ?? 0; return py > 20 ? 0 : py; };

export default function DashboardPage() {
  const pageBg = useColorModeValue("#fafafa", "#0a0a0a");
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [aiStudioOpen, setAiStudioOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const { data: layout, isLoading, error } = useDashboardLayout();
  const { mutateAsync: saveLayout, isPending: isSaving } = useSaveLayout();
  const { mutateAsync: deleteWidget } = useDeleteWidget();
  const { pendingPositions, exitEdit } = useEditMode();

  const allWidgets = layout?.widgets ?? [];

  // Sync totalPages when layout loads
  useEffect(() => {
    if (allWidgets.length > 0) {
      const maxP = Math.max(...allWidgets.map(normPage));
      setTotalPages((prev) => Math.max(prev, maxP + 1));
    }
  }, [allWidgets.length]);

  // Count widgets on a given page index
  const widgetsOnPage = useCallback(
    (pageIdx) => allWidgets.filter((w) => normPage(w) === pageIdx).length,
    [allWidgets]
  );

  // Find first page (from currentPage onwards) that has room for a new widget.
  // If all pages up to totalPages-1 are full, the widget goes to a new page.
  const computeTargetSlot = useCallback(() => {
    let p = currentPage;
    const maxP = totalPages - 1;
    while (widgetsOnPage(p) >= MAX_SLOTS && p <= maxP) p++;
    return { targetPage: p, targetSlot: widgetsOnPage(p) };
  }, [currentPage, totalPages, widgetsOnPage]);

  const { targetPage, targetSlot } = computeTargetSlot();

  const handleSave = async () => {
    if (!layout?.layoutId || !pendingPositions.length) return;
    await saveLayout({ layoutId: layout.layoutId, widgets: pendingPositions });
  };

  const handleRemoveWidget = async (widgetId) => {
    if (!layout?.layoutId) return;
    if (typeof widgetId === "string" && isNaN(Number(widgetId))) return;
    await deleteWidget({ layoutId: layout.layoutId, widgetId });
  };

  const handleAddPage = () => {
    const newPage = totalPages;
    setTotalPages((prev) => prev + 1);
    setCurrentPage(newPage);
  };

  // Called by catalog/AI studio after a widget is successfully added
  const handleWidgetAdded = useCallback((addedToPage) => {
    setTotalPages((prev) => Math.max(prev, addedToPage + 1));
    setCurrentPage(addedToPage);
  }, []);

  const displayWidgets = allWidgets;
  const displayLayoutId = layout?.layoutId ?? null;

  return (
    <Flex height="100dvh" overflow="hidden">
      <Sidebar />
      <Flex flex="1" direction="column" overflow="hidden" minW="0">
        <Header />
        <DashboardToolbar
          layoutName={layout?.name}
          onSave={handleSave}
          onReset={exitEdit}
          onAddWidget={() => setCatalogOpen(true)}
          onAiStudio={() => setAiStudioOpen(true)}
          isSaving={isSaving}
        />

        <Box flex="1" overflowY="auto" bg={pageBg} px="4" pb="6" pt="2">
          {isLoading ? (
            <Flex direction="column" gap="4" maxW="1400px" mx="auto" pt="4">
              <Skeleton height="200px" borderRadius="lg" />
              <Flex gap="4">
                <Skeleton flex="1" height="300px" borderRadius="lg" />
                <Skeleton flex="1" height="300px" borderRadius="lg" />
              </Flex>
            </Flex>
          ) : error ? (
            <Flex direction="column" align="center" justify="center" h="300px" gap="3">
              <Text fontSize="sm" color="#f87171">Dashboard yüklenemedi.</Text>
              <Button size="sm" bg="#2f6df6" color="white" onClick={() => window.location.reload()}>
                Sayfayı Yenile
              </Button>
            </Flex>
          ) : (
            <Box maxW="1400px" mx="auto">
              <DynamicGrid
                widgets={displayWidgets}
                layoutId={displayLayoutId}
                onRemoveWidget={handleRemoveWidget}
                onAddWidget={() => setCatalogOpen(true)}
                currentPage={currentPage}
                onPageChange={setCurrentPage}
                onAddPage={handleAddPage}
                totalPages={totalPages}
              />
            </Box>
          )}
        </Box>
      </Flex>

      <WidgetCatalogDrawer
        open={catalogOpen}
        onClose={() => setCatalogOpen(false)}
        layoutId={layout?.layoutId}
        nextSlotIndex={targetSlot}
        pageIndex={targetPage}
        onAdded={handleWidgetAdded}
      />

      <AiWidgetStudio
        open={aiStudioOpen}
        onClose={() => setAiStudioOpen(false)}
        layoutId={layout?.layoutId}
        nextSlotIndex={targetSlot}
        pageIndex={targetPage}
        onAdded={handleWidgetAdded}
      />
    </Flex>
  );
}
