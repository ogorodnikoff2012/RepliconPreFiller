package tk.xenon98.replicon.selenium.jira;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import tk.xenon98.replicon.driver.Driver;
import tk.xenon98.replicon.driver.DriverMethod;
import tk.xenon98.replicon.log.Logger;
import tk.xenon98.replicon.log.LoggingService;
import tk.xenon98.replicon.selenium.core.SeleniumDriver;
import tk.xenon98.replicon.selenium.okta.OktaDriver;

@Service
public class JiraDriver implements Driver {

	private static final String JIRA_BROWSE_URL = "https://activeviam.atlassian.net/browse";
	private static final String APP_ID = "Atlassian Cloud Jira SAML";
	private final SeleniumDriver seleniumDriver;
	private final Logger logger;
	private final OktaDriver oktaDriver;

	private final ConcurrentHashMap<String, Ticket> ticketCache = new ConcurrentHashMap<>();
	private final AtomicReference<CompletableFuture<Void>> tabLoadingTask = new AtomicReference<>();

	public JiraDriver(final SeleniumDriver seleniumDriver, final OktaDriver oktaDriver, final LoggingService loggingService) {
		this.seleniumDriver = seleniumDriver;
		this.oktaDriver = oktaDriver;
		this.logger = loggingService.getLogger(this.getClass().getName());
	}

	@DriverMethod
	public CompletableFuture<Ticket> getTicket(final String ticketId) {
		if (this.ticketCache.containsKey(ticketId)) {
			return CompletableFuture.completedFuture(this.ticketCache.get(ticketId));
		}
		return tabGuard().thenCompose(unused -> this.seleniumDriver.execute(webDriver -> doGetTicket(webDriver, ticketId))).thenApply(ticket -> {
			this.ticketCache.put(ticketId, ticket);
			return ticket;
		});
	}

	private CompletableFuture<Void> tabGuard() {
		if (tabLoadingTask.get() == null) {
			synchronized (tabLoadingTask) {
				if (tabLoadingTask.get() == null) {
					tabLoadingTask.set(this.oktaDriver.openApp(APP_ID).thenAccept(handle -> seleniumDriver.bindTabAndHandle(APP_ID, handle)));
				}
			}
		}
		return tabLoadingTask.get();
	}

	private Ticket doGetTicket(final WebDriver webDriver, final String ticketId) {
		seleniumDriver.switchToTab(APP_ID);
		webDriver.get(JIRA_BROWSE_URL + "/" + ticketId);

		new WebDriverWait(webDriver, Duration.ofSeconds(30)).until(ExpectedConditions.presenceOfElementLocated(
				By.cssSelector("[data-test-id=\"issue.views.issue-base.foundation.summary.heading\"]")));

		final String title = webDriver.findElement(By.cssSelector("[data-test-id=\"issue.views.issue-base.foundation.summary.heading\"]")).getText();
		final String origin = webDriver.findElement(By.cssSelector("[data-test-id=\"issue.views.field.select.common.select-inline-edit.customfield_10148\"] > div:nth-child(2)")).getText();
		return new Ticket(ticketId, title, origin);
	}
}
