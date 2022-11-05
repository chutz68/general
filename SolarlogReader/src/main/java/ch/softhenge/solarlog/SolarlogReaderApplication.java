package ch.softhenge.solarlog;

import ch.softhenge.solarlog.weather.WeatherService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@SpringBootApplication
public class SolarlogReaderApplication {

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public WeatherService getWeatherService() {
		return new WeatherService(getRestTemplate());
	}

	public static void main(String[] args) {
		SpringApplication.run(SolarlogReaderApplication.class, args);
	}

	/**
	 * To run the application, run the following command in a terminal window:
	 * ./gradlew bootRun
	 *
	 * @param applicationContext the ApplicationContext
	 * @return commandLineRunner
	 */
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext applicationContext) {
		return args -> {
			System.out.println("Spring boot beans:");
			String[] beanNames = applicationContext.getBeanDefinitionNames();
			Arrays.stream(beanNames)
					.sorted()
					.forEach(System.out::println);
		};
	}

}
