package tk.xenon98.replicon.selenium.replicon;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import tk.xenon98.replicon.driver.Driver;
import tk.xenon98.replicon.driver.DriverMethod;
import tk.xenon98.replicon.range.Range;
import tk.xenon98.replicon.selenium.core.SeleniumDriver;

@Service
public class RepliconDriver implements Driver {

	private static final String REPLICON_TIMESHEET_URL = "https://na4.replicon.com/QuartetFinancial/my/timesheet/";
	public static final String APP_ID = "Replicon";
	private final SeleniumDriver seleniumDriver;

	private final AtomicReference<RepliconTimesheetEditor> currentTimesheetEditor = new AtomicReference<>();

	public RepliconDriver(final SeleniumDriver seleniumDriver) {
		this.seleniumDriver = seleniumDriver;
	}

	@DriverMethod
	public CompletableFuture<List<Range<LocalDate>>> retrieveNotSubmittedDates() {
		return seleniumDriver.execute(this::doRetrieveNotSubmittedDates);
	}

	private List<Range<LocalDate>> doRetrieveNotSubmittedDates(final WebDriver webDriver) {
		openAllTimesheetsPage(webDriver);
		final List<Range<LocalDate>> result = new ArrayList<>();
		final List<WebElement> rows = webDriver.findElements(By.cssSelector("#grid tr.ui-widget-content"));
		for (final WebElement row : rows) {
			final List<WebElement> cells = row.findElements(By.tagName("td"));
			final String period = cells.get(1).getText();
			final String status = cells.get(2).getText();

			if ("Not Submitted".equals(status)) {
				result.add(parseRange(period));
			}
		}
		return result;
	}

	private Range<LocalDate> parseRange(final String period) {
		final String[] dates = period.split(" - ");
		if (dates.length != 2) {
			throw new IllegalArgumentException("Invalid period: " + period);
		}

		return new Range<>(parseDate(dates[0]), parseDate(dates[1]));
	}

	private LocalDate parseDate(final String date) {
		final String[] numbers = date.split("/");
		if (numbers.length != 3) {
			throw new IllegalArgumentException("Invalid date: " + date);
		}

		final int year = Integer.parseInt(numbers[0]);
		final int month = Integer.parseInt(numbers[1]);
		final int day = Integer.parseInt(numbers[2]);

		return LocalDate.of(year, month, day);
	}

	private void openAllTimesheetsPage(final WebDriver webDriver) {
		seleniumDriver.switchToTab(APP_ID);
		webDriver.get(REPLICON_TIMESHEET_URL);
		new WebDriverWait(webDriver, Duration.ofSeconds(5)).until(ExpectedConditions.presenceOfElementLocated(
				By.className("repliconLogo")));
	}

	RepliconTimesheetEditor getCurrentTimesheetEditor() {
		return currentTimesheetEditor.get();
	}

	@DriverMethod
	public CompletableFuture<Void> editTimesheet(final LocalDate weekDay, final Consumer<RepliconTimesheetEditor> action) {
		return seleniumDriver.execute(webDriver -> {
			seleniumDriver.switchToTab(APP_ID);
			webDriver.get(REPLICON_TIMESHEET_URL + weekDay.getYear() + '-' + weekDay.getMonthValue() + '-' + weekDay.getDayOfMonth());
			final var editor = new RepliconTimesheetEditor(this, webDriver);
			try {
				currentTimesheetEditor.set(editor);
				action.accept(editor);
			} finally {
				currentTimesheetEditor.set(null);
			}
			return null;
		});
	}
}
