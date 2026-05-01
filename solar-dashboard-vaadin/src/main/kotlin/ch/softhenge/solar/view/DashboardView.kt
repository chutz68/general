package ch.softhenge.solar.view

import ch.softhenge.solar.service.DailyData
import ch.softhenge.solar.service.MonthlyDayData
import ch.softhenge.solar.service.MonthlyTotals
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.service.TodaySums
import ch.softhenge.solar.util.TimeUtils
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Route("data")
@PageTitle("Solar Dashboard")
class DashboardView(private val solarService: SolarService) : VerticalLayout() {

    private val monthCardsLayout = HorizontalLayout().apply { setWidthFull(); isSpacing = true }
    private val monthGridContainer = VerticalLayout().apply { setPadding(false); setSpacing(false); setWidthFull() }
    private val monthLabel = H2()
    private var currentMonth: YearMonth = YearMonth.from(TimeUtils.today())

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = TimeUtils.today()

        val nav = HorizontalLayout(
            H1(t("dashboard.title")),
            RouterLink(t("dashboard.nav.flow"), EnergyFlowView::class.java),
            RouterLink(t("dashboard.nav.live"), LiveView::class.java)
        ).apply { setAlignItems(Alignment.BASELINE); setSpacing(true) }
        add(nav)

        // --- Today ---
        val todaySums = solarService.getTodaySums()
        val current = solarService.getCurrentData()
        val lastTime = current?.let { TimeUtils.formatDateTime(it.t) } ?: today.format(formatter)
        add(H2("${t("dashboard.today")} – $lastTime (${TimeUtils.zoneLabel()})"))
        add(buildTodayCards(todaySums))

        // --- Last 30 days ---
        add(H2(t("dashboard.last30days")))
        val dailyData = solarService.getDailyData(today.minusDays(30).format(formatter), today.format(formatter))
        add(buildDailyGrid(dailyData))

        // --- Monthly ---
        add(buildMonthToolbar())
        add(monthLabel)
        add(monthCardsLayout)
        add(monthGridContainer)

