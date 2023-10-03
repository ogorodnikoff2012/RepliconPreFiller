package tk.xenon98.replicon.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import tk.xenon98.replicon.log.Logger;
import tk.xenon98.replicon.log.LoggingService;

@Controller
public class UiController {

	private final Logger logger;

	@Value("${spring.application.name}")
	protected String appName;

	@Value("${server.port}")
	protected int port;

	public UiController(final LoggingService loggingService) {
		this.logger = loggingService.getLogger(this.getClass().getName());
	}

	@GetMapping("/")
	public String homePage(final Model model) {
		this.logger.info("Home page requested");
		model.addAttribute("appName", appName);
		return "home";
	}
}
