import { useState, useEffect } from "react"
import { Box, Flex, Text, SimpleGrid, Skeleton } from "@chakra-ui/react"
import { TrendingUp, TrendingDown } from "lucide-react"
import { useColorModeValue } from "@/components/ui/color-mode"
import api from "@/services/api"

const TR_MONTHS_SHORT = ["Oca","Şub","Mar","Nis","May","Haz","Tem","Ağu","Eyl","Eki","Kas","Ara"]

function formatAmount(value: number): string {
  return `₺${value.toLocaleString("tr-TR", { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

function calcTrend(current: number, previous: number): { trend: "up" | "down"; change: string } {
  if (previous === 0) return { trend: "up", change: "+0%" }
  const pct = ((current - previous) / previous) * 100
  return {
    trend: pct >= 0 ? "up" : "down",
    change: `${pct >= 0 ? "+" : ""}${pct.toFixed(1)}%`,
  }
}

interface KpiItem {
  label: string
  period: string
  value: string
  trend: "up" | "down"
  change: string
}

export function KpiCards() {
  const [kpis, setKpis] = useState<KpiItem[]>([])
  const [loading, setLoading] = useState(true)

  const cardBg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const positive = useColorModeValue("#16a34a", "#4ade80")
  const negative = useColorModeValue("#dc2626", "#f87171")

  useEffect(() => {
    async function load() {
      try {
        const now = new Date()
        const thisMonthStart = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`
        const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1)
        const lastMonthStart = `${lastMonthDate.getFullYear()}-${String(lastMonthDate.getMonth() + 1).padStart(2, "0")}-01`
        const today = now.toISOString().split("T")[0]

        const [txRes, invRes] = await Promise.all([
          api.get("/api/transactions", { params: { startDate: lastMonthStart, endDate: today } }),
          api.get("/api/invoices", { params: { paymentStatus: "pending" } }),
        ])

        const transactions: any[] = txRes.data
        const pendingInvoices: any[] = invRes.data

        const thisMonthTxs = transactions.filter((t) => t.transactionDate >= thisMonthStart)
        const lastMonthTxs = transactions.filter((t) => t.transactionDate < thisMonthStart)

        const sum = (txs: any[], type: string) =>
          txs.filter((t) => t.transactionType === type).reduce((acc, t) => acc + t.amount, 0)

        const thisIncome = sum(thisMonthTxs, "INCOME")
        const thisExpense = sum(thisMonthTxs, "EXPENSE")
        const lastIncome = sum(lastMonthTxs, "INCOME")
        const lastExpense = sum(lastMonthTxs, "EXPENSE")
        const thisNet = thisIncome - thisExpense
        const lastNet = lastIncome - lastExpense
        const pendingTotal = pendingInvoices.reduce((acc: number, inv: any) => acc + inv.totalAmount, 0)

        const period = `${TR_MONTHS_SHORT[now.getMonth()]} ${now.getFullYear()}`

        setKpis([
          { label: "Toplam Gelir",    period, value: formatAmount(thisIncome),   ...calcTrend(thisIncome, lastIncome) },
          { label: "Toplam Gider",    period, value: formatAmount(thisExpense),  ...calcTrend(thisExpense, lastExpense) },
          { label: "Net Kâr",         period, value: formatAmount(thisNet),      ...calcTrend(thisNet, lastNet) },
          { label: "Bekleyen Fatura", period, value: formatAmount(pendingTotal), trend: pendingTotal > 0 ? "up" : "down", change: `${pendingInvoices.length} fatura` },
        ])
      } catch (err) {
        console.error("KpiCards fetch error:", err)
        setKpis([])
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <SimpleGrid columns={{ base: 1, sm: 2, lg: 4 }} gap="4">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} height="110px" borderRadius="lg" />
        ))}
      </SimpleGrid>
    )
  }

  return (
    <SimpleGrid columns={{ base: 1, sm: 2, lg: 4 }} gap="4">
      {kpis.map((kpi) => (
        <Box
          key={kpi.label}
          bg={cardBg}
          border="1px solid"
          borderColor={borderColor}
          borderRadius="lg"
          p="5"
          transition="all 0.15s ease"
        >
          <Flex justify="space-between" align="flex-start" mb="3">
            <Text fontSize="xs" fontWeight="500" color={textMuted} textTransform="uppercase" letterSpacing="0.04em">
              {kpi.label}
            </Text>
            <Text fontSize="xs" color={textMuted}>
              {kpi.period}
            </Text>
          </Flex>
          <Text fontSize="xl" fontWeight="700" color={textPrimary} letterSpacing="-0.02em" className="tabular-nums" mb="2">
            {kpi.value}
          </Text>
          <Flex align="center" gap="1.5">
            {kpi.trend === "up" ? (
              <TrendingUp size={12} color={positive} />
            ) : (
              <TrendingDown size={12} color={negative} />
            )}
            <Text fontSize="xs" fontWeight="500" color={kpi.trend === "up" ? positive : negative} className="tabular-nums">
              {kpi.change}
            </Text>
            {kpi.label !== "Bekleyen Fatura" && (
              <Text fontSize="xs" color={textMuted}>geçen aya göre</Text>
            )}
          </Flex>
        </Box>
      ))}
    </SimpleGrid>
  )
}