        loadMonthData()
    }

    private fun t(key: String) = getTranslation(key)

    // -------------------- Today section --------------------

    private fun buildTodayCards(sums: TodaySums): HorizontalLayout {
        val pWh = sums.pWh
        val cWh = sums.cWh
        val selfConsumptionPct = if (pWh > 0) (pWh - sums.eWh) / pWh * 100 else null
        val autarkyPct         = if (cWh > 0) (cWh - sums.iWh) / cWh * 100 else null
        return HorizontalLayout().apply {
            setWidthFull()
            add(
                card(t("dashboard.card.production"),  "${(pWh / 1000).format(2)} kWh"),
                card(t("dashboard.card.consumption"), "${(cWh / 1000).format(2)} kWh"),
                card(t("dashboard.card.export"),      "${(sums.eWh / 1000).format(2)} kWh"),
                card(t("dashboard.card.import"),      "${(sums.iWh / 1000).format(2)} kWh"),
                card(t("dashboard.card.selfcons"),    "${selfConsumptionPct?.format(2) ?: "-"} %"),
                card(t("dashboard.card.autarky"),     "${autarkyPct?.format(2) ?: "-"} %"),
            )
        }
    }

    private fun card(label: String, value: String): VerticalLayout {
        val card = VerticalLayout()
        card.add(Span(label).also { it.style.set("font-size", "0.85em").set("color", "var(--lumo-secondary-text-color)") })
        card.add(Span(value).also { it.style.set("font-size", "1.5em").set("font-weight", "bold") })
        card.style
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "8px")
            .set("padding", "1em")
            .set("min-width", "130px")
        return card
    }

    private fun buildDailyGrid(data: List<DailyData>): Grid<DailyData> {
        val grid = Grid(DailyData::class.java, false)
        grid.setWidthFull()
        grid.addColumn { it.day }.setHeader(t("dashboard.col.day")).setSortable(true)
        grid.addColumn { (it.pWh / 1000).format(2) }.setHeader(t("dashboard.col.production"))
        grid.addColumn { (it.cWh / 1000).format(2) }.setHeader(t("dashboard.col.consumption"))
        grid.addColumn { (it.eWh / 1000).format(2) }.setHeader(t("dashboard.col.export"))
        grid.addColumn { (it.iWh / 1000).format(2) }.setHeader(t("dashboard.col.import"))
        grid.addColumn { it.selfConsumptionPct?.format(2) ?: "-" }.setHeader(t("dashboard.col.selfcons"))
        grid.addColumn { it.autarkyPct?.format(2) ?: "-" }.setHeader(t("dashboard.col.autarky"))
        grid.addColumn { "${it.tempRealMin.format(2)} / ${it.tempRealMax.format(2)} °C" }.setHeader(t("dashboard.col.temp"))
        grid.addColumn { if (it.rowCount == 288) "OK" else "${it.missingRows} missing" }.setHeader(t("dashboard.col.quality"))
        grid.setItems(data)
        return grid
    }

    // -------------------- Monthly section --------------------

    private fun buildMonthToolbar(): HorizontalLayout {
        val prev    = Button("◀") { navigateMonth(-1) }
        val next    = Button("▶") { navigateMonth(1) }
        val current = Button(t("month.current")) {
            currentMonth = YearMonth.from(TimeUtils.today())
            loadMonthData()
        }
        return HorizontalLayout(prev, next, current).apply { alignItems = Alignment.BASELINE }
    }

    private fun navigateMonth(months: Long) {
        val target = currentMonth.plusMonths(months)
        if (!target.isAfter(YearMonth.from(TimeUtils.today()))) {
            currentMonth = target
            loadMonthData()
        }
    }

    private fun loadMonthData() {
        val fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val from = currentMonth.atDay(1)
        val to   = currentMonth.atEndOfMonth()

        val data   = solarService.getMonthlyData(from.format(fmt), to.format(fmt))
        val totals = solarService.summarize(data)

        monthLabel.text = "${t("month.heading")} – ${currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN))}"

        monthCardsLayout.removeAll()
        monthCardsLayout.add(*buildMonthCards(totals).toTypedArray())

        monthGridContainer.removeAll()
        monthGridContainer.add(buildMonthGrid(data))
    }

    private fun buildMonthCards(s: MonthlyTotals): List<VerticalLayout> {
        fun kwh(wh: Double) = "${"%.1f".format(wh / 1000)} kWh"
        fun mm(v: Double)   = "${"%.1f".format(v)} mm"
        fun pct(v: Double?) = v?.let { "${"%.1f".format(it)} %" } ?: "-"
        val sunshineH = s.sunshineMin / 60.0
        return listOf(
            monthCard(t("month.card.production"),  kwh(s.pWh),                                "#f6c90e"),
            monthCard(t("month.card.consumption"), kwh(s.cWh),                                "#3da4ab"),
            monthCard(t("month.card.charged"),     kwh(s.bcWh),                               "#2ecc71"),
            monthCard(t("month.card.discharged"),  kwh(s.bdWh),                               "#e74c3c"),
            monthCard(t("month.card.export"),      kwh(s.eWh),                                "#378ADD"),
            monthCard(t("month.card.import"),      kwh(s.iWh),                                "#D85A30"),
            monthCard(t("month.card.heatpump"),    kwh(s.pHpHeatingWh + s.pHpWarmwaterWh),    "#9b59b6"),
            monthCard(t("month.card.sunshine"),    "${"%.1f".format(sunshineH)} h",            "#f39c12"),
            monthCard(t("month.card.temp"),        tempRange(s.tempMin, s.tempMax),            "#e67e22"),
            monthCard(t("month.card.rain"),        mm(s.rainAmountSum),                        "#5dade2"),
            monthCard(t("month.card.snow"),        mm(s.snowAmountSum),                        "#aed6f1"),
            monthCard(t("month.card.selfcons"),    pct(s.avgSelfConsumptionPct),               "#16a085"),
            monthCard(t("month.card.autarky"),     pct(s.avgAutarkyPct),                       "#27ae60"),
        )
    }

    private fun monthCard(label: String, value: String, color: String): VerticalLayout {
        val card = VerticalLayout()
        card.setPadding(true)
        card.setSpacing(false)
        card.style
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-top", "3px solid $color")
            .set("border-radius", "8px")
            .set("min-width", "110px")
            .set("flex", "1")
        card.add(Span(label).also { it.style.set("font-size", "11px").set("color", "var(--lumo-secondary-text-color)") })
        card.add(Span(value).also { it.style.set("font-size", "16px").set("font-weight", "600") })
        return card
    }

    private fun buildMonthGrid(data: List<MonthlyDayData>): Grid<MonthlyDayData> {
        val grid = Grid(MonthlyDayData::class.java, false)
        grid.setWidthFull()
        grid.setAllRowsVisible(true)
        grid.addColumn { it.day }.setHeader(t("month.col.day")).setAutoWidth(true).setSortable(true)
        grid.addColumn { (it.pWh / 1000).format(2) }.setHeader(t("month.col.production")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { (it.cWh / 1000).format(2) }.setHeader(t("month.col.consumption")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { (it.bcWh / 1000).format(2) }.setHeader(t("month.col.charged")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { (it.bdWh / 1000).format(2) }.setHeader(t("month.col.discharged")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { (it.eWh / 1000).format(2) }.setHeader(t("month.col.export")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { (it.iWh / 1000).format(2) }.setHeader(t("month.col.import")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { ((it.pHpHeatingDayWh + it.pHpWarmwaterDayWh) / 1000).format(2) }.setHeader(t("month.col.heatpump")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { hoursMinutes(it.dayLengthMin) }.setHeader(t("month.col.daylength")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { hoursMinutes(it.sunshineMin) }.setHeader(t("month.col.sunshine")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { "${it.tempRealMin.format(1)} / ${it.tempRealMax.format(1)}" }.setHeader(t("month.col.temp")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { it.rainAmountSum.format(1) }.setHeader(t("month.col.rain")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { it.snowAmountSum.format(1) }.setHeader(t("month.col.snow")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { it.selfConsumptionPct?.format(1) ?: "-" }.setHeader(t("month.col.selfcons")).setTextAlign(ColumnTextAlign.END)
        grid.addColumn { it.autarkyPct?.format(1) ?: "-" }.setHeader(t("month.col.autarky")).setTextAlign(ColumnTextAlign.END)
        grid.setItems(data)
        return grid
    }

    private fun hoursMinutes(min: Int): String {
        if (min <= 0) return "-"
        return "%d:%02d".format(min / 60, min % 60)
    }

    private fun tempRange(min: Double?, max: Double?): String {
        if (min == null || max == null) return "-"
        return "%.1f / %.1f °C".format(min, max)
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}
