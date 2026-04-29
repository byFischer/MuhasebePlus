import { useState, useEffect } from "react";
import { Box, Text, Spinner, VStack } from "@chakra-ui/react";
import { useColorModeValue } from "../../ui/color-mode";

export default function GenericDataWidget({ widgetId, layoutId, widgetType }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");
  const textPrimary = useColorModeValue("#09090b", "#fafafa");

  useEffect(() => {
    if (!widgetId || !layoutId) { setLoading(false); return; }
    import("../../../services/dashboardService").then(({ dashboardService }) => {
      dashboardService.getWidgetData(layoutId, widgetId)
        .then((d) => { setData(d); setLoading(false); })
        .catch(() => { setError("Veri yüklenemedi"); setLoading(false); });
    });
  }, [widgetId, layoutId]);

  if (loading) return (
    <Box p="4" minH="80px" display="flex" alignItems="center" justifyContent="center">
      <Spinner size="sm" color="#2f6df6" />
    </Box>
  );

  if (error) return (
    <Box p="4" minH="80px" display="flex" alignItems="center" justifyContent="center">
      <Text fontSize="sm" color="#f87171">{error}</Text>
    </Box>
  );

  return (
    <Box p="4">
      <pre style={{ fontSize: "11px", color: textMuted, whiteSpace: "pre-wrap", wordBreak: "break-all" }}>
        {JSON.stringify(data, null, 2)}
      </pre>
    </Box>
  );
}
