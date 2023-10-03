package tk.xenon98.replicon.selenium.google;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import tk.xenon98.replicon.driver.Driver;
import tk.xenon98.replicon.driver.DriverMethod;
import tk.xenon98.replicon.selenium.core.SeleniumDriver;

@Service
public class GoogleCalendarDriver implements Driver {

	private static final String APP_ID = "GOOGLE_CALENDAR";
	private static final String APP_URL = "https://calendar.google.com";
	private static final String[] MONTH_SHORT_NAMES = {
			"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
	};
	private static final Pattern DATE_TIME_PATTERN =
			Pattern.compile("((\\w+) (\\d+), (\\d+) at )?(\\d{2}):(\\d{2})");


	private final SeleniumDriver seleniumDriver;
	private final GoogleLoginDriver googleLoginDriver;

	public GoogleCalendarDriver(final SeleniumDriver seleniumDriver, final GoogleLoginDriver googleLoginDriver) {
		this.seleniumDriver = seleniumDriver;
		this.googleLoginDriver = googleLoginDriver;
	}

	@DriverMethod
	public CompletableFuture<List<CalendarEvent>> getEventsForDate(final LocalDate date) {
		return this.googleLoginDriver.ensureLogin()
				.thenCompose(__ -> seleniumDriver.execute(driver -> {
					while (true) {
						try {
							return doGetEventsForDate(driver, date);
						} catch (StaleElementReferenceException ignored) {

						}
					}
				}));
	}

	private List<CalendarEvent> doGetEventsForDate(final WebDriver driver, final LocalDate date) {
		seleniumDriver.switchToTab(APP_ID);
		driver.get(getUrlForDate(date));

		int delayMs = 1000;
		while (driver.getTitle().contains("403")) {
			try {
				Thread.sleep(delayMs);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			driver.get(getUrlForDate(date));
			delayMs *= 2;
		}

		new WebDriverWait(driver, Duration.ofSeconds(30)).until(
				ExpectedConditions.numberOfElementsToBe(By.cssSelector("div[role=gridcell] > h2"), 2));
		final int numOfEvents =
				parseEventSpec(driver.findElements(By.cssSelector("div[role=gridcell] > h2")).get(1).getText());

		final List<String> eventDescriptions = ((Supplier<List<String>>) () -> {
			final WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
			for (int i = 0; i < 5; ++i) {
				try {
					wait.until(ExpectedConditions.numberOfElementsToBe(
							By.cssSelector("div[role=presentation] > div[role=button]"), numOfEvents));
					final List<WebElement> eventButtons =
							driver.findElements(By.cssSelector("div[role=presentation] > div[role=button]"));
					return eventButtons.stream().map(WebElement::getText).toList();
				} catch (final StaleElementReferenceException e) {
					// Try again...
				}
			}
			throw new IllegalStateException("Cannot get event buttons");
		}).get();

		final List<CalendarEvent> events = eventDescriptions.stream().map(desc -> parseEvent(desc, date)).toList();
		System.out.println(events);
		return events;
	}

	private int parseEventSpec(final String eventSpec) {
		final String token = eventSpec.split(" ", 2)[0];
		if ("NO".equalsIgnoreCase(token)) {
			return 0;
		}
		return Integer.parseInt(token);
	}

	private CalendarEvent parseEvent(final String eventDescription, final LocalDate date) {
		final String[] lines = eventDescription.split("\n");

		final String title = lines[1];
		final String dateTimeSpec = lines[0];
		final Matcher m = DATE_TIME_PATTERN.matcher(dateTimeSpec);
		final LocalDateTime begin = nextDateTime(m, date, dateTimeSpec);
		final LocalDateTime end = nextDateTime(m, date, dateTimeSpec);

		return new CalendarEvent(begin, end, title);
	}

	private LocalDateTime nextDateTime(final Matcher m, final LocalDate defaultDate, final String lineToParse) {
		if (!m.find()) {
			throw new IllegalArgumentException("Bad description:\n" + lineToParse);
		}

		final LocalDate date;
		if (m.group(1) != null) {
			date = LocalDate.of(Integer.parseInt(m.group(4)), parseMonth(m.group(2)),
					Integer.parseInt(m.group(3)));
		} else {
			date = defaultDate;
		}
		final LocalTime time = LocalTime.of(Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));

		return LocalDateTime.of(date, time);
	}

	private int parseMonth(final String month) {
		final String monthUpper = month.toUpperCase();
		for (int i = 0; i < MONTH_SHORT_NAMES.length; ++i) {
			if (monthUpper.startsWith(MONTH_SHORT_NAMES[i])) {
				return i + 1;
			}
		}
		throw new IllegalArgumentException("Not a month: " + month);
	}

	private String getUrlForDate(final LocalDate date) {
		return String.format("https://calendar.google.com/calendar/u/0/r/day/%d/%d/%d", date.getYear(),
				date.getMonthValue(), date.getDayOfMonth());
	}
}
