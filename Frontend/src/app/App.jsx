import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { Provider } from "@/components/ui/provider";
import { Toaster } from "@/components/ui/toaster";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/context/AuthContext";
import ProtectedRoute from "@/components/ProtectedRoute";
import AppShell from "@/components/layout/AppShell";

import LoginPage from "@/pages/LoginPage";
import PasswordResetPage from "@/pages/PasswordResetPage";
import DashboardPage from "@/pages/DashboardPage";
import CariPage from "@/pages/CariPage";
import FaturaPage from "@/pages/FaturaPage";
import StokPage from "@/pages/StokPage";
import GelirGiderPage from "@/pages/GelirGiderPage";
import BankaPage from "@/pages/BankaPage";
import SablonPage from "@/pages/SablonPage";
import RaporPage from "@/pages/RaporPage";
import BudgetPage from "@/pages/BudgetPage";
import LogPage from "@/pages/LogPage";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 } },
});

export default function App() {
  return (
    <Provider>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AuthProvider>
            <Toaster />
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/forgot-password" element={<PasswordResetPage />} />
              <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
                <Route path="/dashboard"   element={<DashboardPage />} />
                <Route path="/cari"        element={<CariPage />} />
                <Route path="/fatura"      element={<FaturaPage />} />
                <Route path="/stok"        element={<StokPage />} />
                <Route path="/gelir-gider" element={<GelirGiderPage />} />
                <Route path="/banka"       element={<BankaPage />} />
                <Route path="/sablon"      element={<SablonPage />} />
                <Route path="/rapor"       element={<RaporPage />} />
                <Route path="/butce"       element={<BudgetPage />} />
                <Route path="/log"         element={<LogPage />} />
                <Route index element={<Navigate to="/dashboard" replace />} />
              </Route>
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    </Provider>
  );
}
