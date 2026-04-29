import { lazy } from "react";

const KpiWidget        = lazy(() => import("./KpiWidget"));
const TimeSeriesWidget = lazy(() => import("./TimeSeriesWidget"));
const ActivityFeedWidget = lazy(() => import("./ActivityFeedWidget"));
const AccountsWidget   = lazy(() => import("./AccountsWidget"));
const AiInsightWidget  = lazy(() => import("./AiInsightWidget"));
const MoodMeterWidget  = lazy(() => import("./MoodMeterWidget"));
const QuickActionsWidget = lazy(() => import("./QuickActionsWidget"));
const PinnedNoteWidget = lazy(() => import("./PinnedNoteWidget"));
const GenericDataWidget = lazy(() => import("./GenericDataWidget"));

const REGISTRY = {
  KPI:           { component: KpiWidget,          title: "KPI Kartları" },
  TIME_SERIES:   { component: TimeSeriesWidget,   title: "Zaman Serisi" },
  PIE:           { component: GenericDataWidget,  title: "Pasta Grafik" },
  TOP_N:         { component: GenericDataWidget,  title: "En İyi N" },
  ACTIVITY_FEED: { component: ActivityFeedWidget, title: "Son İşlemler" },
  HEATMAP:       { component: GenericDataWidget,  title: "Aktivite Haritası" },
  PROGRESS_GOAL: { component: GenericDataWidget,  title: "Hedef İlerlemesi" },
  FORECAST:      { component: GenericDataWidget,  title: "Tahmin" },
  AI_INSIGHT:    { component: AiInsightWidget,    title: "AI Analiz" },
  CONVERSATIONAL:{ component: AiInsightWidget,    title: "Sohbet Asistanı" },
  DAILY_BRIEF:   { component: AiInsightWidget,    title: "Günlük Özet" },
  ANOMALY:       { component: GenericDataWidget,  title: "Anomali Tespiti" },
  MOOD_METER:    { component: MoodMeterWidget,    title: "Finansal Nabız" },
  QUICK_ACTIONS: { component: QuickActionsWidget, title: "Hızlı Aksiyonlar" },
  PINNED_NOTE:   { component: PinnedNoteWidget,   title: "Notlar" },
  ACCOUNTS:      { component: AccountsWidget,     title: "Banka Hesapları" },
};

export function getWidgetMeta(type) {
  return REGISTRY[type] ?? { component: GenericDataWidget, title: type };
}

export default REGISTRY;
