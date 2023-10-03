package tk.xenon98.replicon.report;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tk.xenon98.replicon.git.GitCheckoutHistory;
import tk.xenon98.replicon.selenium.google.GoogleCalendarDriver;
import tk.xenon98.replicon.selenium.jira.JiraDriver;

@Configuration
public class SimpleReportGeneratorConfig {
	@Bean
	public SimpleReportGenerator simpleReportGenerator(
			@Autowired GitCheckoutHistory gitCheckoutHistory,
			@Autowired JiraDriver jiraDriver,
			@Autowired GoogleCalendarDriver googleCalendarDriver) {
		return new SimpleReportGenerator(gitCheckoutHistory, jiraDriver, googleCalendarDriver);
	}
}
