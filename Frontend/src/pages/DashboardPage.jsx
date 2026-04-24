import {
  ArrowDownIcon,
  ArrowUpIcon,
  AvatarIcon,
  BarChartIcon,
  BellIcon,
  CalendarIcon,
  ChevronDownIcon,
  ChevronRightIcon,
  ClockIcon,
  FileTextIcon,
  LightningBoltIcon,
  MagnifyingGlassIcon,
  PieChartIcon,
} from "@radix-ui/react-icons";
import { useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";

const navItems = ["Genel Bakış", "Faturalar", "Nakit Akışı", "Masraflar", "Stok", "Raporlar"];
const chartRangeOptions = ["1A", "3A", "6A", "YTD", "1Y"];

const dashboardBySection = {
  "Genel Bakış": {
    metricLabel: "Net Kâr (Bu Ay)",
    totalValue: "₺4.815.250",
    totalDelta: "%5.7",
    totalTrend: "up",
    healthScore: 76,
    chartTitle: "Aylık Ciro Trendi",
    chartTotal: "₺54.815.250",
    chartDelta: "%2.4",
    chartTrend: "down",
    rangeSeries: {
      "1A": [412000, 426000, 438000, 444000, 452000, 461000, 472000, 468000, 479000, 488000, 495000, 508000],
      "3A": [398000, 406000, 414000, 438000, 446000, 472000, 489000, 496000, 504000, 519000, 531000, 544000],
      "6A": [368000, 381000, 395000, 422000, 448000, 472000, 498000, 512000, 526000, 539000, 551000, 566000],
      YTD: [355000, 372000, 389000, 412000, 441000, 468000, 486000, 503000, 521000, 545000, 566000, 584000],
      "1Y": [332000, 341000, 356000, 379000, 402000, 435000, 458000, 482000, 505000, 531000, 558000, 586000],
    },
    financeCards: [
      { label: "Tahsilatlar", value: "₺1.248.400", accent: "teal" },
      { label: "Ödemeler", value: "₺684.120", accent: "orange" },
      { label: "Bekleyen Fatura", value: "₺214.780", accent: "violet" },
      { label: "KDV + SGK", value: "₺97.650", accent: "red" },
    ],
  },
  Faturalar: {
    metricLabel: "Toplam Fatura Tutarı",
    totalValue: "₺2.194.600",
    totalDelta: "%3.1",
    totalTrend: "up",
    healthScore: 73,
    chartTitle: "Fatura Tahsilat Hızı",
    chartTotal: "₺18.411.900",
    chartDelta: "%1.2",
    chartTrend: "up",
    rangeSeries: {
      "1A": [168000, 171000, 176000, 179000, 185000, 187000, 191000, 195000, 197000, 201000, 205000, 209000],
      "3A": [154000, 159000, 162000, 169000, 173000, 181000, 186000, 191000, 195000, 198000, 203000, 208000],
      "6A": [146000, 151000, 157000, 165000, 173000, 181000, 189000, 196000, 201000, 206000, 214000, 222000],
      YTD: [139000, 148000, 156000, 164000, 172000, 181000, 190000, 198000, 206000, 215000, 224000, 232000],
      "1Y": [122000, 131000, 142000, 154000, 168000, 176000, 187000, 199000, 212000, 224000, 237000, 249000],
    },
    financeCards: [
      { label: "Kesilen Fatura", value: "1.842", accent: "teal" },
      { label: "Açık Fatura", value: "326", accent: "orange" },
      { label: "Geciken", value: "84", accent: "red" },
      { label: "Ortalama Vade", value: "23 gün", accent: "violet" },
    ],
  },
  "Nakit Akışı": {
    metricLabel: "Nakit Akış Dengesi",
    totalValue: "₺1.106.420",
    totalDelta: "%2.8",
    totalTrend: "up",
    healthScore: 79,
    chartTitle: "Nakit Giriş/Çıkış",
    chartTotal: "₺9.285.330",
    chartDelta: "%0.8",
    chartTrend: "down",
    rangeSeries: {
      "1A": [91000, 92000, 93200, 94500, 95800, 96400, 97300, 98700, 99400, 100200, 101500, 102100],
      "3A": [86000, 87800, 89200, 91500, 93800, 95200, 96800, 98500, 99900, 101100, 102500, 103900],
      "6A": [81000, 83500, 85800, 88400, 91400, 94800, 97200, 99600, 101500, 103400, 105300, 107100],
      YTD: [76000, 79200, 82600, 86100, 89600, 93200, 96800, 100400, 104200, 108000, 112500, 116100],
      "1Y": [71000, 74300, 78100, 82800, 87400, 92100, 97300, 102100, 108200, 113800, 119700, 124500],
    },
    financeCards: [
      { label: "Nakit Girişi", value: "₺724.500", accent: "teal" },
      { label: "Nakit Çıkışı", value: "₺512.300", accent: "orange" },
      { label: "Bekleyen Tahsilat", value: "₺308.120", accent: "violet" },
      { label: "Kasa Bakiyesi", value: "₺96.410", accent: "red" },
    ],
  },
  Masraflar: {
    metricLabel: "Toplam Masraf (Bu Ay)",
    totalValue: "₺684.120",
    totalDelta: "%1.7",
    totalTrend: "down",
    healthScore: 70,
    chartTitle: "Masraf Trendi",
    chartTotal: "₺5.441.200",
    chartDelta: "%1.9",
    chartTrend: "up",
    rangeSeries: {
      "1A": [52000, 52600, 53200, 54000, 54800, 55600, 56100, 56800, 57400, 58100, 58900, 59500],
      "3A": [48600, 49400, 50200, 51400, 52800, 53900, 54800, 55600, 56600, 57800, 58700, 59800],
      "6A": [45200, 46800, 48300, 50100, 51900, 53800, 55600, 57200, 58600, 60200, 61700, 63400],
      YTD: [42100, 43700, 45500, 47800, 50100, 52700, 55300, 57800, 60400, 63100, 65900, 68800],
      "1Y": [39200, 40900, 42800, 45200, 47700, 50500, 53400, 56300, 59300, 62500, 65800, 69100],
    },
    financeCards: [
      { label: "Personel", value: "₺244.800", accent: "teal" },
      { label: "Operasyon", value: "₺182.400", accent: "orange" },
      { label: "Abonelik", value: "₺78.920", accent: "violet" },
      { label: "Diğer", value: "₺57.400", accent: "red" },
    ],
  },
  Stok: {
    metricLabel: "Stok Değeri",
    totalValue: "₺1.392.450",
    totalDelta: "%4.4",
    totalTrend: "up",
    healthScore: 74,
    chartTitle: "Stok Devir Eğrisi",
    chartTotal: "₺12.084.000",
    chartDelta: "%0.6",
    chartTrend: "up",
    rangeSeries: {
      "1A": [104000, 106000, 109500, 111000, 113500, 116000, 118000, 120000, 121500, 123000, 124500, 126200],
      "3A": [99500, 101500, 104000, 107000, 109500, 112300, 115100, 117800, 120500, 123100, 125500, 128000],
      "6A": [94000, 96800, 99500, 102800, 106200, 110000, 113800, 117000, 121000, 124500, 128000, 131500],
      YTD: [88200, 91500, 95300, 99500, 104200, 108800, 113700, 118500, 123900, 129300, 135000, 140500],
      "1Y": [82500, 86100, 90500, 95400, 100100, 106300, 112600, 119400, 126800, 134700, 143100, 151600],
    },
    financeCards: [
      { label: "Kritik Ürün", value: "18", accent: "red" },
      { label: "Siparişte", value: "64", accent: "orange" },
      { label: "Hızlı Dönen", value: "214", accent: "teal" },
      { label: "Stok Yaşı", value: "29 gün", accent: "violet" },
    ],
  },
  Raporlar: {
    metricLabel: "Raporlanan Toplam",
    totalValue: "₺7.561.880",
    totalDelta: "%2.1",
    totalTrend: "up",
    healthScore: 81,
    chartTitle: "Rapor Performansı",
    chartTotal: "₺41.280.300",
    chartDelta: "%1.1",
    chartTrend: "up",
    rangeSeries: {
      "1A": [345000, 349000, 352000, 358000, 361000, 365000, 368000, 371000, 374000, 378000, 383000, 387000],
      "3A": [326000, 331000, 337000, 343000, 349000, 355000, 362000, 369000, 376000, 384000, 392000, 401000],
      "6A": [309000, 316000, 324000, 334000, 345000, 356000, 368000, 381000, 395000, 410000, 426000, 442000],
      YTD: [291000, 300000, 310000, 322000, 334000, 348000, 362000, 377000, 393000, 410000, 428000, 447000],
      "1Y": [270000, 281000, 294000, 309000, 325000, 342000, 360000, 379000, 399000, 420000, 442000, 466000],
    },
    financeCards: [
      { label: "Aylık Rapor", value: "24", accent: "teal" },
      { label: "Hazır Şablon", value: "13", accent: "orange" },
      { label: "Bekleyen Onay", value: "7", accent: "violet" },
      { label: "Denetim Notu", value: "2", accent: "red" },
    ],
  },
};

const transactionFeed = [
  { company: "Yıldız Lojistik", value: "₺84.220", delta: "+4.2%", trend: "up" },
  { company: "Marmara Yazılım", value: "₺42.770", delta: "-1.4%", trend: "down" },
  { company: "Ares Sağlık", value: "₺63.500", delta: "+2.8%", trend: "up" },
  { company: "Ekin Gıda", value: "₺28.190", delta: "+1.2%", trend: "up" },
  { company: "Nova Endüstri", value: "₺18.820", delta: "-0.9%", trend: "down" },
  { company: "Atlas Enerji", value: "₺71.450", delta: "+3.4%", trend: "up" },
  { company: "Tuna Tekstil", value: "₺23.040", delta: "+0.7%", trend: "up" },
  { company: "Pusula Kimya", value: "₺39.990", delta: "-1.1%", trend: "down" },
];

const incomeDistribution = [
  { label: "Hizmet", percent: 82, tone: "orange" },
  { label: "Ürün", percent: 65, tone: "yellow" },
  { label: "Abonelik", percent: 48, tone: "light" },
  { label: "Diğer", percent: 30, tone: "charcoal" },
];

const expenseDistribution = [
  { label: "Personel", percent: 70, tone: "orange" },
  { label: "Kira", percent: 52, tone: "yellow" },
  { label: "Vergi", percent: 44, tone: "light" },
  { label: "Operasyon", percent: 34, tone: "charcoal" },
];

const insightItems = [
  "Nakit girişleri geçen aya göre %8.1 arttı.",
  "Önümüzdeki 7 gün içinde 12 adet fatura vadesi doluyor.",
  "Personel maliyet oranı hedefin %2.3 altında seyrediyor.",
];

const chartLabels = ["Ocak", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"];

const chartWidth = 760;
const chartHeight = 260;
const chartTop = 36;
const chartBottom = 228;
const chartLeft = 20;
const chartRight = 730;

function formatCurrency(value) {
  return value.toLocaleString("tr-TR", {
    style: "currency",
    currency: "TRY",
    maximumFractionDigits: 0,
  });
}

function buildChartGeometry(values) {
  const safeValues = values.length > 0 ? values : [0];
  const minValue = Math.min(...safeValues);
  const maxValue = Math.max(...safeValues);
  const valueRange = Math.max(maxValue - minValue, 1);
  const stepX = (chartRight - chartLeft) / Math.max(safeValues.length - 1, 1);

  const points = safeValues.map((value, index) => {
    const x = chartLeft + stepX * index;
    const y = chartBottom - ((value - minValue) / valueRange) * (chartBottom - chartTop);
    return {
      x,
      y,
      value,
      label: chartLabels[index] ?? `P${index + 1}`,
    };
  });

  const polyline = points.map((point) => `${point.x},${point.y}`).join(" ");
  const area = `${points
    .map((point, index) => `${index === 0 ? "M" : "L"}${point.x},${point.y}`)
    .join(" ")} L${chartRight},${chartBottom} L${chartLeft},${chartBottom} Z`;

  return { points, polyline, area };
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [activeNav, setActiveNav] = useState(navItems[0]);
  const [activeRange, setActiveRange] = useState(chartRangeOptions[4]);
  const [activeTool, setActiveTool] = useState("search");
  const [showAllTransactions, setShowAllTransactions] = useState(false);
  const [allocationMode, setAllocationMode] = useState("gelir");
  const [showRiskDetails, setShowRiskDetails] = useState(false);
  const [hoverIndex, setHoverIndex] = useState(null);

  const section = dashboardBySection[activeNav];

  const chart = useMemo(
    () => buildChartGeometry(section.rangeSeries[activeRange] ?? []),
    [section, activeRange]
  );

  const activePointIndex = hoverIndex === null ? Math.max(chart.points.length - 1, 0) : hoverIndex;
  const activePoint = chart.points[activePointIndex] ?? { x: chartRight, y: chartBottom, value: 0, label: "-" };

  const previousPoint = chart.points[Math.max(activePointIndex - 1, 0)] ?? activePoint;
  const valueDelta = previousPoint.value === 0 ? 0 : ((activePoint.value - previousPoint.value) / previousPoint.value) * 100;
  const valueDeltaLabel = `${valueDelta >= 0 ? "+" : ""}${valueDelta.toFixed(2)}%`;

  const tooltipLeft = Math.min(Math.max((activePoint.x / chartWidth) * 100, 12), 88);
  const tooltipTop = Math.min(Math.max((activePoint.y / chartHeight) * 100, 18), 78);

  const visibleTransactions = showAllTransactions ? transactionFeed : transactionFeed.slice(0, 4);
  const distributionItems = allocationMode === "gelir" ? incomeDistribution : expenseDistribution;

  const onChartMove = (event) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const relativeX = event.clientX - rect.left;
    const ratio = Math.min(Math.max(relativeX / rect.width, 0), 1);
    const index = Math.round(ratio * Math.max(chart.points.length - 1, 0));
    setHoverIndex(index);
  };

  return (
    <main className="dash-page">
      <section className="dash-shell">
        <header className="dash-topbar">
          <div className="dash-brand-wrap">
            <div className="dash-logo" aria-hidden>
              <span className="dash-logo-bar" />
              <span className="dash-logo-dot" />
            </div>
            <strong>MuhasebePlus</strong>
          </div>

          <nav className="dash-nav" aria-label="Ana menü">
            {navItems.map((item) => (
              <button
                key={item}
                type="button"
                className={`dash-nav-item ${activeNav === item ? "is-active" : ""}`}
                onClick={() => {
                  setActiveNav(item);
                  setHoverIndex(null);
                }}
              >
                {item}
              </button>
            ))}
          </nav>

          <div className="dash-actions">
            <button
              type="button"
              className={`dash-icon-button ${activeTool === "search" ? "is-active" : ""}`}
              aria-label="Ara"
              onClick={() => setActiveTool("search")}
            >
              <MagnifyingGlassIcon />
            </button>
            <button
              type="button"
              className={`dash-icon-button ${activeTool === "notifications" ? "is-active" : ""}`}
              aria-label="Bildirimler"
              onClick={() => setActiveTool("notifications")}
            >
              <BellIcon />
            </button>
            <button
              type="button"
              className={`dash-user-chip ${activeTool === "profile" ? "is-active" : ""}`}
              aria-label="Profil"
              onClick={() => setActiveTool("profile")}
            >
              <span className="dash-user-avatar" aria-hidden>
                <AvatarIcon />
              </span>
              {user?.companyName && (
                <span className="dash-company-name">{user.companyName}</span>
              )}
              <ChevronDownIcon />
            </button>
          </div>
        </header>

        <section className="dash-panel dash-performance">
          <div className="dash-panel-head">
            <h1>{activeNav} Performansı</h1>
            <div className="dash-health-chip">
              <LightningBoltIcon />
              <span>Sağlık Skoru</span>
              <strong>{section.healthScore}</strong>
            </div>
          </div>

          <div className="dash-performance-grid">
            <div className="dash-profit-column">
              <p className="dash-subtle">{section.metricLabel}</p>
              <div className="dash-total-line">
                <strong>{section.totalValue}</strong>
                <span className={`dash-pill ${section.totalTrend === "up" ? "positive" : "negative-soft"}`}>
                  {section.totalTrend === "up" ? <ArrowUpIcon /> : <ArrowDownIcon />}
                  {section.totalDelta}
                </span>
              </div>

              <div className="dash-kpi-grid">
                {section.financeCards.map((card) => (
                  <article key={card.label} className="dash-kpi-card">
                    <span className={`dash-kpi-dot ${card.accent}`} aria-hidden />
                    <p>{card.label}</p>
                    <strong>{card.value}</strong>
                  </article>
                ))}
              </div>
            </div>

            <article className="dash-revenue-panel">
              <div className="dash-revenue-head">
                <div>
                  <p>{section.chartTitle}</p>
                  <strong>{section.chartTotal}</strong>
                </div>
                <span className={`dash-pill ${section.chartTrend === "up" ? "positive" : "negative"}`}>
                  {section.chartTrend === "up" ? <ArrowUpIcon /> : <ArrowDownIcon />}
                  {section.chartDelta}
                </span>
              </div>

              <div className="dash-range-group" role="group" aria-label="Grafik aralığı">
                {chartRangeOptions.map((range) => (
                  <button
                    key={range}
                    type="button"
                    className={`dash-range-button ${activeRange === range ? "is-active" : ""}`}
                    onClick={() => {
                      setActiveRange(range);
                      setHoverIndex(null);
                    }}
                  >
                    {range}
                  </button>
                ))}
              </div>

              <div
                className="dash-chart-wrap"
                onMouseMove={onChartMove}
                onMouseLeave={() => setHoverIndex(null)}
                aria-label="Finans grafiği"
              >
                <div className="dash-chart-gridlines" aria-hidden>
                  {chartLabels.map((label) => (
                    <span key={label}>{label}</span>
                  ))}
                </div>

                <svg viewBox="0 0 760 260" className="dash-chart-svg" preserveAspectRatio="none" aria-hidden>
                  <defs>
                    <linearGradient id="chartArea" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="rgba(255, 255, 255, 0.35)" />
                      <stop offset="100%" stopColor="rgba(255, 255, 255, 0.02)" />
                    </linearGradient>
                  </defs>

                  <path className="dash-chart-area" d={chart.area} />
                  <polyline className="dash-chart-line" points={chart.polyline} />
                  <line
                    className="dash-chart-focus-line"
                    x1={activePoint.x}
                    y1={chartTop}
                    x2={activePoint.x}
                    y2={chartBottom}
                  />
                  <circle className="dash-chart-focus-dot" cx={activePoint.x} cy={activePoint.y} r="5.8" />
                </svg>

                <div className="dash-chart-tooltip" style={{ left: `${tooltipLeft}%`, top: `${tooltipTop}%` }}>
                  <p>{activePoint.label}</p>
                  <strong>{formatCurrency(activePoint.value)}</strong>
                  <small>{valueDeltaLabel}</small>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section className="dash-panel dash-flow-row" aria-label="Güncel hareketler">
          {visibleTransactions.map((item) => (
            <article key={item.company} className="dash-flow-item">
              <div className="dash-flow-meta">
                <p>{item.company}</p>
                <strong>{item.value}</strong>
              </div>
              <span className={`dash-flow-change ${item.trend === "up" ? "positive" : "negative"}`}>
                {item.trend === "up" ? <ArrowUpIcon /> : <ArrowDownIcon />}
                {item.delta}
              </span>
            </article>
          ))}
          <button
            type="button"
            className={`dash-flow-more ${showAllTransactions ? "is-active" : ""}`}
            aria-label="Daha fazla"
            onClick={() => setShowAllTransactions((current) => !current)}
          >
            {showAllTransactions ? "Daha Az" : "Daha Fazla"}
            <ChevronRightIcon />
          </button>
        </section>

        <section className="dash-bottom-grid">
          <article className="dash-panel dash-income-card">
            <div className="dash-card-title">
              <h2>{allocationMode === "gelir" ? "Gelir Dağılımı" : "Gider Dağılımı"}</h2>
              <button
                type="button"
                className={`dash-mini-chip ${allocationMode === "gider" ? "is-active" : ""}`}
                onClick={() =>
                  setAllocationMode((current) => (current === "gelir" ? "gider" : "gelir"))
                }
              >
                <PieChartIcon />
                {allocationMode === "gelir" ? "Gelir" : "Gider"}
              </button>
            </div>

            <div className="dash-distribution-grid">
              {distributionItems.map((item) => (
                <div key={item.label} className="dash-distribution-item">
                  <div className={`dash-distribution-track ${item.tone}`}>
                    <span style={{ height: `${item.percent}%` }} />
                  </div>
                  <strong>{item.percent}%</strong>
                  <p>{item.label}</p>
                </div>
              ))}
            </div>
          </article>

          <article className="dash-panel dash-risk-card">
            <div className="dash-card-title">
              <h2>Nakit Sağlığı</h2>
              <button
                type="button"
                className={`dash-mini-icon ${showRiskDetails ? "is-active" : ""}`}
                aria-label="Detaylar"
                onClick={() => setShowRiskDetails((current) => !current)}
              >
                <ChevronRightIcon />
              </button>
            </div>

            <div className="dash-risk-score">
              <strong>72</strong>
              <span>/100</span>
            </div>

            <div className="dash-gauge" style={{ "--score": 72 }}>
              <div className="dash-gauge-cap" />
            </div>

            <p className="dash-risk-note">
              {showRiskDetails
                ? "Kısa vadeli yükümlülükleri karşılama oranı %4 arttı, tahsilat temposu hızlandı."
                : "Tahsilat hızındaki artışla istikrar +%4 iyileşti."}
            </p>
          </article>

          <article className="dash-panel dash-insight-card">
            <div className="dash-card-title">
              <h2>Muhasebe İçgörüleri</h2>
              <button
                type="button"
                className={`dash-mini-icon ${showAllTransactions ? "is-active" : ""}`}
                aria-label="Aç"
                onClick={() => setShowAllTransactions((current) => !current)}
              >
                <ChevronRightIcon />
              </button>
            </div>

            <ul>
              {insightItems.map((text) => (
                <li key={text}>{text}</li>
              ))}
            </ul>

            <div className="dash-insight-meta">
              <span>
                <CalendarIcon />
                Son Güncelleme: Bugün
              </span>
              <span>
                <ClockIcon />
                16:40
              </span>
              <span>
                <FileTextIcon />
                3 kritik not
              </span>
              <span>
                <BarChartIcon />
                Haftalık rapora eklendi
              </span>
            </div>
          </article>
        </section>
      </section>
    </main>
  );
}
