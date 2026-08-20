package ch.softhenge.solar.view

import ch.softhenge.solar.service.MonthlyDayData
import ch.softhenge.solar.service.SolarService
import ch.softhenge.solar.util.TimeUtils
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dependency.JavaScript
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Route("weather", layout = MainLayout::class)
@PageTitle("Weather")
@JavaScript("https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js")
class WeatherView(private val solarService: SolarService) : VerticalLayout() {

    private val chartDiv = Div()
    private var monthStart: LocalDate = TimeUtils.today().withDayOfMonth(1)

    init {
        setSizeFull()
        setPadding(true)
        setSpacing(true)

        add(buildToolbar())

        chartDiv.element.setAttribute("id", "weather-chart")
        chartDiv.setWidthFull()
        chartDiv.height = "550px"
        add(chartDiv)

        loadData()
    }

    private fun buildToolbar(): HorizontalLayout {
        val prevBtn = Button("◀") {
            monthStart = monthStart.minusMonths(1)
            loadData()
        }
        val nextBtn = Button("▶") {
            val next = monthStart.plusMonths(1)
            if (next <= TimeUtils.today().withDayOfMonth(1)) {
                monthStart = next
                loadData()
            }
        }
        val currentBtn = Button(getTranslation("weather.current")) {
            monthStart = TimeUtils.today().withDayOfMonth(1)
            loadData()
        }

        return HorizontalLayout(prevBtn, currentBtn, nextBtn).apply {
            alignItems = Alignment.BASELINE
        }
    }

    private fun loadData() {
        val from = monthStart
        val to   = monthStart.plusMonths(1).minusDays(1)
        val data = solarService.getMonthlyData(from.toString(), to.toString())
        renderChart(data)
    }

    private fun renderChart(data: List<MonthlyDayData>) {
        val monthLabel = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

        val days       = data.map { "\"${it.day.substring(5)}\"" }.joinToString(",")  // MM-DD
        val pKwh       = data.map { "%.2f".format(it.pWh / 1000.0) }.joinToString(",")
        val tempMax    = data.map { it.tempRealMax }.joinToString(",")
        val tempMin    = data.map { it.tempRealMin }.joinToString(",")
        val rain       = data.map { it.rainAmountSum }.joinToString(",")
        val snow       = data.map { it.snowAmountSum }.joinToString(",")
        val sunshineH  = data.map { "%.1f".format(it.sunshineMin / 60.0) }.joinToString(",")

        val js = """
            (function() {
                var dom = document.getElementById('weather-chart');
                if (!dom) return;
                var chart = echarts.getInstanceByDom(dom) || echarts.init(dom);
                var option = {
                    title: { text: 'Weather & Solar – $monthLabel', left: 'center' },
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: { type: 'cross' },
                        formatter: function(params) {
                            var s = params[0].axisValue + '<br/>';
                            params.forEach(function(p) {
                                s += p.marker + ' ' + p.seriesName + ': <b>' + p.value + '</b><br/>';
                            });
                            return s;
                        }
                    },
                    legend: {
                        data: ['Solar (kWh)', 'Temp Max (°C)', 'Temp Min (°C)', 'Rain (mm)', 'Snow (mm)', 'Sunshine (h)'],
                        top: 30
                    },
                    grid: { left: '5%', right: '14%', bottom: '15%', top: '80px', containLabel: true },
                    toolbox: { feature: { dataZoom: { yAxisIndex: 'none' }, restore: {}, saveAsImage: {} } },
                    dataZoom: [
                        { type: 'inside', start: 0, end: 100 },
                        { start: 0, end: 100, bottom: 5 }
                    ],
                    xAxis: {
                        type: 'category',
                        data: [$days],
                        axisLabel: { rotate: 30 }
                    },
                    yAxis: [
                        {
                            type: 'value',
                            name: 'Solar [kWh]',
                            position: 'left',
                            axisLine: { show: true, lineStyle: { color: '#f6c90e' } },
                            axisLabel: { color: '#f6c90e' },
                            nameTextStyle: { color: '#f6c90e' },
                            min: 0
                        },
                        {
                            type: 'value',
                            name: 'Temp [°C]',
                            position: 'right',
                            axisLine: { show: true, lineStyle: { color: '#e74c3c' } },
                            axisLabel: { color: '#e74c3c' },
                            nameTextStyle: { color: '#e74c3c' }
                        },
                        {
                            type: 'value',
                            name: 'mm / h',
                            position: 'right',
                            offset: 70,
                            axisLine: { show: true, lineStyle: { color: '#3498db' } },
                            axisLabel: { color: '#3498db' },
                            nameTextStyle: { color: '#3498db' },
                            min: 0
                        }
                    ],
                    series: [
                        {
                            name: 'Solar (kWh)',
                            type: 'bar',
                            data: [$pKwh],
                            yAxisIndex: 0,
                            color: '#f6c90e',
                            opacity: 0.85,
                            barMaxWidth: 18
                        },
                        {
                            name: 'Temp Max (°C)',
                            type: 'line',
                            data: [$tempMax],
                            yAxisIndex: 1,
                            color: '#e74c3c',
                            smooth: true,
                            lineStyle: { width: 2 },
                            symbol: 'circle',
                            symbolSize: 4
                        },
                        {
                            name: 'Temp Min (°C)',
                            type: 'line',
                            data: [$tempMin],
                            yAxisIndex: 1,
                            color: '#3498db',
                            smooth: true,
                            lineStyle: { width: 2 },
                            symbol: 'circle',
                            symbolSize: 4,
                            areaStyle: { color: 'rgba(52,152,219,0.05)' }
                        },
                        {
                            name: 'Rain (mm)',
                            type: 'bar',
                            data: [$rain],
                            yAxisIndex: 2,
                            color: '#2980b9',
                            opacity: 0.7,
                            barMaxWidth: 8,
                            stack: 'precip'
                        },
                        {
                            name: 'Snow (mm)',
                            type: 'bar',
                            data: [$snow],
                            yAxisIndex: 2,
                            color: '#ecf0f1',
                            opacity: 0.9,
                            barMaxWidth: 8,
                            stack: 'precip',
                            itemStyle: { borderColor: '#bdc3c7', borderWidth: 1 }
                        },
                        {
                            name: 'Sunshine (h)',
                            type: 'line',
                            data: [$sunshineH],
                            yAxisIndex: 2,
                            color: '#f39c12',
                            smooth: true,
                            lineStyle: { width: 2, type: 'dashed' },
                            symbol: 'none'
                        }
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
