package tk.xenon98.replicon.selenium.core;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {

	@Bean
	public void webDriverManager() {
		WebDriverManager.chromedriver().setup();
	}

	@Bean(destroyMethod = "quit")
	public WebDriver webDriver() {
		//Create a map to store  preferences
		Map<String, Object> prefs = new HashMap<>();

		//add key and value to map as follow to switch off browser notification
		//Pass the argument 1 to allow and 2 to block
		prefs.put("profile.default_content_setting_values.notifications", 2);

		//Create an instance of ChromeOptions
		ChromeOptions options = new ChromeOptions();

		// set ExperimentalOption - prefs
		options.setExperimentalOption("prefs", prefs);

		//Now Pass ChromeOptions instance to ChromeDriver Constructor to initialize chrome driver which will switch off this browser notification on the chrome browser
		return new ChromeDriver(options);
	}

	@Bean
	public Thread webDriverWindowPinger(@Autowired ConfigurableApplicationContext ctx, @Autowired WebDriver webDriver) {
		final var t = new Thread(() -> {
			try {
				while (true) {
					webDriver.getTitle();
					Thread.sleep(250);
				}
			} catch (Exception e) {
			}
			ctx.close();
		}, "web-driver-window-pinger");
		t.start();
		return t;
	}
}
