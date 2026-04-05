package ch.softhenge.solar

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@Theme("solar-dashboard")
class SolarDashboardApplication : AppShellConfigurator

fun main(args: Array<String>) {
    runApplication<SolarDashboardApplication>(*args)
}
