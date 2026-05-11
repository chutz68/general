package ch.softhenge.solar.view

import ch.softhenge.solar.service.EnergyFlowData
import ch.softhenge.solar.service.EnergyFlowKpis
import ch.softhenge.solar.service.SolarService
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Route("flow")
@PageTitle("Energiefluss – Solar")
@CssImport("./styles/energy-flow.css")
class EnergyFlowView(
    private val solarService: SolarService
) : VerticalLayout() {

    // ── UI-Referenzen ──────────────────────────────────────────────────────
    private val svgWrap   = Div()
    private val kpiBar    = Div()
    private val lastUpdate = Span()
    private val liveDot   = Span()

    // ── Auto-Refresh ───────────────────────────────────────────────────────
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var refreshTask: ScheduledFuture<*>? = null
    private var currentUi: UI? = null

    // ── Init ───────────────────────────────────────────────────────────────
    init {
        setSizeFull()
        setPadding(false)
        setSpacing(false)

        val container = Div().apply { addClassName("ef-container") }

        // Header
        liveDot.addClassName("ef-live-dot")
        val liveBadge = Div(liveDot, Span("Live · 5-Min")).apply {
            addClassName("ef-live-badge")
        }
        val header = Div(
            Span("Energiefluss").apply { addClassName("ef-title") },
            Div(liveBadge, lastUpdate).apply {
                addClassName("ef-live-badge")
                style.set("gap", "12px")
            }
        ).apply { addClassName("ef-header") }

        svgWrap.addClassName("ef-svg-wrap")
        kpiBar.addClassName("ef-kpi-bar")

        val divider = Div().apply { addClassName("ef-divider") }

        container.add(header, svgWrap, divider, kpiBar)
        add(container)

        // Initialer Ladeversuch mit Platzhalter-Daten
        renderPlaceholder()
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onAttach(event: AttachEvent) {
        super.onAttach(event)
        currentUi = event.ui
        refreshData()                          // sofort laden
        refreshTask = scheduler.scheduleAtFixedRate(
            { currentUi?.access { refreshData() } },
            REFRESH_INTERVAL_SECONDS,
            REFRESH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        )
    }

    override fun onDetach(event: DetachEvent) {
        super.onDetach(event)
        refreshTask?.cancel(false)
        currentUi = null
    }

    // ── Daten laden ────────────────────────────────────────────────────────

    private fun refreshData() {
        val data = solarService.getEnergyFlowData()
        if (data != null) {
            val kpis = solarService.computeKpis(data)
            renderFlow(data, kpis)
            lastUpdate.text = "aktualisiert ${data.t}"
        }
    }

    // ── SVG rendern ────────────────────────────────────────────────────────

    private fun renderFlow(d: EnergyFlowData, k: EnergyFlowKpis) {
        svgWrap.removeAll()
        svgWrap.element.setProperty("innerHTML", buildSvg(d, k))
        renderKpis(d, k)
    }

    private fun renderPlaceholder() {
        svgWrap.element.setProperty("innerHTML", buildSvg(PLACEHOLDER, PLACEHOLDER_KPIS))
        renderKpis(PLACEHOLDER, PLACEHOLDER_KPIS)
    }

    // ── KPI-Leiste ─────────────────────────────────────────────────────────

    private fun renderKpis(d: EnergyFlowData, k: EnergyFlowKpis) {
        kpiBar.removeAll()
        kpiBar.add(
            kpiCard("Eigenverbrauch", "${k.selfConsumptionPct.roundToInt()} %", null),
            kpiCard("Autarkie",       "${k.autarkyPct.roundToInt()} %",         null),
            kpiCard("Sonnenstunden",  "%.1f h".format(k.sunHours),              null),
            kpiCard("Peak heute",     "${k.peakW.roundToInt().fmtW()}",          k.peakTime),
        )
    }

    private fun kpiCard(label: String, value: String, sub: String?): Div {
        val lbl = Span(label).apply { addClassName("ef-kpi-label") }
        val val_ = Span(value).apply { addClassName("ef-kpi-value") }
        val card = Div(lbl, val_).apply { addClassName("ef-kpi-card") }
        if (sub != null) {
            card.add(Span(sub).apply { addClassName("ef-kpi-sub") })
        }
        return card
    }

    // ── SVG-Builder ────────────────────────────────────────────────────────

    private fun buildSvg(d: EnergyFlowData, k: EnergyFlowKpis): String {
        // Flussklassen je nach Vorzeichen
        val solarFlow  = if (d.pW > 0) "ef-flow-right" else "ef-flow-none"
        val battFlow   = if (d.bcW > 0) "ef-flow-down"
        else if (d.bdW > 0) "ef-flow-up"
        else "ef-flow-none"
        // Einspeisung: Pfad WR→Netz (abwärts), ef-flow-left, marker-end am Netz
        // Bezug:       Pfad Netz→WR (aufwärts), ef-flow-right (0→-24), marker-end am WR
        val gridFlow  = if (k.isFeeding) "ef-flow-down" else if (k.gridW != 0.0) "ef-flow-right" else "ef-flow-none"
        val houseFlow  = if (d.cW > 0) "ef-flow-right" else "ef-flow-none"

        // Netz-Badge
        val gridBadgeClass = if (k.isFeeding) "ef-badge ef-badge-feed" else "ef-badge ef-badge-draw"
        val gridBadgeText  = if (k.isFeeding) "Einspeisung" else "Bezug"
        val gridColor      = if (k.isFeeding) "#185FA5" else "#D85A30"
        val gridFlowColor  = if (k.isFeeding) "#185FA5" else "#D85A30"

        // Batterie-Füllbalken (0..60px Breite)
        val socPct    = d.soc ?: 0.0
        val battFill  = (socPct / 100.0 * 56).coerceIn(0.0, 56.0)
        val battLabel = if (d.bcW > 0) "↑ Laden" else if (d.bdW > 0) "↓ Entladen" else "Standby"

        // Watt-Linie Solar→WR
        val pvArrowColor  = if (d.pW > 50) "#185FA5" else "#888780"
        val pvFlowClass   = if (d.pW > 50) solarFlow else "ef-flow-none"
        val houseArrowCol = if (d.cW  > 0) "#1D9E75" else "#888780"
        val battArrowCol  = if (d.bcW > 0 || d.bdW > 0) "#1D9E75" else "#888780"

        return """
<svg class="ef-svg" viewBox="0 0 680 600" xmlns="http://www.w3.org/2000/svg" aria-label="Energiefluss-Diagramm">
  <defs>
    <marker id="ef-ar" viewBox="0 0 10 10" refX="8" refY="5"
            markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M2 1L8 5L2 9" fill="none" stroke="context-stroke"
            stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    </marker>
  </defs>

  <!-- ════════════════════════ SONNE ════════════════════════ -->
  <g transform="translate(340,68)">
    <circle class="ef-sun-ring" cx="0" cy="0" r="40" fill="#EF9F27"/>
    <circle cx="0" cy="0" r="28" fill="#FAC775" stroke="#EF9F27" stroke-width="1" opacity=".9"/>
    ${sunRays()}
    <text font-size="9" font-weight="500" fill="#633806"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="0" y="4">${d.pW.roundToInt().fmtW()}</text>
  </g>

  <!-- Sonne → Solar-Panel -->
  <line class="$pvFlowClass" x1="316" y1="93" x2="196" y2="148"
        stroke="#EF9F27" stroke-width="1.5" marker-end="url(#ef-ar)"/>

  <!-- ════════════════════════ SOLARANLAGE (links) ════════════════════════ -->
  <g class="ef-node">
    <rect x="40" y="148" width="160" height="96" rx="8"
          fill="var(--lumo-base-color)" stroke="var(--lumo-contrast-20pct)" stroke-width="0.5"/>
    <!-- Panel-Zeichnung -->
    ${panelIcon(54, 160)}
    <!-- Werte -->
    <text font-size="14" font-weight="500" fill="var(--lumo-body-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="120" y="218">${d.pW.roundToInt().fmtW()}</text>
    <text font-size="10" fill="var(--lumo-secondary-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="120" y="234">${(d.pWhToday / 1000.0).fmt1()} kWh heute</text>
  </g>

  <!-- Solar → Wechselrichter -->
  <line class="$pvFlowClass" x1="200" y1="218" x2="270" y2="238"
        stroke="$pvArrowColor" stroke-width="2" marker-end="url(#ef-ar)"/>
  <!-- Watt-Label -->
  <rect x="190" y="207" width="60" height="16" rx="4"
        fill="var(--lumo-contrast-5pct)"/>
  <text font-size="9" fill="$pvArrowColor" font-family="var(--lumo-font-family)"
        text-anchor="middle" x="220" y="219">${d.pW.roundToInt().fmtW()}</text>

  <!-- ════════════════════════ WECHSELRICHTER (Mitte) ════════════════════ -->
  <g class="ef-node">
    <rect x="270" y="200" width="140" height="80" rx="8"
          fill="var(--lumo-base-color)" stroke="var(--lumo-contrast-30pct)" stroke-width="1"/>
    <text font-size="18" fill="#0C447C" font-family="var(--lumo-font-family)"
          text-anchor="middle" x="340" y="244">∿</text>
    <text font-size="11" font-weight="500" fill="var(--lumo-body-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="340" y="263">Wechselrichter</text>
    <text font-size="10" fill="var(--lumo-secondary-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="340" y="276">98.2 % η</text>
  </g>

  <!-- WR → Haus -->
  <line class="$houseFlow" x1="410" y1="238" x2="480" y2="215"
        stroke="$houseArrowCol" stroke-width="2" marker-end="url(#ef-ar)"/>
  <rect x="420" y="224" width="52" height="16" rx="4"
        fill="var(--lumo-contrast-5pct)"/>
  <text font-size="9" fill="$houseArrowCol" font-family="var(--lumo-font-family)"
        text-anchor="middle" x="446" y="236">${d.cW.roundToInt().fmtW()}</text>

  <!-- ════════════════════════ VERBRAUCH (rechts) ════════════════════════ -->
  <g class="ef-node">
    <rect x="480" y="148" width="160" height="96" rx="8"
          fill="var(--lumo-base-color)" stroke="var(--lumo-contrast-20pct)" stroke-width="0.5"/>
    <!-- Haus-Icon -->
    ${houseIcon(560, 160)}
    <!-- Werte -->
    <text font-size="14" font-weight="500" fill="var(--lumo-body-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="560" y="218">${d.cW.roundToInt().fmtW()}</text>
    <text font-size="10" fill="var(--lumo-secondary-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="560" y="234">${(d.cWhToday / 1000.0).fmt1()} kWh heute</text>
  </g>

  <!-- WR → Batterie (vertikal) -->
  <line class="$battFlow" x1="340" y1="280" x2="340" y2="348"
        stroke="$battArrowCol" stroke-width="1.5" marker-end="url(#ef-ar)"/>
  <rect x="350" y="306" width="46" height="16" rx="4"
        fill="var(--lumo-contrast-5pct)"/>
  <text font-size="9" fill="$battArrowCol" font-family="var(--lumo-font-family)"
        text-anchor="middle" x="373" y="318">${(if (d.bcW > 0) d.bcW else d.bdW).roundToInt().fmtW()}</text>

  <!-- ════════════════════════ BATTERIE (Mitte unten) ════════════════════ -->
  <g class="ef-node">
    <rect x="270" y="348" width="140" height="96" rx="8"
          fill="var(--lumo-base-color)" stroke="var(--lumo-contrast-20pct)" stroke-width="0.5"/>
    <!-- Batterie-Symbol -->
    <rect x="308" y="360" width="60" height="26" rx="3"
          fill="none" stroke="var(--lumo-contrast-30pct)" stroke-width="1"/>
    <rect x="368" y="367" width="5" height="12" rx="1"
          fill="var(--lumo-contrast-30pct)"/>
    <!-- Füllbalken -->
    <rect x="311" y="363" width="${battFill.roundToInt()}" height="20" rx="2"
          fill="#1D9E75" opacity=".65"/>
    <!-- SOC-Label -->
    <text font-size="9" font-weight="500" fill="#085041"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="340" y="377">${socPct.roundToInt()} %</text>
    <!-- Leistung -->
    <text font-size="14" font-weight="500" fill="var(--lumo-body-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="340" y="412">${(if (d.bcW > 0) d.bcW else d.bdW).roundToInt().fmtW()}</text>
    <text font-size="10" fill="var(--lumo-secondary-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="340" y="428">$battLabel</text>
  </g>

  <!-- WR ↔ Netz: Pfadrichtung bestimmt Pfeilspitze und Flussrichtung -->
  <path class="$gridFlow" fill="none"
        stroke="$gridFlowColor" stroke-width="1.5"
        marker-end="url(#ef-ar)"
        d="${if (k.isFeeding) "M285 280 L285 318 L120 318 L120 348" else "M120 348 L120 318 L285 318 L285 280"}"/>
  <rect x="143" y="305" width="82" height="16" rx="4"
        fill="var(--lumo-contrast-5pct)"/>
  <text font-size="9" fill="$gridFlowColor" font-family="var(--lumo-font-family)"
        text-anchor="middle" x="184" y="317">${gridBadgeText}: ${abs(k.gridW).roundToInt().fmtW()}</text>

  <!-- ════════════════════════ NETZ (links unten) ════════════════════════ -->
  <g class="ef-node">
    <rect x="40" y="348" width="160" height="110" rx="8"
          fill="var(--lumo-base-color)" stroke="var(--lumo-contrast-20pct)" stroke-width="0.5"/>
    <!-- Netz-Icon -->
    ${gridIcon(120, 358)}
    <!-- Aktueller Wert mit Richtungsangabe -->
    <text font-size="13" font-weight="500" fill="$gridFlowColor"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="120" y="382">${gridBadgeText}: ${abs(k.gridW).roundToInt().fmtW()}</text>
    <!-- Tageswerte: immer beide anzeigen -->
    <text font-size="10" fill="var(--lumo-secondary-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="120" y="400">↑ Einsp.: ${(d.eWhToday / 1000.0).fmt1()} kWh</text>
    <text font-size="10" fill="var(--lumo-secondary-text-color)"
          font-family="var(--lumo-font-family)"
          text-anchor="middle" x="120" y="416">↓ Bezug: ${(d.iWhToday / 1000.0).fmt1()} kWh</text>
  </g>

</svg>""".trimIndent()
    }

    // ── SVG-Hilfsfunktionen ────────────────────────────────────────────────

    private fun sunRays() = buildString {
        val rays = listOf(0, 45, 90, 135, 180, 225, 270, 315)
        rays.forEach { deg ->
            val rad = Math.toRadians(deg.toDouble())
            val x1 = (Math.sin(rad) * 33).roundToInt()
            val y1 = -(Math.cos(rad) * 33).roundToInt()
            val x2 = (Math.sin(rad) * 40).roundToInt()
            val y2 = -(Math.cos(rad) * 40).roundToInt()
            append("""<line x1="$x1" y1="$y1" x2="$x2" y2="$y2" """)
            append("""stroke="#EF9F27" stroke-width="1.5" stroke-linecap="round"/>""")
        }
    }

    private fun panelIcon(x: Int, y: Int) = """
        <rect x="$x"       y="$y"        width="44" height="30" rx="2"
              fill="#185FA5" opacity=".15" stroke="#185FA5" stroke-width="0.5"/>
        <line x1="${x+22}" y1="$y"        x2="${x+22}" y2="${y+30}"
              stroke="#185FA5" stroke-width="0.5" opacity=".5"/>
        <line x1="$x"      y1="${y+15}"   x2="${x+44}" y2="${y+15}"
              stroke="#185FA5" stroke-width="0.5" opacity=".5"/>
        <rect x="${x+50}"  y="$y"         width="44" height="30" rx="2"
              fill="#185FA5" opacity=".15" stroke="#185FA5" stroke-width="0.5"/>
        <line x1="${x+72}" y1="$y"        x2="${x+72}" y2="${y+30}"
              stroke="#185FA5" stroke-width="0.5" opacity=".5"/>
        <line x1="${x+50}" y1="${y+15}"   x2="${x+94}" y2="${y+15}"
              stroke="#185FA5" stroke-width="0.5" opacity=".5"/>
    """.trimIndent()

    private fun houseIcon(cx: Int, y: Int) = """
        <polygon points="${cx},${y} ${cx-18},${y+18} ${cx+18},${y+18}"
                 fill="var(--lumo-contrast-20pct)"/>
        <rect x="${cx-10}" y="${y+18}" width="20" height="16" rx="1"
              fill="var(--lumo-contrast-10pct)"/>
    """.trimIndent()

    private fun gridIcon(cx: Int, y: Int) = """
        <line x1="${cx-40}" y1="${y+8}"  x2="${cx+40}" y2="${y+8}"
              stroke="var(--lumo-contrast-20pct)" stroke-width="1.2" stroke-linecap="round"/>
        <line x1="${cx-40}" y1="${y+15}" x2="${cx+40}" y2="${y+15}"
              stroke="var(--lumo-contrast-20pct)" stroke-width="1.2" stroke-linecap="round" opacity=".6"/>
        <circle cx="${cx-24}" cy="${y+11}" r="3" fill="#185FA5" opacity=".5"/>
        <circle cx="${cx}"    cy="${y+11}" r="3" fill="#185FA5" opacity=".5"/>
        <circle cx="${cx+24}" cy="${y+11}" r="3" fill="#185FA5" opacity=".5"/>
    """.trimIndent()

    // ── Formatter-Extensions ───────────────────────────────────────────────

    private fun Int.fmtW(): String =
        if (this >= 1000) "%.1f kW".format(this / 1000.0) else "$this W"

    private fun Double.fmt1(): String = "%.1f".format(this)

    // ── Companion ──────────────────────────────────────────────────────────

    companion object {
        private const val REFRESH_INTERVAL_SECONDS = 300L  // 5 Minuten

        // Platzhalter-Daten bis erster BigQuery-Abruf abgeschlossen
        val PLACEHOLDER = EnergyFlowData(
            t = "--:--", pW = 0.0, cW = 0.0, bcW = 0.0, bdW = 0.0, soc = null,
            pWhToday = 0.0, cWhToday = 0.0, iWhToday = 0.0, eWhToday = 0.0,
            sunMinutes = 0, peakW = 0.0, peakTime = "--:--",
        )
        val PLACEHOLDER_KPIS = EnergyFlowKpis(
            selfConsumptionPct = 0.0, autarkyPct = 0.0,
            sunHours = 0.0, peakW = 0.0, peakTime = "--:--", isFeeding = true, gridW = 0.0,
        )
    }
}
