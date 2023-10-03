package tk.xenon98.replicon.selenium.okta;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import tk.xenon98.replicon.driver.Driver;
import tk.xenon98.replicon.driver.DriverMethod;
import tk.xenon98.replicon.log.Logger;
import tk.xenon98.replicon.log.LoggingService;
import tk.xenon98.replicon.selenium.core.SeleniumDriver;

@Service
public class OktaDriver implements Driver {

	private static final String TAB_NAME = "Okta";
	private static final String OKTA_USER_HOME_URL = "https://sso.activeviam.com/app/UserHome";
	private final SeleniumDriver seleniumDriver;
	private final Logger logger;

	public OktaDriver(final SeleniumDriver seleniumDriver, final LoggingService loggingService) {
		this.seleniumDriver = seleniumDriver;
		this.logger = loggingService.getLogger(this.getClass().getName());
	}

	@DriverMethod
	public CompletableFuture<List<String>> login(final @NonNull String username, final @NonNull String password) {
		return seleniumDriver.execute(webDriver -> doLogin(webDriver, username, password));
	}

	@DriverMethod
	public CompletableFuture<String> openApp(final @NonNull String appName) {
		return seleniumDriver.execute(webDriver -> {
			tryOpenDashboardWithoutLogin(webDriver);

			Set<String> oldWindowHandles = webDriver.getWindowHandles();

			final var articleElement = webDriver.findElements(By.className("chiclet--article")).stream().filter(element -> element.getText().equals(appName)).findFirst().orElseThrow();
			articleElement.click();

			Set<String> newWindowHandles = webDriver.getWindowHandles();
			String currentHandle = null;
			for (final String handle : newWindowHandles) {
				if (!oldWindowHandles.contains(handle)) {
					currentHandle = handle;
					break;
				}
			}
			if (currentHandle == null) {
				currentHandle = webDriver.getWindowHandle();
			}
			webDriver.switchTo().window(currentHandle);
			this.seleniumDriver.bindTabAndHandle(appName, currentHandle);
			return currentHandle;
		});
	}

	private void tryOpenDashboardWithoutLogin(final WebDriver webDriver) {
		seleniumDriver.switchToTab(TAB_NAME);
		webDriver.get(OKTA_USER_HOME_URL);
		new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(
				ExpectedConditions.presenceOfElementLocated(By.className("dashboard--main")));
		new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(
				ExpectedConditions.numberOfElementsToBeMoreThan(By.className("chiclet--article"), 0));
	}

	private List<String> doLogin(final WebDriver webDriver, final String username, final String password) {
		seleniumDriver.switchToTab(TAB_NAME);
		webDriver.get(OKTA_USER_HOME_URL);
		try {
			new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(
					ExpectedConditions.presenceOfElementLocated(By.className("dashboard--main")));
			logger.info("Already logged in");
		} catch (TimeoutException e) {
			logger.info("Logging in");

			new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(
					ExpectedConditions.presenceOfElementLocated(By.id("okta-signin-username")));
			webDriver.findElement(By.id("okta-signin-username")).sendKeys(username);
			webDriver.findElement(By.id("okta-signin-password")).sendKeys(password);
			webDriver.findElement(By.id("okta-signin-submit")).click();

			new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(ExpectedConditions.presenceOfElementLocated(
					By.cssSelector("#form65 > div.o-form-button-bar > input")));
			webDriver.findElement(By.cssSelector("#form65 > div.o-form-button-bar > input")).click();

			new WebDriverWait(webDriver, Duration.ofSeconds(60)).until(
					ExpectedConditions.presenceOfElementLocated(By.className("dashboard--main")));
		}

		new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(
				ExpectedConditions.numberOfElementsToBeMoreThan(By.className("chiclet--article"), 0));
		return webDriver.findElements(By.className("chiclet--article")).stream().map(WebElement::getText).toList();
	}
}
