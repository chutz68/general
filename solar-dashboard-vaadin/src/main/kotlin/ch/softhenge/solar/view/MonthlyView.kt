package ch.softhenge.solar.view

import ch.softhenge.solar.service.MonthlyDayData
import ch.softhenge.solar.service.MonthlyTotals
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.util.TimeUtils
import com.vaadin.flow.component.button.Button
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

@Route("month")
@PageTitle("Solar Monthly View")
class MonthlyView(private val solarService: SolarService) : VerticalLayout() {

    private val cardsLayout = HorizontalLayout().apply {
        setWidthFull()
        isSpacing = true
    }
    private val gridContainer = VerticalLayout().apply {
        setPadding(false)
        setSpacing(false)
        setWidthFull()
    }
    private val monthLabel = H2()
    private var currentMonth: YearMonth = YearMonth.from(TimeUtils.today())

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val nav = HorizontalLayout(
            H1(t("month.title")),
            RouterLink(t("month.nav.dashboard"), DashboardView::class.java),
            RouterLink(t("month.nav.flow"),      EnergyFlowView::class.java),
            RouterLink(t("month.nav.live"),      LiveView::class.java)
        ).apply { alignItems = Alignment.BASELINE; setSpacing(true) }
        add(nav)

        add(buildToolbar())
        add(monthLabel)
        add(cardsLayout)
        add(gridContainer)

        loadData()
    }

    private fun t(key: String) = getTranslation(key)

    private fun buildToolbar(): HorizontalLayout {
        val prev    = Button("◀") { navigateMonth(-1) }
        val next    = Button("▶") { navigateMonth(1) }
        val current = Button(t("month.current")) {
            currentMonth = YearMonth.from(TimeUtils.today())
            loadData()
        }
        return HorizontalLayout(prev, next, current).apply {
            alignItems = Alignment.BASELINE
        }
    }

    private fun navigateMonth(months: Long) {
        val target = currentMonth.plusMonths(months)
        if (!target.isAfter(YearMonth.from(TimeUtils.today()))) {
            currentMonth = target
            loadData()
        }
    }

    private fun loadData() {
        val from = currentMonth.atDay(1)
        val to   = currentMonth.atEndOfMonth()
        val fmt  = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val data    = solarService.getMonthlyData(from.format(fmt), to.format(fmt))
        val totals  = solarService.summarize(data)

        val title   = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN))
        monthLabel.text = "${t("month.heading")} – $title"

        cardsLayout.removeAll()
        cardsLayout.add(*buildCards(totals).toTypedArray())

        gridContainer.removeAll()
        gridContainer.add(buildGrid(data))
    }

    // -------------------- KPI cards --------------------

    private fun buildCards(s: MonthlyTotals): List<VerticalLayout> {
        fun kwh(wh: Double) = "${"%.1f".format(wh / 1000)} kWh"
        fun mm(v: Double)   = "${"%.1f".format(v)} mm"
        fun pct(v: Double?) = v?.let { "${"%.1f".format(it)} %" } ?: "-"

        val sunshineH = s.sunshineMin / 60.0
        return listOf(
            card(t("month.card.production"),  kwh(s.pWh),                 "#f6c90e"),
            card(t("month.card.consumption"), kwh(s.cWh),                 "#3da4ab"),
            card(t("month.card.charged"),     kwh(s.bcWh),                "#2ecc71"),
            card(t("month.card.discharged"),  kwh(s.bdWh),                "#e74c3c"),
            card(t("month.card.export"),      kwh(s.eWh),                 "#378ADD"),
            card(t("month.card.import"),      kwh(s.iWh),                 "#D85A30"),
            card(t("month.card.heatpump"),    kwh(s.pHpHeatingWh + s.pHpWarmwaterWh), "#9b59b6"),
            card(t("month.card.sunshine"),    "${"%.1f".format(sunshineH)} h", "#f39c12"),
            card(t("month.card.temp"),        tempRange(s.tempMin, s.tempMax), "#e67e22"),
            card(t("month.card.rain"),        mm(s.rainAmountSum),        "#5dade2"),
            card(t("month.card.snow"),        mm(s.snowAmountSum),        "#aed6f1"),
            card(t("month.card.selfcons"),    pct(s.avgSelfConsumptionPct), "#16a085"),
            card(t("month.card.autarky"),     pct(s.avgAutarkyPct),       "#27ae60"),
        )
    }

    private fun card(label: String, value: String, color: String): VerticalLayout {
        val card = VerticalLayout()
        card.setPadding(true)
        card.setSpacing(false)
        card.style
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-top", "3px solid $color")
            .set("border-radius", "8px")
            .set("min-width", "110px")
            .set("flex", "1")
        val lbl = Span(label).also {
            it.style.set("font-size", "11px").set("color", "var(--lumo-secondary-text-color)")
        }
        val v = Span(value).also {
            it.style.set("font-size", "16px").set("font-weight", "600")
        }
        card.add(lbl, v)
        return card
    }

    // -------------------- Grid --------------------

    private fun buildGrid(data: List<MonthlyDayData>): Grid<MonthlyDayData> {
        val grid = Grid(MonthlyDayData::class.java, false)
        grid.setWidthFull()
        grid.setAllRowsVisible(true)

        grid.addColumn { it.day }.setHeader(t("month.col.day")).setAutoWidth(true).setSortable(true)
        grid.addColumn { (it.pWh / 1000).format(2) }.setHeader(t("month.col.production")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { (it.cWh / 1000).format(2) }.setHeader(t("month.col.consumption")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { (it.bcWh / 1000).format(2) }.setHeader(t("month.col.charged")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { (it.bdWh / 1000).format(2) }.setHeader(t("month.col.discharged")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { (it.eWh / 1000).format(2) }.setHeader(t("month.col.export")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { (it.iWh / 1000).format(2) }.setHeader(t("month.col.import")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { ((it.pHpHeatingDayWh + it.pHpWarmwaterDayWh) / 1000).format(2) }
            .setHeader(t("month.col.heatpump")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { hoursMinutes(it.dayLengthMin) }.setHeader(t("month.col.daylength")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { hoursMinutes(it.sunshineMin) }.setHeader(t("month.col.sunshine")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { "${it.tempRealMin.format(1)} / ${it.tempRealMax.format(1)}" }
            .setHeader(t("month.col.temp")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { it.rainAmountSum.format(1) }.setHeader(t("month.col.rain")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { it.snowAmountSum.format(1) }.setHeader(t("month.col.snow")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { it.selfConsumptionPct?.format(1) ?: "-" }.setHeader(t("month.col.selfcons")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)
        grid.addColumn { it.autarkyPct?.format(1) ?: "-" }.setHeader(t("month.col.autarky")).setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END)

        grid.setItems(data)
        return grid
    }

    private fun hoursMinutes(min: Int): String {
        if (min <= 0) return "-"
        val h = min / 60
        val m = min % 60
        return "%d:%02d".format(h, m)
    }

    private fun tempRange(min: Double?, max: Double?): String {
        if (min == null || max == null) return "-"
        return "%.1f / %.1f °C".format(min, max)
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}
