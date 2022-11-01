package ch.softhenge.solarlog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
class SolarlogReaderApplicationTests {

	@Autowired
	private RestTemplate restTemplate;

	@Test
	void contextLoads() {

	}


}
