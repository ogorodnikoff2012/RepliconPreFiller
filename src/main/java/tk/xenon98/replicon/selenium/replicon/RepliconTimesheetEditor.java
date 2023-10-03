package tk.xenon98.replicon.selenium.replicon;

import com.google.common.collect.Streams;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import tk.xenon98.replicon.selenium.replicon.timesheet.Row;
import tk.xenon98.replicon.selenium.replicon.timesheet.RowKey;
import tk.xenon98.replicon.selenium.replicon.timesheet.Timesheet;

@RequiredArgsConstructor
public class RepliconTimesheetEditor {

	private static final Pattern DATE_FROM_TITLEHTML_PATTERN = Pattern.compile("(\\d+)/(\\d+)/(\\d+)");
	public static final String TIMESHEET_ROWS_SELECTOR = ".dataGrid > tbody[sectiontype=rows][section='0'] > tr";

	private final RepliconDriver repliconDriver;
	private final WebDriver webDriver;

	private void checkAccess() {
		if (repliconDriver.getCurrentTimesheetEditor() != this) {
			throw new IllegalCallerException("Editor must be accessed only inside RepliconDriver#edit() context");
		}
	}

	public Timesheet retrieveModel() {
		checkAccess();
		final Timesheet timesheet = new Timesheet();
		final List<LocalDate> dates = discoverDates();
		final List<RawRow> rawRows = retrieveRawData();

		for (final var rawRow : rawRows) {
			final String[] projectAndTaskSplitted = rawRow.projectAndTask.split("\n");
			final String project = projectAndTaskSplitted[0];
			final String task = projectAndTaskSplitted[1];
			final boolean onSite = rawRow.site.startsWith("On");

			final RowKey rowKey = new RowKey(project, task, rawRow.activity, onSite);
			timesheet.appendRow(rowKey);

			Streams.forEachPair(dates.stream(), rawRow.values.stream(), (date, value) -> {
				if (value.isEmpty()) {
					return;
				}

				timesheet.updateValue(rowKey, date, __ -> Double.valueOf(value));
			});
		}

		return timesheet;
	}

	public void submitModel(final Timesheet newModel) {
		checkAccess();
		final Timesheet oldModel = retrieveModel();
		try {
			doSubmitModel(newModel);
		} catch (Exception e) {
			e.printStackTrace();
			doSubmitModel(oldModel);
		}
	}

	private void doSubmitModel(final Timesheet model) {
		clearRows();
		for (; ; ) {
			try {
				final List<LocalDate> dates = discoverDates();
				for (final Row row : model.getRows()) {
					appendRow(row, dates);
				}
				ensureSave();
				return;
			} catch (StaleElementReferenceException e) {

			}
		}
	}

	private void ensureSave() {
		new WebDriverWait(webDriver, Duration.ofSeconds(5)).until((__) ->
				webDriver.findElement(By.className("saveIndicator")).getText().equals("Changes have been saved"));
	}

