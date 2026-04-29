import { useState, useEffect } from "react";
import { Box, VStack, Text } from "@chakra-ui/react";
import { useColorModeValue } from "../../ui/color-mode";

const MOOD_COLORS = { excellent: "#4ade80", good: "#60a5fa", fair: "#fbbf24", poor: "#f87171" };

function ScoreRing({ score, color }) {
  const radius = 36;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (score / 100) * circumference;
  return (
    <svg width="90" height="90" viewBox="0 0 90 90">
      <circle cx="45" cy="45" r={radius} fill="none" stroke="#262626" strokeWidth="8" />
      <circle
        cx="45" cy="45" r={radius} fill="none"
        stroke={color} strokeWidth="8"
        strokeDasharray={circumference} strokeDashoffset={offset}
        strokeLinecap="round"
        transform="rotate(-90 45 45)"
        style={{ transition: "stroke-dashoffset 0.6s ease" }}
      />
      <text x="45" y="50" textAnchor="middle" fontSize="18" fontWeight="700" fill={color}>
        {score}
      </text>
    </svg>
  );
}

export default function MoodMeterWidget({ widgetId, layoutId }) {
  const [data, setData] = useState(null);
  const textPrimary = useColorModeValue("#09090b", "#fafafa");
  const textMuted = useColorModeValue("#71717a", "#a1a1aa");

  useEffect(() => {
    if (!widgetId || !layoutId) return;
    import("../../../services/dashboardService").then(({ dashboardService }) => {
      dashboardService.getWidgetData(layoutId, widgetId).then(setData).catch(() => {});
    });
  }, [widgetId, layoutId]);

  if (!data) {
    return (
      <Box p="4" minH="120px" display="flex" alignItems="center" justifyContent="center">
        <Text fontSize="sm" color={textMuted}>Yükleniyor...</Text>
      </Box>
    );
  }

  const color = MOOD_COLORS[data.mood] ?? "#60a5fa";
  const moodLabel = data.score >= 75 ? "Mükemmel" : data.score >= 50 ? "İyi" : data.score >= 25 ? "Orta" : "Zayıf";

  return (
    <VStack gap="3" p="4" minH="150px" align="center" justify="center">
      <ScoreRing score={data.score} color={color} />
      <VStack gap="1">
        <Text fontSize="sm" fontWeight="600" color={textPrimary}>
          Finansal Sağlık: {moodLabel}
        </Text>
        <Text fontSize="xs" color={textMuted}>Gelir/Gider oranına göre</Text>
      </VStack>
    </VStack>
  );
}
