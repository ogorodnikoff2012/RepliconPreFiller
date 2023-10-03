package tk.xenon98.replicon.selenium.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import tk.xenon98.replicon.driver.Driver;
import tk.xenon98.replicon.driver.DriverMethod;

@Service
@DependsOn("webDriverManager")
public class SeleniumDriver implements DisposableBean, Driver {

	private final WebDriver webDriver;
	private final ExecutorService taskQueue;
	private final TabAssigner tabAssigner;

	public SeleniumDriver(final WebDriver webDriver) {
		this.webDriver = webDriver;
		this.taskQueue = Executors.newSingleThreadExecutor();
		this.tabAssigner = new TabAssigner(webDriver);
	}

	@DriverMethod
	public <T> CompletableFuture<T> execute(final Function<WebDriver, T> callback) {
		return CompletableFuture.supplyAsync(() -> callback.apply(this.webDriver), this.taskQueue);
	}

	@DriverMethod
	public String getWindowHandleForTab(final String tab) {
		return this.tabAssigner.getTab(tab);
	}

	@DriverMethod
	public void switchToTab(final String tab) {
		final String windowHandle = getWindowHandleForTab(tab);
		this.webDriver.switchTo().window(windowHandle);
	}

	@DriverMethod
	public boolean hasTab(final String tab) {
		return this.tabAssigner.hasTab(tab);
	}

	@DriverMethod
	public CompletableFuture<Collection<String>> windows() {
		return execute(WebDriver::getWindowHandles);
	}

	@Override
	public void destroy() throws Exception {
		this.taskQueue.shutdown();
	}

	@DriverMethod
	public void bindTabAndHandle(final String tab, final String handle) {
		this.tabAssigner.bindTabAndHandle(tab, handle);
	}
}
