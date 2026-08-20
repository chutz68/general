package ch.softhenge.solar.view

import ch.softhenge.solar.service.MonthlyDayData
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.util.TimeUtils
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.grid.ColumnTextAlign
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class RankedDay(val rank: Int, val day: String, val value: String)

@Route("topten", layout = MainLayout::class)
@PageTitle("Top 10")
class TopTenView(private val solarService: SolarService) : VerticalLayout() {

    private var periodMode = "month"
    private var periodStart: LocalDate = TimeUtils.today().withDayOfMonth(1)
    private val periodLabel = H3()
    private val tablesLayout = VerticalLayout().apply { setPadding(false); setSpacing(true); setWidthFull() }

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        add(buildToolbar())
        add(tablesLayout)

        loadData()
    }

    private fun buildToolbar(): VerticalLayout {
        val monthBtn = Button(getTranslation("topten.period.month")) {
            periodMode = "month"
            periodStart = TimeUtils.today().withDayOfMonth(1)
            it.source.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            loadData()
        }.also { it.addThemeVariants(ButtonVariant.LUMO_PRIMARY) }

        val yearBtn = Button(getTranslation("topten.period.year")) {
            periodMode = "year"
            periodStart = TimeUtils.today().withDayOfMonth(1).withMonth(1)
            monthBtn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY)
            it.source.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            loadData()
        }

        monthBtn.addClickListener {
            yearBtn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY)
        }

        val modeToggle = HorizontalLayout(monthBtn, yearBtn).apply { isSpacing = true }

        val prevBtn = Button("◀") { navigatePeriod(-1) }
        val nextBtn = Button("▶") { navigatePeriod(1) }

        val nav = HorizontalLayout(prevBtn, periodLabel, nextBtn).apply {
            alignItems = Alignment.CENTER
            isSpacing = true
        }

        return VerticalLayout(modeToggle, nav).apply {
            setPadding(false)
            setSpacing(true)
        }
    }

    private fun navigatePeriod(delta: Int) {
        val candidate = when (periodMode) {
            "year"  -> periodStart.plusYears(delta.toLong())
            else    -> periodStart.plusMonths(delta.toLong())
        }
        val limit = TimeUtils.today().withDayOfMonth(1).withMonth(1)
        if (candidate >= limit || delta < 0) {
            periodStart = candidate
            loadData()
        }
    }

    private fun loadData() {
        val (from, to) = periodRange()
        periodLabel.text = periodLabelText()
        val data = solarService.getMonthlyData(from.toString(), to.toString())
        renderTables(data)
    }

    private fun periodRange(): Pair<LocalDate, LocalDate> = when (periodMode) {
        "year" -> {
            val y = periodStart.year
            LocalDate.of(y, 1, 1) to LocalDate.of(y, 12, 31)
        }
        else -> periodStart to periodStart.plusMonths(1).minusDays(1)
    }

    private fun periodLabelText(): String {
        val fmt = if (periodMode == "year")
            DateTimeFormatter.ofPattern("yyyy")
        else
            DateTimeFormatter.ofPattern("MMMM yyyy")
        return periodStart.format(fmt)
    }

    private fun renderTables(data: List<MonthlyDayData>) {
        tablesLayout.removeAll()

        val topProduction = ranked(data.sortedByDescending { it.pWh }) {
            "%.1f kWh".format(it.pWh / 1000.0)
        }
        val topHottest = ranked(data.sortedByDescending { it.tempRealMax }) {
            "%.1f °C".format(it.tempRealMax)
        }
        val topRainiest = ranked(data.filter { it.rainAmountSum > 0 }.sortedByDescending { it.rainAmountSum }) {
            "%.1f mm".format(it.rainAmountSum)
        }
        val topColdest = ranked(data.sortedBy { it.tempRealMin }) {
            "%.1f °C".format(it.tempRealMin)
        }
        val topSnow = ranked(data.filter { it.snowAmountSum > 0 }.sortedByDescending { it.snowAmountSum }) {
            "%.1f mm".format(it.snowAmountSum)
        }

        val row1 = HorizontalLayout(
            buildTableCard(getTranslation("topten.table.production"), topProduction),
            buildTableCard(getTranslation("topten.table.hottest"),    topHottest)
        ).apply { setWidthFull(); isSpacing = true }

        val row2 = HorizontalLayout(
            buildTableCard(getTranslation("topten.table.rainiest"),  topRainiest),
            buildTableCard(getTranslation("topten.table.coldest"),   topColdest)
        ).apply { setWidthFull(); isSpacing = true }

        val row3 = HorizontalLayout(
            buildTableCard(getTranslation("topten.table.snow"), topSnow),
        ).apply { setWidthFull(); isSpacing = true }

        tablesLayout.add(row1, row2, row3)
    }

    private fun ranked(sorted: List<MonthlyDayData>, fmt: (MonthlyDayData) -> String): List<RankedDay> =
        sorted.take(10).mapIndexed { i, d -> RankedDay(i + 1, d.day, fmt(d)) }

    private fun buildTableCard(title: String, rows: List<RankedDay>): VerticalLayout {
        val grid = Grid<RankedDay>().apply {
            addColumn(RankedDay::rank)
                .setHeader("#")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setAutoWidth(true)
                .setFlexGrow(0)
            addColumn(RankedDay::day)
                .setHeader(getTranslation("topten.col.day"))
                .setAutoWidth(true)
                .setFlexGrow(1)
            addColumn(RankedDay::value)
                .setHeader(getTranslation("topten.col.value"))
                .setTextAlign(ColumnTextAlign.END)
                .setAutoWidth(true)
                .setFlexGrow(0)
            setItems(rows)
            setAllRowsVisible(true)
            width = "100%"
        }

        return VerticalLayout(H3(title), grid).apply {
            setPadding(true)
            setSpacing(false)
            width = "50%"
            style.set("border", "1px solid var(--lumo-contrast-10pct)")
                 .set("border-radius", "var(--lumo-border-radius-m)")
        }
    }
}
