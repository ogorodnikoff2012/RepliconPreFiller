package tk.xenon98.replicon.selenium.google;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import tk.xenon98.replicon.driver.Driver;
import tk.xenon98.replicon.driver.DriverMethod;
import tk.xenon98.replicon.selenium.core.SeleniumDriver;

@Service
public class GoogleLoginDriver implements Driver {

	private static final String APP_ID = "GOOGLE";
	private static final String LOGIN_PAGE_URL = "https://accounts.google.com/";
	private final SeleniumDriver seleniumDriver;
	private final AtomicReference<CompletableFuture<Void>> loginFuture = new AtomicReference<>();

	public GoogleLoginDriver(final SeleniumDriver seleniumDriver) {
		this.seleniumDriver = seleniumDriver;
	}

	@DriverMethod
	public CompletableFuture<Void> login(final String username, final String password) {
		if (loginFuture.get() == null) {
			synchronized (loginFuture) {
				if (loginFuture.get() == null) {
					loginFuture.set(seleniumDriver.execute(driver -> doLogin(driver, username, password)));
				}
			}
		}
		return loginFuture.get();
	}

	@DriverMethod
	public CompletableFuture<Void> ensureLogin() {
		if (loginFuture.get() == null) {
			synchronized (loginFuture) {
				if (loginFuture.get() == null) {
					throw new IllegalStateException("Login not performed");
				}
			}
		}
		return loginFuture.get();
	}

	private Void doLogin(final WebDriver webDriver, final String username, final String password) {
		seleniumDriver.switchToTab(APP_ID);

		webDriver.get(LOGIN_PAGE_URL);
		
		new WebDriverWait(webDriver, Duration.ofSeconds(30)).until(ExpectedConditions.elementToBeClickable(By.id("identifierId")));
		webDriver.findElement(By.id("identifierId")).sendKeys(username);
		webDriver.findElement(By.id("identifierNext")).click();

		new WebDriverWait(webDriver, Duration.ofSeconds(30)).until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[type=password]")));
		webDriver.findElement(By.cssSelector("input[type=password]")).sendKeys(password);
		webDriver.findElement(By.id("passwordNext")).click();

		new WebDriverWait(webDriver, Duration.ofSeconds(60)).until(ExpectedConditions.textToBe(By.id("headingText"), "2-Step Verification"));
		new WebDriverWait(webDriver, Duration.ofSeconds(60)).until(ExpectedConditions.urlContains("myaccount.google.com"));

		return null;
	}
}
