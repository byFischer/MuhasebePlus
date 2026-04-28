import { useState, useEffect } from "react"
import { Box, Flex, Text, Skeleton } from "@chakra-ui/react"
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts"
import { useColorModeValue } from "@/components/ui/color-mode"
import api from "@/services/api"

function formatCurrency(value: number) {
  if (value >= 1000000) return `${(value / 1000000).toFixed(1)}M`
  return `${(value / 1000).toFixed(0)}K`
}

interface TooltipPayloadItem { name: string; value: number; color: string }

function CustomTooltip({ active, payload, label }: { active?: boolean; payload?: TooltipPayloadItem[]; label?: string }) {
  const bg = useColorModeValue("#ffffff", "#111111")
  const border = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  if (!active || !payload) return null
  return (
    <Box bg={bg} border="1px solid" borderColor={border} borderRadius="md" p="3" shadow="sm">
      <Text fontSize="xs" color={textMuted} mb="2">{label}</Text>
      {payload.map((entry, i) => (
        <Flex key={i} align="center" gap="2" mb="0.5">
          <Box w="2" h="2" borderRadius="sm" bg={entry.color} />
          <Text fontSize="xs" color={textMuted}>{entry.name === "giren" ? "Giren" : "Çıkan"}:</Text>
          <Text fontSize="xs" fontWeight="600" color={textPrimary} className="tabular-nums">
            ₺{entry.value.toLocaleString("tr-TR")}
          </Text>
        </Flex>
      ))}
    </Box>
  )
}

interface WeekPoint { week: string; giren: number; cikan: number }

export function CashFlowChart() {
  const [data, setData] = useState<WeekPoint[]>([])
  const [loading, setLoading] = useState(true)

  const cardBg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const gridColor = useColorModeValue("#f4f4f5", "#1a1a1a")
  const barIn = useColorModeValue("#09090b", "#fafafa")
  const barOut = useColorModeValue("#d4d4d8", "#3f3f46")

  useEffect(() => {
    async function load() {
      try {
        const now = new Date()
        const sixWeeksAgo = new Date(now)
        sixWeeksAgo.setDate(sixWeeksAgo.getDate() - 42)
        const startDate = sixWeeksAgo.toISOString().split("T")[0]
        const endDate = now.toISOString().split("T")[0]

        const res = await api.get("/api/transactions", { params: { startDate, endDate } })
        const transactions: any[] = res.data

        const slots: WeekPoint[] = Array.from({ length: 6 }, (_, i) => ({
          week: `H${i + 1}`,
          giren: 0,
          cikan: 0,
        }))

        for (const tx of transactions) {
          const txDate = new Date(tx.transactionDate)
          const daysAgo = Math.floor((now.getTime() - txDate.getTime()) / (1000 * 60 * 60 * 24))
          const slotIndex = 5 - Math.min(5, Math.floor(daysAgo / 7))
          if (slotIndex < 0 || slotIndex > 5) continue
          if (tx.transactionType === "INCOME") slots[slotIndex].giren += tx.amount
          else slots[slotIndex].cikan += tx.amount
        }

        setData(slots)
      } catch (err) {
        console.error("CashFlowChart fetch error:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <Skeleton
        height="300px"
        borderRadius="lg"
        width={{ base: "100%", lg: "340px" }}
        flexShrink={0}
      />
    )
  }

  return (
    <Box bg={cardBg} border="1px solid" borderColor={borderColor} borderRadius="lg" p="5" width={{ base: "100%", lg: "340px" }} flexShrink={0}>
      <Flex justify="space-between" align="center" mb="5">
        <Box>
          <Text fontSize="sm" fontWeight="600" color={textPrimary}>Nakit Akışı</Text>
          <Text fontSize="xs" color={textMuted}>Haftalık</Text>
        </Box>
        <Flex gap="3">
          <Flex align="center" gap="1.5">
            <Box w="2" h="2" borderRadius="sm" bg={barIn} />
            <Text fontSize="xs" color={textMuted}>Giren</Text>
          </Flex>
          <Flex align="center" gap="1.5">
            <Box w="2" h="2" borderRadius="sm" bg={barOut} />
            <Text fontSize="xs" color={textMuted}>Çıkan</Text>
          </Flex>
        </Flex>
      </Flex>
      <Box height="240px">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 4, right: 0, left: 4, bottom: 0 }} barGap={2}>
            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
            <XAxis dataKey="week" tick={{ fill: textMuted, fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis tickFormatter={formatCurrency} tick={{ fill: textMuted, fontSize: 11 }} axisLine={false} tickLine={false} />
            <Tooltip content={<CustomTooltip />} />
            <Bar dataKey="giren" fill={barIn} radius={[3, 3, 0, 0]} maxBarSize={16} />
            <Bar dataKey="cikan" fill={barOut} radius={[3, 3, 0, 0]} maxBarSize={16} />
          </BarChart>
        </ResponsiveContainer>
      </Box>
    </Box>
  )
}
