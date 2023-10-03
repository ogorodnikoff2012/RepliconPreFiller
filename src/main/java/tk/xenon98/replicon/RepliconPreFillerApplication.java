package tk.xenon98.replicon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import tk.xenon98.replicon.selenium.core.SeleniumDriver;

@SpringBootApplication
public class RepliconPreFillerApplication {

	public static void main(String[] args) {
		final var ctx = SpringApplication.run(RepliconPreFillerApplication.class, args);
		openUi(ctx);
	}

	private static void openUi(final ApplicationContext ctx) {
		final int serverPort = ctx.getEnvironment().getProperty("server.port", int.class);
		final var seleniumDriver = ctx.getBean(SeleniumDriver.class);
		seleniumDriver.execute(webDriver -> {
			seleniumDriver.switchToTab("UI");
			webDriver.manage().window().maximize();
			webDriver.get("http://localhost:" + serverPort);
			return null;
		});
	}

}
