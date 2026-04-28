import { useState, useEffect } from "react"
import { Box, Flex, Text, VStack, Skeleton } from "@chakra-ui/react"
import { TrendingUp, TrendingDown, Minus, Landmark, Banknote, Clock } from "lucide-react"
import { useColorModeValue } from "@/components/ui/color-mode"
import api from "@/services/api"

const CURRENCY_SYMBOLS: Record<string, string> = { TRY: "₺", USD: "$", EUR: "€" }
const ACCOUNT_ICONS = [Landmark, Banknote, Clock]

function formatBalance(amount: number, currency: string): string {
  const symbol = CURRENCY_SYMBOLS[currency] ?? "₺"
  return `${symbol}${amount.toLocaleString("tr-TR", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

interface AccountItem { name: string; balance: string; trend: "up" | "down" | "neutral" }
interface ExpenseItem { name: string; value: number }

export function AccountsSummary() {
  const [accounts, setAccounts] = useState<AccountItem[]>([])
  const [expenses, setExpenses] = useState<ExpenseItem[]>([])
  const [loading, setLoading] = useState(true)

  const cardBg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const positive = useColorModeValue("#16a34a", "#4ade80")
  const negative = useColorModeValue("#dc2626", "#f87171")
  const iconBg = useColorModeValue("#f4f4f5", "#1a1a1a")

  useEffect(() => {
    async function load() {
      try {
        const [accountsRes, txRes] = await Promise.all([
          api.get("/api/bank-accounts"),
          api.get("/api/transactions", { params: { type: "EXPENSE" } }),
        ])

        const bankAccounts: any[] = accountsRes.data
        setAccounts(
          bankAccounts.slice(0, 3).map((a) => ({
            name: `${a.bankName} — ${a.currency}`,
            balance: formatBalance(a.currentBalance ?? 0, a.currency),
            trend: "neutral" as const,
          }))
        )

        const txs: any[] = txRes.data
        const totalExpense = txs.reduce((sum, t) => sum + t.amount, 0)
        if (totalExpense > 0) {
          const categoryMap: Record<string, number> = {}
          for (const t of txs) {
            const cat = t.category || "Diğer"
            categoryMap[cat] = (categoryMap[cat] ?? 0) + t.amount
          }
          setExpenses(
            Object.entries(categoryMap)
              .sort((a, b) => b[1] - a[1])
              .slice(0, 5)
              .map(([name, amount]) => ({
                name,
                value: Math.round((amount / totalExpense) * 100),
              }))
          )
        }
      } catch (err) {
        console.error("AccountsSummary fetch error:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <Flex gap="4" direction={{ base: "column", lg: "row" }}>
        <Skeleton height="200px" borderRadius="lg" flex="1" />
        <Skeleton height="200px" borderRadius="lg" flex="1" />
      </Flex>
    )
  }

  return (
    <Flex gap="4" direction={{ base: "column", lg: "row" }}>
      {/* Hesaplar */}
      <Box bg={cardBg} border="1px solid" borderColor={borderColor} borderRadius="lg" p="5" flex="1" minW="0">
        <Text fontSize="sm" fontWeight="600" color={textPrimary} mb="1">Hesaplar</Text>
        <Text fontSize="xs" color={textMuted} mb="4">Güncel bakiyeler</Text>
        {accounts.length === 0 ? (
          <Flex justify="center" align="center" height="80px">
            <Text fontSize="sm" color={textMuted}>Banka hesabı bulunamadı</Text>
          </Flex>
        ) : (
          <VStack gap="3" align="stretch">
            {accounts.map((account, i) => {
              const AccountIcon = ACCOUNT_ICONS[i % ACCOUNT_ICONS.length]
              return (
                <Flex
                  key={account.name}
                  align="center" justify="space-between"
                  py="2.5" px="3" borderRadius="md"
                  _hover={{ bg: iconBg }} transition="background 0.1s ease" cursor="pointer"
                >
                  <Flex align="center" gap="3">
                    <Flex align="center" justify="center" w="8" h="8" borderRadius="md" bg={iconBg}>
                      <AccountIcon size={15} color={textMuted} />
                    </Flex>
                    <Box>
                      <Text fontSize="sm" fontWeight="500" color={textPrimary}>{account.name}</Text>
                      <Text fontSize="xs" color={textMuted} className="tabular-nums">{account.balance}</Text>
                    </Box>
                  </Flex>
                  <Box>
                    {account.trend === "up" && <TrendingUp size={14} color={positive} />}
                    {account.trend === "down" && <TrendingDown size={14} color={negative} />}
                    {account.trend === "neutral" && <Minus size={14} color={textMuted} />}
                  </Box>
                </Flex>
              )
            })}
          </VStack>
        )}
      </Box>

      {/* Gider Dağılımı */}
      <Box bg={cardBg} border="1px solid" borderColor={borderColor} borderRadius="lg" p="5" flex="1" minW="0">
        <Text fontSize="sm" fontWeight="600" color={textPrimary} mb="1">Gider Dağılımı</Text>
        <Text fontSize="xs" color={textMuted} mb="4">Kategoriye göre</Text>
        {expenses.length === 0 ? (
          <Flex justify="center" align="center" height="80px">
            <Text fontSize="sm" color={textMuted}>Henüz gider işlemi yok</Text>
          </Flex>
        ) : (
          <VStack gap="3" align="stretch">
            {expenses.map((item) => (
              <Box key={item.name}>
                <Flex justify="space-between" mb="1.5">
                  <Text fontSize="xs" fontWeight="500" color={textPrimary}>{item.name}</Text>
                  <Text fontSize="xs" fontWeight="500" color={textMuted} className="tabular-nums">%{item.value}</Text>
                </Flex>
                <Box height="4px" borderRadius="full" bg={iconBg} overflow="hidden">
                  <Box
                    height="100%" borderRadius="full" bg={textPrimary}
                    width={`${item.value}%`}
                    opacity={0.6 + (item.value / 100) * 0.4}
                    transition="width 0.4s ease"
                  />
                </Box>
              </Box>
            ))}
          </VStack>
        )}
      </Box>
    </Flex>
  )
}
