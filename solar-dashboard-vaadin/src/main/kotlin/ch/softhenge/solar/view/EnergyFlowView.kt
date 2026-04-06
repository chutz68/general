package ch.softhenge.solar.view

import ch.softhenge.solar.service.CurrentData
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.service.TodaySums
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.dependency.JavaScript
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import ch.softhenge.solar.util.TimeUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Route("flow")
@PageTitle("Energiefluss")
@JavaScript("https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js")
class EnergyFlowView(private val solarService: SolarService) : VerticalLayout() {

    private val chartDiv   = Div()
    private val lastUpdate = Span()

    private val cardPWh  = summaryCard(getTranslation("flow.card.production"),  "#f6c90e")
    private val cardBcWh = summaryCard(getTranslation("flow.card.charged"),     "#2ecc71")
    private val cardBdWh = summaryCard(getTranslation("flow.card.discharged"),  "#e74c3c")
    private val cardScWh = summaryCard(getTranslation("flow.card.selfcons"),    "#9b59b6")
    private val cardEWh  = summaryCard(getTranslation("flow.card.export"),      "#378ADD")
    private val cardIWh  = summaryCard(getTranslation("flow.card.import"),      "#D85A30")

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val nav = HorizontalLayout(
            H1(getTranslation("flow.title")),
            RouterLink(getTranslation("flow.nav.dashboard"),  DashboardView::class.java),
            RouterLink(getTranslation("flow.nav.live"),       LiveView::class.java)
        ).apply { alignItems = Alignment.BASELINE; setSpacing(true) }
        add(nav)

        chartDiv.element.setAttribute("id", "flow-chart")
        chartDiv.setWidthFull()
        chartDiv.height = "420px"
        add(chartDiv)

        lastUpdate.style.set("font-size", "12px").set("color", "var(--lumo-secondary-text-color)")
        add(lastUpdate)

        val cards = HorizontalLayout(cardPWh, cardBcWh, cardBdWh, cardScWh, cardEWh, cardIWh)
        cards.setWidthFull()
        cards.isSpacing = true
        add(cards)