	private void appendRow(final Row row, final List<LocalDate> dates) {
		final var wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.id("add-new-timeline")));
		final int rowId = webDriver.findElements(By.cssSelector(TIMESHEET_ROWS_SELECTOR)).size();
		webDriver.findElement(By.id("add-new-timeline")).click();
		wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector(TIMESHEET_ROWS_SELECTOR), rowId + 1));

		final var tableRow = webDriver.findElements(By.cssSelector(TIMESHEET_ROWS_SELECTOR)).get(rowId);
		enterProjectAndTask(tableRow, row.getKey().getProject(), row.getKey().getTask());
		enterActivity(tableRow, row.getKey().getActivity());
		enterOnSite(tableRow, row.getKey().isOnSite());

		final var valueCells = tableRow.findElements(By.className("day"));
		for (final var kv : row.getValues().entrySet()) {
			final var date = kv.getKey();
			final var value = kv.getValue();
			if (value == null) {
				continue;
			}

			int idx = dates.indexOf(date);
			if (idx < 0) {
				throw new IllegalArgumentException("Bad date: " + date);
			}
			valueCells.get(idx).findElement(By.tagName("input")).sendKeys(value.toString());
		}
	}

	private void enterOnSite(final WebElement tableRow, final boolean onSite) {
		tableRow.findElement(By.className("extensionField")).click();
		final var text = onSite ? "On-Site" : "Off-Site";

		selectMatchingValue(text);
	}

	private void selectMatchingValue(final String text) {
		for (; ; ) {
			final var divDropdown = findDivDropdownContent();
			final var availableValues = divDropdown.findElements(By.tagName("li"));
			if (availableValues.isEmpty()) {
				continue;
			}
			for (final var availableValue : availableValues) {
				if (text.equals(availableValue.getText().strip())) {
					availableValue.click();
					return;
				}
			}
			throw new IllegalArgumentException("Cannot find option: " + text);
		}
	}

	private WebElement findDivDropdownContent() {
		final Supplier<Optional<WebElement>> finder =
				() -> {
					final var divs = webDriver.findElements(By.className("divDropdownContent"))
							.stream()
							.filter(WebElement::isDisplayed)
							.toList();
					return divs.isEmpty() ? Optional.empty() : Optional.of(divs.get(0));
				};
		final var wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
		wait.until(__ -> finder.get().isPresent());
		return finder.get().orElseThrow();
	}

	private void enterActivity(final WebElement tableRow, final String activity) {
		tableRow.findElement(By.className("activity")).click();

		selectMatchingValue(activity);
	}

	private void enterProjectAndTask(final WebElement tableRow, final String project, final String task) {
		final String cssSelector = ".searchAllListContainer li[isdataelement=true]";
		tableRow.findElement(By.className("taskFixedWidth")).click();

		for (; ; ) {
			final var divDropdown = findDivDropdownContent();
			divDropdown.findElement(By.className("searchAll")).click();
			final var availableValues = divDropdown.findElements(By.cssSelector(cssSelector));
			if (availableValues.isEmpty()) {
				continue;
			}
			for (final WebElement availableValue : availableValues) {
				final String valueText = availableValue.getText();
				final String[] lines = valueText.split("\n");
				if (project.equals(lines[0]) && task.equals(lines[1])) {
					availableValue.click();
					return;
				}
			}
			throw new IllegalArgumentException("Cannot find project and task: " + project + ", " + task);
		}

	}

	private void clearRows() {
		final var wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
		webDriver.findElement(By.className("clearall")).click();
		wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[value='Clear Time Entries']")));
		webDriver.findElement(By.cssSelector("[value='Clear Time Entries']")).click();
		wait.until(ExpectedConditions.numberOfElementsToBe(By.cssSelector(TIMESHEET_ROWS_SELECTOR), 0));
	}

	private List<RawRow> retrieveRawData() {
		for (; ; ) {
			try {

				final List<RawRow> rawRows = new ArrayList<>();

				final var tableRows = webDriver.findElements(By.cssSelector(TIMESHEET_ROWS_SELECTOR));
				for (final var tr : tableRows) {
					final String projectAndTask = tr.findElement(By.className("taskFixedWidth")).getText();
					final String activity = tr.findElement(By.className("activity")).getText();
					final String site = tr.findElement(By.className("extensionField")).getText();
					final ArrayList<String> values = new ArrayList<>();
					for (final var cell : tr.findElements(By.className("day"))) {
						values.add(cell.findElement(By.tagName("input")).getAttribute("value"));
					}

					rawRows.add(new RawRow(projectAndTask, activity, site, values));
				}

				return rawRows;

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private List<LocalDate> discoverDates() {
		for (; ; ) {
			try {
				final var dayCells = webDriver.findElements(By.cssSelector(".dataGrid > thead > tr > th.day"));
				final var result =
						dayCells.stream().map(cell -> cell.getAttribute("titlehtml"))
								.map(this::parseDateFromTitleHtml)
								.toList();
				if (!result.isEmpty()) {
					return result;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private LocalDate parseDateFromTitleHtml(final String titleHtml) {
		final Matcher m = DATE_FROM_TITLEHTML_PATTERN.matcher(titleHtml);
		if (!m.find()) {
			throw new IllegalArgumentException("Cannot parse date from title: " + titleHtml);
		}
		final int year = Integer.parseInt(m.group(1));
		final int month = Integer.parseInt(m.group(2));
		final int day = Integer.parseInt(m.group(3));
		return LocalDate.of(year, month, day);
	}

	@Value
	private static class RawRow {

		String projectAndTask;
		String activity;
		String site;
		List<String> values;
	}
}
