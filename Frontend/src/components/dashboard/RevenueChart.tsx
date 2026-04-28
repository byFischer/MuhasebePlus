import { useState, useEffect } from "react"
import { Box, Flex, Text, Skeleton } from "@chakra-ui/react"
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from "recharts"
import { useColorModeValue } from "@/components/ui/color-mode"
import api from "@/services/api"

const TR_MONTHS = ["Oca","Şub","Mar","Nis","May","Haz","Tem","Ağu","Eyl","Eki","Kas","Ara"]

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
          <Box w="2" h="2" borderRadius="full" bg={entry.color} />
          <Text fontSize="xs" color={textMuted}>{entry.name === "gelir" ? "Gelir" : "Gider"}:</Text>
          <Text fontSize="xs" fontWeight="600" color={textPrimary} className="tabular-nums">
            ₺{entry.value.toLocaleString("tr-TR")}
          </Text>
        </Flex>
      ))}
    </Box>
  )
}

interface ChartPoint { month: string; gelir: number; gider: number }

export function RevenueChart() {
  const [data, setData] = useState<ChartPoint[]>([])
  const [loading, setLoading] = useState(true)

  const cardBg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const gridColor = useColorModeValue("#f4f4f5", "#1a1a1a")
  const lineIncome = useColorModeValue("#09090b", "#fafafa")
  const lineExpense = useColorModeValue("#a1a1aa", "#52525b")

  useEffect(() => {
    async function load() {
      try {
        const now = new Date()
        const start = new Date(now.getFullYear(), now.getMonth() - 5, 1)
        const startDate = `${start.getFullYear()}-${String(start.getMonth() + 1).padStart(2, "0")}-01`
        const endDate = now.toISOString().split("T")[0]

        const res = await api.get("/api/transactions", { params: { startDate, endDate } })
        const transactions: any[] = res.data

        // Build last 6 months slots
        const slots: ChartPoint[] = Array.from({ length: 6 }, (_, i) => {
          const d = new Date(now.getFullYear(), now.getMonth() - (5 - i), 1)
          return { month: TR_MONTHS[d.getMonth()], gelir: 0, gider: 0 }
        })

        for (const tx of transactions) {
          const txDate = new Date(tx.transactionDate)
          const idx = slots.findIndex((_, i) => {
            const d = new Date(now.getFullYear(), now.getMonth() - (5 - i), 1)
            return txDate.getFullYear() === d.getFullYear() && txDate.getMonth() === d.getMonth()
          })
          if (idx < 0) continue
          if (tx.transactionType === "INCOME") slots[idx].gelir += tx.amount
          else slots[idx].gider += tx.amount
        }

        setData(slots)
      } catch (err) {
        console.error("RevenueChart fetch error:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return <Skeleton height="300px" borderRadius="lg" flex="1" minW="0" />
  }

  return (
    <Box bg={cardBg} border="1px solid" borderColor={borderColor} borderRadius="lg" p="5" flex="1" minW="0">
      <Flex justify="space-between" align="center" mb="5">
        <Box>
          <Text fontSize="sm" fontWeight="600" color={textPrimary}>Gelir & Gider</Text>
          <Text fontSize="xs" color={textMuted}>Son 6 ay</Text>
        </Box>
        <Flex gap="4">
          <Flex align="center" gap="1.5">
            <Box w="2" h="2" borderRadius="full" bg={lineIncome} />
            <Text fontSize="xs" color={textMuted}>Gelir</Text>
          </Flex>
          <Flex align="center" gap="1.5">
            <Box w="2" h="2" borderRadius="full" bg={lineExpense} />
            <Text fontSize="xs" color={textMuted}>Gider</Text>
          </Flex>
        </Flex>
      </Flex>
      <Box height="240px">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 4, right: 4, left: 4, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke={gridColor} vertical={false} />
            <XAxis dataKey="month" tick={{ fill: textMuted, fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis tickFormatter={formatCurrency} tick={{ fill: textMuted, fontSize: 11 }} axisLine={false} tickLine={false} />
            <Tooltip content={<CustomTooltip />} />
            <defs>
              <linearGradient id="incomeGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={lineIncome} stopOpacity={0.12} />
                <stop offset="100%" stopColor={lineIncome} stopOpacity={0} />
              </linearGradient>
              <linearGradient id="expenseGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={lineExpense} stopOpacity={0.08} />
                <stop offset="100%" stopColor={lineExpense} stopOpacity={0} />
              </linearGradient>
            </defs>
            <Area type="monotone" dataKey="gelir" stroke={lineIncome} strokeWidth={1.5} fill="url(#incomeGrad)" dot={false} activeDot={{ r: 3, strokeWidth: 0, fill: lineIncome }} />
            <Area type="monotone" dataKey="gider" stroke={lineExpense} strokeWidth={1.5} fill="url(#expenseGrad)" dot={false} activeDot={{ r: 3, strokeWidth: 0, fill: lineExpense }} />
          </AreaChart>
        </ResponsiveContainer>
      </Box>
    </Box>
  )
}