        loadData()
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        val ui = attachEvent.ui
        ui.setPollInterval(300_000)
        ui.addPollListener { loadData() }
    }

    private fun loadData() {
        val current = solarService.getCurrentData()
        val sums    = solarService.getTodaySums()
        if (current != null) {
            renderChart(current)
            updateSummaryCards(sums)
            lastUpdate.text = "${getTranslation("flow.lastupdate")}: ${TimeUtils.formatDateTime(current.t)} (${TimeUtils.zoneLabel()})"
        }
    }

    private fun summaryCard(label: String, color: String): VerticalLayout {
        val card = VerticalLayout()
        card.setPadding(true)
        card.setSpacing(false)
        card.style
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-top", "3px solid $color")
            .set("border-radius", "8px")
            .set("min-width", "100px")
            .set("flex", "1")
        val lbl = Span(label)
        lbl.style.set("font-size", "11px").set("color", "var(--lumo-secondary-text-color)")
        val value = Span("- kWh")
        value.style.set("font-size", "18px").set("font-weight", "600")
        card.add(lbl, value)
        return card
    }

    private fun updateCard(card: VerticalLayout, value: String) {
        val span = card.children.filter { it is Span }.toList().lastOrNull() as? Span
        span?.text = value
    }

    private fun updateSummaryCards(sums: TodaySums) {
        fun kwh(wh: Double) = "${"%.2f".format(wh / 1000)} kWh"
        updateCard(cardPWh,  kwh(sums.pWh))
        updateCard(cardBcWh, kwh(sums.bcWh))
        updateCard(cardBdWh, kwh(sums.bdWh))
        updateCard(cardScWh, kwh(sums.scWh))
        updateCard(cardEWh,  kwh(sums.eWh))
        updateCard(cardIWh,  kwh(sums.iWh))
    }

    private fun renderChart(d: CurrentData) {
        val pW  = d.pW
        val cW  = d.cW
        val soc = d.soc ?: 0.0

        val surplus   = pW - cW
        val solarCons = min(pW, cW)
        val solarBat  = if (surplus > 0) min(surplus, 3000.0 * (1 - soc / 100)) else 0.0
        val solarGrid = if (surplus > 0) max(0.0, surplus - solarBat) else 0.0
        val batCons   = if (surplus < 0) min(abs(surplus), 3000.0 * (soc / 100)) else 0.0
        val gridCons  = if (surplus < 0) max(0.0, abs(surplus) - batCons) else 0.0

        val gridNet   = solarGrid - gridCons
        val gridLabel = if (gridNet >= 0) getTranslation("flow.node.feedin") else getTranslation("flow.node.draw")

        val wSolarCons = max(1.0, solarCons / 300)
        val wSolarBat  = max(1.0, solarBat  / 300)
        val wSolarGrid = max(1.0, solarGrid / 300)
        val wBatCons   = max(1.0, batCons   / 300)
        val wGridCons  = max(1.0, gridCons  / 300)

        fun w(v: Double) = v.roundToInt()

        val solarName = "${getTranslation("flow.node.solar")}\\n${w(pW)} W"
        val batName   = "${getTranslation("flow.node.battery")}\\n${soc.roundToInt()}%"
        val gridName  = "${getTranslation("flow.node.grid")}\\n${w(abs(gridNet))} W\\n$gridLabel"
        val consName  = "${getTranslation("flow.node.consumption")}\\n${w(cW)} W"

        val links = buildString {
            if (solarCons > 0) append("""{ source: '$solarName', target: '$consName', value: ${w(solarCons)}, lineStyle: { color: '#E09A00', width: $wSolarCons } },""")
            if (solarBat  > 0) append("""{ source: '$solarName', target: '$batName',  value: ${w(solarBat)},  lineStyle: { color: '#1D9E75', width: $wSolarBat  } },""")
            if (solarGrid > 0) append("""{ source: '$solarName', target: '$gridName', value: ${w(solarGrid)}, lineStyle: { color: '#378ADD', width: $wSolarGrid } },""")
            if (batCons   > 0) append("""{ source: '$batName',   target: '$consName', value: ${w(batCons)},   lineStyle: { color: '#1D9E75', width: $wBatCons  } },""")
            if (gridCons  > 0) append("""{ source: '$gridName',  target: '$consName', value: ${w(gridCons)},  lineStyle: { color: '#D85A30', width: $wGridCons } },""")
        }

        val js = """
            (function() {
                var dom = document.getElementById('flow-chart');
                if (!dom) return;
                var chart = echarts.getInstanceByDom(dom) || echarts.init(dom);
                chart.setOption({
                    tooltip: { formatter: function(p) { return p.data.value ? p.data.value + ' W' : ''; } },
                    series: [{
                        type: 'graph',
                        layout: 'none',
                        symbolSize: [150, 70],
                        symbol: 'roundRect',
                        roam: false,
                        label: { show: true, fontSize: 13, fontWeight: '500', lineHeight: 18 },
                        edgeSymbol: ['none','arrow'],
                        edgeSymbolSize: 10,
                        edgeLabel: { show: true, fontSize: 12, formatter: function(p) { return p.data.value + ' W'; } },
                        data: [
                            { name: '$solarName', x: 340, y: 60,  itemStyle: { color: '#FFF3CC', borderColor: '#E09A00', borderWidth: 2 }, label: { color: '#633806' } },
                            { name: '$batName',   x: 80,  y: 230, itemStyle: { color: '#DCFAF0', borderColor: '#1D9E75', borderWidth: 2 }, label: { color: '#04342C' } },
                            { name: '$gridName',  x: 600, y: 230, itemStyle: { color: '#E0EFFC', borderColor: '#378ADD', borderWidth: 2 }, label: { color: '#042C53' } },
                            { name: '$consName',  x: 340, y: 390, itemStyle: { color: '#FDECEA', borderColor: '#E24B4A', borderWidth: 2 }, label: { color: '#501313' } }
                        ],
                        links: [$links],
                        lineStyle: { curveness: 0.3 }
                    }]
                }, true);
                setTimeout(function() { chart.resize(); }, 100);
                window.addEventListener('resize', function() { chart.resize(); });
            })();
        """.trimIndent()

        chartDiv.element.executeJs(js)
    }
}
