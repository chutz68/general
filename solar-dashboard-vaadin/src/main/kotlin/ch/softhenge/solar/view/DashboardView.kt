package ch.softhenge.solar.view

import ch.softhenge.solar.service.DailyData
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.service.TodaySums
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Route("")
@PageTitle("Solar Dashboard")
class DashboardView(private val solarService: SolarService) : VerticalLayout() {

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val data = solarService.getDailyData(
            thirtyDaysAgo.format(formatter),
            today.format(formatter)
        )

        // Title + navigation
        val nav = HorizontalLayout(
            H1("☀️ Solar Dashboard"),
            RouterLink("⚡ Energiefluss", EnergyFlowView::class.java),
            RouterLink("📈 Live View", LiveView::class.java)
        ).apply { setAlignItems(Alignment.BASELINE); setSpacing(true) }
        add(nav)

        // Summary cards for today – from 5-min data (always current)
        val todaySums = solarService.getTodaySums()
        add(H2("Today – ${today.format(formatter)}"))
        add(buildSummaryCards(todaySums))

        // 30-day table
        add(H2("Last 30 Days"))
        add(buildGrid(data))
    }

    private fun buildSummaryCards(sums: TodaySums): HorizontalLayout {
        val pWh = sums.pWh
        val cWh = sums.cWh
        val selfConsumptionPct = if (pWh > 0) (pWh - sums.eWh) / pWh * 100 else null
        val autarkyPct         = if (cWh > 0) (cWh - sums.iWh) / cWh * 100 else null

        val layout = HorizontalLayout()
        layout.setWidthFull()
        layout.add(
            card("Production",       "${(pWh / 1000).format(2)} kWh"),
            card("Consumption",      "${(cWh / 1000).format(2)} kWh"),
            card("Export",           "${(sums.eWh / 1000).format(2)} kWh"),
            card("Import",           "${(sums.iWh / 1000).format(2)} kWh"),
            card("Self Consumption", "${selfConsumptionPct?.format(2) ?: "-"} %"),
            card("Autarky",          "${autarkyPct?.format(2) ?: "-"} %"),
        )
        return layout
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

    private fun buildGrid(data: List<DailyData>): Grid<DailyData> {
        val grid = Grid(DailyData::class.java, false)
        grid.setWidthFull()

        grid.addColumn { it.day }.setHeader("Day").setSortable(true)
        grid.addColumn { (it.pWh / 1000).format(2) }.setHeader("Production (kWh)")
        grid.addColumn { (it.cWh / 1000).format(2) }.setHeader("Consumption (kWh)")
        grid.addColumn { (it.eWh / 1000).format(2) }.setHeader("Export (kWh)")
        grid.addColumn { (it.iWh / 1000).format(2) }.setHeader("Import (kWh)")
        grid.addColumn { it.selfConsumptionPct?.format(2) ?: "-" }.setHeader("Self Cons. %")
        grid.addColumn { it.autarkyPct?.format(2) ?: "-" }.setHeader("Autarky %")
        grid.addColumn { "${it.tempRealMin.format(2)} / ${it.tempRealMax.format(2)} °C" }.setHeader("Temp Min/Max")
        grid.addColumn { if (it.rowCount == 288) "✅" else "⚠️ ${it.missingRows} missing" }.setHeader("Quality")

        grid.setItems(data)
        return grid
    }

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}
