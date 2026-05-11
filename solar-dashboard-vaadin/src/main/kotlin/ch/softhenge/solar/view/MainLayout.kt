package ch.softhenge.solar.view

import ch.softhenge.solar.util.TimeUtils
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.RouterLayout
import org.slf4j.LoggerFactory

class MainLayout : AppLayout(), RouterLayout {

    private val log = LoggerFactory.getLogger(MainLayout::class.java)

    init {
        detectBrowserTimezone()
        createHeader()
        createDrawer()
    }

    private fun detectBrowserTimezone() {
        UI.getCurrent()?.page?.executeJs(
            "return Intl.DateTimeFormat().resolvedOptions().timeZone"
        )?.then(String::class.java) { tz ->
            TimeUtils.setZone(tz)
            log.debug("Browser timezone: $tz → TimeUtils.ZONE=${TimeUtils.ZONE.id}")
        }
    }

    private fun createHeader() {
        val toggle = DrawerToggle()

        val title = H1("Solar Dashboard").apply {
            style
                .set("font-size", "1.1rem")
                .set("font-weight", "600")
                .set("margin", "0")
                .set("color", "var(--lumo-base-color)")
        }

        val header = HorizontalLayout(toggle, title).apply {
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            style
                .set("background", "var(--lumo-primary-color)")
                .set("padding", "0 1rem")
                .set("height", "56px")
        }

        addToNavbar(header)
    }

    private fun createDrawer() {
        val nav = SideNav().apply {
            addItem(
                SideNavItem("⚡ Energiefluss",  EnergyFlowView::class.java),
                SideNavItem("📈 Live-Ansicht",   LiveView::class.java),
                SideNavItem("📊 Dashboard",      DashboardView::class.java),
            )
        }
        addToDrawer(nav)
    }
}
