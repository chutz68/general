package ch.softhenge.solar.view

import ch.softhenge.solar.service.FiveMinData
import ch.softhenge.solar.service.SolarService
import com.vaadin.flow.component.button.Button
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
import java.time.LocalDate

@Route("live")
@PageTitle("Solar Live View")
@JavaScript("https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js")
class LiveView(private val solarService: SolarService) : VerticalLayout() {

    private val chartDiv = Div()
    private val datePicker = DatePicker("Date")

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        val nav = HorizontalLayout(
            H1("⚡ Solar Live View"),
            RouterLink("☀️ Dashboard", DashboardView::class.java)
        ).apply { alignItems = Alignment.BASELINE }
        add(nav)

        add(buildToolbar())

        chartDiv.element.setAttribute("id", "solar-chart")
        chartDiv.setWidthFull()
        chartDiv.height = "500px"
        add(chartDiv)

        loadData(LocalDate.now())
    }

    private fun buildToolbar(): HorizontalLayout {
        datePicker.value = LocalDate.now()
        datePicker.max = LocalDate.now()

        val prevButton = Button("◀") {
            datePicker.value = datePicker.value.minusDays(1)
            loadData(datePicker.value)
        }
        val nextButton = Button("▶") {
            if (datePicker.value < LocalDate.now()) {
                datePicker.value = datePicker.value.plusDays(1)
                loadData(datePicker.value)
            }
        }
        val todayButton = Button("Today") {
            datePicker.value = LocalDate.now()
            loadData(datePicker.value)
        }

        datePicker.addValueChangeListener { loadData(it.value) }

        return HorizontalLayout(prevButton, datePicker, nextButton, todayButton).apply {
            alignItems = Alignment.BASELINE
        }
    }

    private fun loadData(date: LocalDate) {
        val data = solarService.getFiveMinData(date.toString())
        renderChart(date.toString(), data)
    }

    private fun renderChart(date: String, data: List<FiveMinData>) {
        val times = data.map { "\"${it.t}\"" }.joinToString(",")
        val pW    = data.map { it.pW }.joinToString(",")
        val cW    = data.map { it.cW }.joinToString(",")
        val bcW   = data.map { it.bcW }.joinToString(",")
        val bdW   = data.map { it.bdW }.joinToString(",")
        val soc   = data.map { it.soc ?: 0.0 }.joinToString(",")

        val js = """
            (function() {
                var dom = document.getElementById('solar-chart');
                if (!dom) return;
                var chart = echarts.getInstanceByDom(dom) || echarts.init(dom);
                var option = {
                    title: { text: 'Solar 5-min – $date' },
                    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
                    legend: { data: ['Production', 'Consumption', 'Batt. Charging', 'Batt. Discharging', 'SOC %'] },
                    grid: { left: '5%', right: '8%', bottom: '15%', containLabel: true },
                    toolbox: { feature: { dataZoom: { yAxisIndex: 'none' }, restore: {}, saveAsImage: {} } },
                    dataZoom: [
                        { type: 'inside', start: 0, end: 100 },
                        { start: 0, end: 100 }
                    ],
                    xAxis: {
                        type: 'category',
                        data: [$times],
                        axisLabel: { interval: 11 }
                    },
                    yAxis: [
                        { type: 'value', name: 'Power [W]', position: 'left' },
                        { type: 'value', name: 'SOC [%]',   position: 'right', min: 0, max: 100 }
                    ],
                    series: [
                        { name: 'Production',        type: 'line', data: [$pW],  smooth: true, yAxisIndex: 0, color: '#f6c90e', areaStyle: { opacity: 0.1 }, lineStyle: { width: 2 } },
                        { name: 'Consumption',       type: 'line', data: [$cW],  smooth: true, yAxisIndex: 0, color: '#3da4ab', lineStyle: { width: 2 } },
                        { name: 'Batt. Charging',    type: 'line', data: [$bcW], smooth: true, yAxisIndex: 0, color: '#2ecc71', lineStyle: { width: 1 } },
                        { name: 'Batt. Discharging', type: 'line', data: [$bdW], smooth: true, yAxisIndex: 0, color: '#e74c3c', lineStyle: { width: 1 } },
                        { name: 'SOC %',             type: 'line', data: [$soc], smooth: true, yAxisIndex: 1, color: '#9b59b6', lineStyle: { width: 2, type: 'dashed' } }
                    ]
                };
                chart.setOption(option);
                window.addEventListener('resize', function() { chart.resize(); });
            })();
        """.trimIndent()

        chartDiv.element.executeJs(js)
    }
}
