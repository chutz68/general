package ch.softhenge.solar.view

import ch.softhenge.solar.service.DailyData
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.service.TodaySums
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
import ch.softhenge.solar.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Route("")
@PageTitle("Solar Dashboard")
class DashboardView(private val solarService: SolarService) : VerticalLayout() {

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val today = TimeUtils.today()
        val thirtyDaysAgo = today.minusDays(30)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val data = solarService.getDailyData(
            thirtyDaysAgo.format(formatter),
            today.format(formatter)
        )

        val nav = HorizontalLayout(
            H1(t("dashboard.title")),
            RouterLink(t("dashboard.nav.flow"), EnergyFlowView::class.java),
            RouterLink(t("dashboard.nav.live"), LiveView::class.java)
        ).apply { setAlignItems(Alignment.BASELINE); setSpacing(true) }
        add(nav)

        val todaySums = solarService.getTodaySums()
        val current = solarService.getCurrentData()
        val lastTime = current?.let { TimeUtils.formatDateTime(it.t) } ?: today.format(formatter)
        add(H2("${t("dashboard.today")} – $lastTime (${TimeUtils.zoneLabel()})"))
        add(buildSummaryCards(todaySums))

        add(H2(t("dashboard.last30days")))
        add(buildGrid(data))
    }

    private fun t(key: String) = getTranslation(key)

    private fun buildSummaryCards(sums: TodaySums): HorizontalLayout {
        val pWh = sums.pWh
        val cWh = sums.cWh
        val selfConsumptionPct = if (pWh > 0) (pWh - sums.eWh) / pWh * 100 else null
        val autarkyPct         = if (cWh > 0) (cWh - sums.iWh) / cWh * 100 else null

        val layout = HorizontalLayout()
        layout.setWidthFull()
        layout.add(
            card(t("dashboard.card.production"), "${(pWh / 1000).format(2)} kWh"),
            card(t("dashboard.card.consumption"), "${(cWh / 1000).format(2)} kWh"),
            card(t("dashboard.card.export"),      "${(sums.eWh / 1000).format(2)} kWh"),
            card(t("dashboard.card.import"),      "${(sums.iWh / 1000).format(2)} kWh"),
            card(t("dashboard.card.selfcons"),    "${selfConsumptionPct?.format(2) ?: "-"} %"),
            card(t("dashboard.card.autarky"),     "${autarkyPct?.format(2) ?: "-"} %"),
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

    private fun Double.format(decimals: Int) = "%.${decimals}f".format(this)
}
