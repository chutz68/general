package ch.softhenge.solar.view

import ch.softhenge.solar.service.FiveMinData
import ch.softhenge.solar.service.SolarService
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.dependency.JavaScript
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import ch.softhenge.solar.util.TimeUtils
import java.time.LocalDate

@Route("live")
@PageTitle("Solar Live View")
@JavaScript("https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js")
class LiveView(private val solarService: SolarService) : VerticalLayout() {

    private val chartDiv = Div()
    private val datePicker = DatePicker(getTranslation("live.today"))
    private var showBattery = false
    private var currentPeriod = "day"

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val nav = HorizontalLayout(
            H1(getTranslation("live.title")),
            RouterLink(getTranslation("live.nav.flow"),  EnergyFlowView::class.java),
            RouterLink(getTranslation("live.nav.data"),  DashboardView::class.java)
        ).apply { alignItems = Alignment.BASELINE; setSpacing(true) }
        add(nav)
        add(buildToolbar())

        chartDiv.element.setAttribute("id", "solar-chart")
        chartDiv.setWidthFull()
        chartDiv.height = "500px"
        add(chartDiv)

        loadData()
    }

    private fun buildToolbar(): VerticalLayout {
        datePicker.value = TimeUtils.today()
        datePicker.max = TimeUtils.today()

        val prevButton  = Button("◀") { navigateDate(-1) }
        val nextButton  = Button("▶") { navigateDate(1) }
        val todayButton = Button(getTranslation("live.today")) {
            datePicker.value = TimeUtils.today()
            loadData()
        }
        datePicker.addValueChangeListener { loadData() }

        val dateNav = HorizontalLayout(prevButton, datePicker, nextButton, todayButton).apply {
            alignItems = Alignment.BASELINE
        }

        val dayButton   = periodButton(getTranslation("live.period.day"),   "day")
        val weekButton  = periodButton(getTranslation("live.period.week"),  "week")
        val monthButton = periodButton(getTranslation("live.period.month"), "month")
        dayButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val periodNav = HorizontalLayout(dayButton, weekButton, monthButton)

        val batteryButton = Button(getTranslation("live.battery.show")) {
            showBattery = !showBattery
            it.source.text = if (showBattery) getTranslation("live.battery.hide") else getTranslation("live.battery.show")
            loadData()
        }

        val toolbar = HorizontalLayout(dateNav, periodNav, batteryButton).apply {
            alignItems = Alignment.BASELINE
            setWidthFull()
        }

        return VerticalLayout(toolbar).apply {
            setPadding(false)
            setSpacing(false)
        }
    }

    private fun periodButton(label: String, period: String): Button {
        return Button(label) {
            currentPeriod = period
            (it.source.parent.get() as HorizontalLayout).children.forEach { btn ->
                if (btn is Button) btn.removeThemeVariants(ButtonVariant.LUMO_PRIMARY)
            }
            it.source.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            loadData()
        }
    }

    private fun navigateDate(days: Long) {
        val newDate = when (currentPeriod) {
            "week"  -> datePicker.value.plusWeeks(days)
            "month" -> datePicker.value.plusMonths(days)
            else    -> datePicker.value.plusDays(days)
        }
        if (newDate <= TimeUtils.today()) {
            datePicker.value = newDate
            loadData()
        }
    }

    private fun loadData() {
        val date = datePicker.value ?: LocalDate.now()
        val (fromDate, toDate) = when (currentPeriod) {
            "week"  -> date.minusDays(6) to date
            "month" -> date.withDayOfMonth(1) to date.withDayOfMonth(date.lengthOfMonth())
            else    -> date to date
        }

        val rawData = solarService.getFiveMinData(fromDate.toString(), toDate.toString())

        val aggregatedData = when (currentPeriod) {
            "month" -> aggregate(rawData, 24)
            "week"  -> aggregate(rawData, 12)
            else    -> rawData
        }

        renderChart(fromDate.toString(), toDate.toString(), aggregatedData)
    }

    private fun aggregate(data: List<FiveMinData>, bucketSize: Int): List<FiveMinData> {
        return data.chunked(bucketSize) { chunk ->
            FiveMinData(
                t   = chunk.first().t,
                pW  = chunk.map { it.pW }.average(),
                cW  = chunk.map { it.cW }.average(),
                bcW = chunk.map { it.bcW }.average(),
                bdW = chunk.map { it.bdW }.average(),
                soc = chunk.mapNotNull { it.soc }.average().takeIf { !it.isNaN() }
            )
        }
    }

    private fun renderChart(fromDate: String, toDate: String, data: List<FiveMinData>) {
        val title = if (fromDate == toDate) fromDate else "$fromDate → $toDate"

        val times = if (currentPeriod == "day") {
            data.map { "\"${it.t.takeLast(5)}\"" }.joinToString(",")
        } else {
            data.map { "\"${it.t}\"" }.joinToString(",")
        }

        val pW  = data.map { it.pW }.joinToString(",")
        val cW  = data.map { it.cW }.joinToString(",")
        val bcW = data.map { it.bcW }.joinToString(",")
        val bdW = data.map { it.bdW }.joinToString(",")
        val soc = data.map { it.soc ?: 0.0 }.joinToString(",")

        val chartType = if (currentPeriod == "month") "bar" else "line"
        val smooth    = if (currentPeriod == "month") "false" else "true"

        val interval = when (currentPeriod) {
            "day"  -> 11
            "week" -> 23
            else   -> 11
        }

        val batterySeries = if (showBattery) """
            { name: 'Battery Charging',    type: '$chartType', data: [$bcW], smooth: $smooth, yAxisIndex: 0, color: '#2ecc71', lineStyle: { width: 1 } },
            { name: 'Battery Discharging', type: '$chartType', data: [$bdW], smooth: $smooth, yAxisIndex: 0, color: '#e74c3c', lineStyle: { width: 1 } },
        """ else ""

        val legendData = if (showBattery)
            "['Production', 'Consumption', 'Battery Charging', 'Battery Discharging', 'SOC %']"
        else
            "['Production', 'Consumption', 'SOC %']"

        val js = """
            (function() {
                var dom = document.getElementById('solar-chart');
                if (!dom) return;
                var chart = echarts.getInstanceByDom(dom) || echarts.init(dom);
                var option = {
                    title: { text: 'Solar – $title' },
                    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
                    legend: { data: $legendData },
                    grid: { left: '5%', right: '8%', bottom: '15%', containLabel: true },
                    toolbox: { feature: { dataZoom: { yAxisIndex: 'none' }, restore: {}, saveAsImage: {} } },
                    dataZoom: [
                        { type: 'inside', start: 0, end: 100 },
                        { start: 0, end: 100 }
                    ],
                    xAxis: {
                        type: 'category',
                        data: [$times],
                        axisLabel: { interval: $interval, rotate: 30 }
                    },
                    yAxis: [
                        { type: 'value', name: 'Power [W]', position: 'left' },
                        { type: 'value', name: 'SOC [%]',   position: 'right', min: 0, max: 100 }
                    ],
                    series: [
                        { name: 'Production',  type: '$chartType', data: [$pW], smooth: $smooth, yAxisIndex: 0, color: '#f6c90e', areaStyle: { opacity: 0.1 }, lineStyle: { width: 2 } },
                        { name: 'Consumption', type: '$chartType', data: [$cW], smooth: $smooth, yAxisIndex: 0, color: '#3da4ab', lineStyle: { width: 2 } },
                        $batterySeries
                        { name: 'SOC %', type: 'line', data: [$soc], smooth: true, yAxisIndex: 1, color: '#9b59b6', lineStyle: { width: 2, type: 'dashed' } }
                    ]
                };
                chart.setOption(option, true);
                setTimeout(function() { chart.resize(); }, 100);
                window.addEventListener('resize', function() { chart.resize(); });
            })();
        """.trimIndent()

        chartDiv.element.executeJs(js)
    }
}
