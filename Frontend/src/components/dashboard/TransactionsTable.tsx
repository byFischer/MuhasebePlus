import { useState, useEffect } from "react"
import { Box, Flex, Text, Table, Skeleton } from "@chakra-ui/react"
import { ArrowUpRight, ArrowDownRight } from "lucide-react"
import { useColorModeValue } from "@/components/ui/color-mode"
import api from "@/services/api"

const TR_MONTHS = ["Oca","Şub","Mar","Nis","May","Haz","Tem","Ağu","Eyl","Eki","Kas","Ara"]

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return `${d.getDate()} ${TR_MONTHS[d.getMonth()]} ${d.getFullYear()}`
}

function formatAmount(amount: number): string {
  return `₺${amount.toLocaleString("tr-TR", { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

interface TxRow {
  id: string
  type: "credit" | "debit"
  description: string
  date: string
  category: string
  amount: string
  status: string
}

export function TransactionsTable() {
  const [rows, setRows] = useState<TxRow[]>([])
  const [loading, setLoading] = useState(true)

  const cardBg = useColorModeValue("#ffffff", "#111111")
  const borderColor = useColorModeValue("#e4e4e7", "#262626")
  const textPrimary = useColorModeValue("#09090b", "#fafafa")
  const textMuted = useColorModeValue("#71717a", "#a1a1aa")
  const headerBg = useColorModeValue("#fafafa", "#0d0d0d")
  const hoverBg = useColorModeValue("#fafafa", "#161616")
  const positive = useColorModeValue("#16a34a", "#4ade80")
  const negative = useColorModeValue("#dc2626", "#f87171")
  const badgeBg = useColorModeValue("#f4f4f5", "#1a1a1a")
  const badgeColor = useColorModeValue("#52525b", "#a1a1aa")

  useEffect(() => {
    async function load() {
      try {
        const res = await api.get("/api/transactions")
        const txs: any[] = res.data.slice(0, 5)
        setRows(
          txs.map((t) => ({
            id: `#${String(t.transactionId).padStart(4, "0")}`,
            type: t.transactionType === "INCOME" ? "credit" : "debit",
            description: t.description || (t.transactionType === "INCOME" ? "Gelir işlemi" : "Gider işlemi"),
            date: formatDate(t.transactionDate),
            category: t.category || "Genel",
            amount: formatAmount(t.amount),
            status: "Tamamlandı",
          }))
        )
      } catch (err) {
        console.error("TransactionsTable fetch error:", err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return <Skeleton height="280px" borderRadius="lg" />
  }

  return (
    <Box bg={cardBg} border="1px solid" borderColor={borderColor} borderRadius="lg" overflow="hidden">
      <Flex justify="space-between" align="center" px="5" py="4">
        <Box>
          <Text fontSize="sm" fontWeight="600" color={textPrimary}>Son İşlemler</Text>
          <Text fontSize="xs" color={textMuted}>En son 5 işlem</Text>
        </Box>
        <Text fontSize="xs" fontWeight="500" color={textMuted} cursor="pointer" _hover={{ color: textPrimary }} transition="color 0.15s ease">
          Tümünü Gör
        </Text>
      </Flex>

      {rows.length === 0 ? (
        <Flex justify="center" align="center" height="120px">
          <Text fontSize="sm" color={textMuted}>Henüz işlem bulunmuyor</Text>
        </Flex>
      ) : (
        <Box overflowX="auto">
          <Table.Root size="sm">
            <Table.Header>
              <Table.Row bg={headerBg}>
                {[
                  { label: "İşlem", end: false },
                  { label: "Tarih", end: false },
                  { label: "Kategori", end: false },
                  { label: "Tutar", end: true },
                  { label: "Durum", end: true },
                ].map(({ label, end }) => (
                  <Table.ColumnHeader
                    key={label}
                    py="2.5" px="5"
                    fontSize="xs" fontWeight="500" color={textMuted}
                    textTransform="uppercase" letterSpacing="0.04em"
                    borderBottom="1px solid" borderColor={borderColor}
                    textAlign={end ? "end" : "start"}
                  >
                    {label}
                  </Table.ColumnHeader>
                ))}
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {rows.map((tx) => (
                <Table.Row key={tx.id} _hover={{ bg: hoverBg }} transition="background 0.1s ease" cursor="pointer">
                  <Table.Cell py="3" px="5" borderBottom="1px solid" borderColor={borderColor}>
                    <Flex align="center" gap="3">
                      <Flex align="center" justify="center" w="7" h="7" borderRadius="md" bg={badgeBg} flexShrink={0}>
                        {tx.type === "credit"
                          ? <ArrowUpRight size={14} color={positive} />
                          : <ArrowDownRight size={14} color={negative} />}
                      </Flex>
                      <Box>
                        <Text fontSize="sm" fontWeight="500" color={textPrimary} lineClamp={1}>{tx.description}</Text>
                        <Text fontSize="xs" color={textMuted}>{tx.id}</Text>
                      </Box>
                    </Flex>
                  </Table.Cell>
                  <Table.Cell py="3" px="5" fontSize="xs" color={textMuted} className="tabular-nums" borderBottom="1px solid" borderColor={borderColor} whiteSpace="nowrap">
                    {tx.date}
                  </Table.Cell>
                  <Table.Cell py="3" px="5" fontSize="xs" color={textMuted} borderBottom="1px solid" borderColor={borderColor}>
                    {tx.category}
                  </Table.Cell>
                  <Table.Cell py="3" px="5" fontSize="sm" fontWeight="500" textAlign="end" className="tabular-nums" color={tx.type === "credit" ? positive : textPrimary} borderBottom="1px solid" borderColor={borderColor} whiteSpace="nowrap">
                    {tx.type === "credit" ? "+" : "-"}{tx.amount}
                  </Table.Cell>
                  <Table.Cell py="3" px="5" textAlign="end" borderBottom="1px solid" borderColor={borderColor}>
                    <Box as="span" display="inline-block" px="2" py="0.5" borderRadius="full" fontSize="xs" fontWeight="500" bg={badgeBg} color={badgeColor}>
                      {tx.status}
                    </Box>
                  </Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </Box>
      )}
    </Box>
  )
}
