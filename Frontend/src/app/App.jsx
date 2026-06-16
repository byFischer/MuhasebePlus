import { lazy, Suspense } from "react";
import { BrowserRouter, HashRouter, Routes, Route, Navigate } from "react-router-dom";
const Router = import.meta.env.VITE_ROUTER === 'hash' ? HashRouter : BrowserRouter;
import { Provider } from "@/components/ui/provider";
import { Toaster } from "@/components/ui/toaster";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/context/AuthContext";
import { getHomepagePath } from "@/lib/preferences";
import ProtectedRoute from "@/components/ProtectedRoute";
import AdminRoute from "@/components/AdminRoute";
import AppShell from "@/components/layout/AppShell";

import LoginPage from "@/pages/LoginPage";
import PasswordResetPage from "@/pages/PasswordResetPage";
import PublicSharePage from "@/pages/PublicSharePage";
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
import SettingsPage from "@/pages/settings/SettingsPage";
import ChekPage from "@/pages/ChekPage";
import HesapPlaniPage from "@/pages/HesapPlaniPage";
import YevmiyePage from "@/pages/YevmiyePage";
import DonemPage from "@/pages/DonemPage";
import BeyannamePage from "@/pages/beyanname/BeyannamePage";
import EDefterPage from "@/pages/edefter/EDefterPage";
import SabitKiymetPage from "@/pages/SabitKiymetPage";

// Admin sayfaları lazy yüklenir; normal kullanıcının bundle'ına dahil edilmez
const AdminDashboardPage = lazy(() => import("@/pages/admin/AdminDashboardPage"));
const AdminUsersPage     = lazy(() => import("@/pages/admin/AdminUsersPage"));
const AdminCompaniesPage = lazy(() => import("@/pages/admin/AdminCompaniesPage"));
const AdminLogsPage      = lazy(() => import("@/pages/admin/AdminLogsPage"));

function AdminPage({ children }) {
  return (
    <AdminRoute>
      <Suspense fallback={<div className="page"><div className="card" style={{ height: 200 }} /></div>}>
        {children}
      </Suspense>
    </AdminRoute>
  );
}

function HomeRedirect() {
  return <Navigate to={getHomepagePath()} replace />;
}

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
              <Route path="/paylasim/:token" element={<PublicSharePage />} />
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
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="/cek"         element={<ChekPage />} />
                <Route path="/hesap-plani" element={<HesapPlaniPage />} />
                <Route path="/yevmiye"     element={<YevmiyePage />} />
                <Route path="/donem"       element={<DonemPage />} />
                <Route path="/beyanname"   element={<BeyannamePage />} />
                <Route path="/edefter"     element={<EDefterPage />} />
                <Route path="/sabit-kiymet"   element={<SabitKiymetPage />} />
                <Route path="/admin"              element={<AdminPage><AdminDashboardPage /></AdminPage>} />
                <Route path="/admin/kullanicilar" element={<AdminPage><AdminUsersPage /></AdminPage>} />
                <Route path="/admin/sirketler"    element={<AdminPage><AdminCompaniesPage /></AdminPage>} />
                <Route path="/admin/loglar"       element={<AdminPage><AdminLogsPage /></AdminPage>} />
                <Route index element={<HomeRedirect />} />
              </Route>
              <Route path="*" element={<HomeRedirect />} />
            </Routes>
          </AuthProvider>
        </Router>
      </QueryClientProvider>
    </Provider>
  );
}

