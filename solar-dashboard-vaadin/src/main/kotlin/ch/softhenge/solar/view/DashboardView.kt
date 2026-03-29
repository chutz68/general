package ch.softhenge.solar.view

import ch.softhenge.solar.service.DailyData
import ch.softhenge.solar.service.SolarService
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
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

        // Title
        add(H1("☀️ Solar Dashboard"))

        // Summary cards for today
        val todayData = data.firstOrNull()
        if (todayData != null) {
            add(H2("Today – ${todayData.day}"))
            add(buildSummaryCards(todayData))
        }

        // 30-day table
        add(H2("Last 30 Days"))
        add(buildGrid(data))
    }

    private fun buildSummaryCards(data: DailyData): HorizontalLayout {
        val layout = HorizontalLayout()
        layout.setWidthFull()
        layout.add(
            card("Production",       "${(data.pWh / 1000).format(2)} kWh"),
            card("Consumption",      "${(data.cWh / 1000).format(2)} kWh"),
            card("Export",           "${(data.eWh / 1000).format(2)} kWh"),
            card("Import",           "${(data.iWh / 1000).format(2)} kWh"),
            card("Self Consumption", "${data.selfConsumptionPct?.format(2) ?: "-"} %"),
            card("Autarky",          "${data.autarkyPct?.format(2) ?: "-"} %"),
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
