import { BrowserRouter, HashRouter, Routes, Route, Navigate } from "react-router-dom";
const Router = import.meta.env.VITE_ROUTER === 'hash' ? HashRouter : BrowserRouter;
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
import InvoicePaymentPage from "@/pages/InvoicePaymentPage";
import StokPage from "@/pages/StokPage";
import GelirGiderPage from "@/pages/GelirGiderPage";
import BankaPage from "@/pages/BankaPage";
import SablonPage from "@/pages/SablonPage";
import RaporPage from "@/pages/RaporPage";
import BudgetPage from "@/pages/BudgetPage";
import LogPage from "@/pages/LogPage";
import WidgetBuilderPage from "@/pages/WidgetBuilderPage";
import WidgetManagerPage from "@/pages/WidgetManagerPage";
import SettingsPage from "@/pages/settings/SettingsPage";
import ChekPage from "@/pages/ChekPage";
import HesapPlaniPage from "@/pages/HesapPlaniPage";
import YevmiyePage from "@/pages/YevmiyePage";
import DonemPage from "@/pages/DonemPage";
import BeyannamePage from "@/pages/beyanname/BeyannamePage";
import EDefterPage from "@/pages/edefter/EDefterPage";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 } },
});

export default function App() {
  return (
    <Provider>
      <QueryClientProvider client={queryClient}>
        <Router>
          <AuthProvider>
            <Toaster />
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/forgot-password" element={<PasswordResetPage />} />
              <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
                <Route path="/dashboard"   element={<DashboardPage />} />
                <Route path="/cari"        element={<CariPage />} />
                <Route path="/fatura"      element={<FaturaPage />} />
                <Route path="/fatura/odemeler" element={<InvoicePaymentPage />} />
                <Route path="/stok"        element={<StokPage />} />
                <Route path="/gelir-gider" element={<GelirGiderPage />} />
                <Route path="/banka"       element={<BankaPage />} />
                <Route path="/sablon"      element={<SablonPage />} />
                <Route path="/rapor"       element={<RaporPage />} />
                <Route path="/butce"       element={<BudgetPage />} />
                <Route path="/log"         element={<LogPage />} />
                <Route path="/widget-builder" element={<WidgetBuilderPage />} />
                <Route path="/widgets" element={<WidgetManagerPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/cek"         element={<ChekPage />} />
                <Route path="/hesap-plani" element={<HesapPlaniPage />} />
                <Route path="/yevmiye"     element={<YevmiyePage />} />
                <Route path="/donem"       element={<DonemPage />} />
                <Route path="/beyanname"   element={<BeyannamePage />} />
                <Route path="/edefter"     element={<EDefterPage />} />
                <Route index element={<Navigate to="/dashboard" replace />} />
              </Route>
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </AuthProvider>
        </Router>
      </QueryClientProvider>
    </Provider>
  );
}

