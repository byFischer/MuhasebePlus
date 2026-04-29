import { Box, Flex } from "@chakra-ui/react";
import { RevenueChart } from "../RevenueChart";
import { CashFlowChart } from "../CashFlowChart";

export default function TimeSeriesWidget({ config }) {
  const chartType = config?.chartType ?? "REVENUE";
  return (
    <Box p="4">
      {chartType === "CASHFLOW" ? <CashFlowChart /> : <RevenueChart />}
    </Box>
  );
}
