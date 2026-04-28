import { Box, Flex } from "@chakra-ui/react";
import { Sidebar } from "../components/dashboard/Sidebar";
import { Header } from "../components/dashboard/Header";
import { KpiCards } from "../components/dashboard/KpiCards";
import { RevenueChart } from "../components/dashboard/RevenueChart";
import { CashFlowChart } from "../components/dashboard/CashFlowChart";
import { AccountsSummary } from "../components/dashboard/AccountsSummary";
import { TransactionsTable } from "../components/dashboard/TransactionsTable";
import { useColorModeValue } from "../components/ui/color-mode";

export default function DashboardPage() {
  const pageBg = useColorModeValue("#fafafa", "#0a0a0a");

  return (
    <Flex height="100dvh" overflow="hidden">
      <Sidebar />
      <Flex flex="1" direction="column" overflow="hidden" minW="0">
        <Header />
        <Box flex="1" overflowY="auto" bg={pageBg} p="6">
          <Flex direction="column" gap="6" maxW="1400px" mx="auto">
            <KpiCards />
            <Flex gap="4" direction={{ base: "column", xl: "row" }}>
              <RevenueChart />
              <CashFlowChart />
            </Flex>
            <AccountsSummary />
            <TransactionsTable />
          </Flex>
        </Box>
      </Flex>
    </Flex>
  );
}
