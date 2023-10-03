package tk.xenon98.replicon.report;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;
import tk.xenon98.replicon.git.GitCheckoutHistory;
import tk.xenon98.replicon.git.PeriodBranch;
import tk.xenon98.replicon.range.Range;
import tk.xenon98.replicon.selenium.google.CalendarEvent;
import tk.xenon98.replicon.selenium.google.GoogleCalendarDriver;
import tk.xenon98.replicon.selenium.jira.JiraDriver;
import tk.xenon98.replicon.selenium.jira.Ticket;
import tk.xenon98.replicon.selenium.replicon.timesheet.RowKey;
import tk.xenon98.replicon.selenium.replicon.timesheet.Timesheet;

public class SimpleReportGenerator {

	public static final LocalTime DAY_BEGIN = LocalTime.of(10, 0);
	public static final LocalTime DAY_END = LocalTime.of(18, 0);

	private static final Pattern TICKET_MATCHER = Pattern.compile("PIVOT-\\d*");
	private static final String MEETING = "Operations";
	private static final String NEW_FEATURE_DEV = "New Core Development";
	private static final String SUPPORT = "support client";
	private static final String DEFAULT_PROJECT = "ActivePivot - R&D";
	private static final String DEFAULT_TASK = "No Task";
	private final GitCheckoutHistory gitCheckoutHistory;
	private final JiraDriver jiraDriver;
	private final GoogleCalendarDriver calendarDriver;

	public SimpleReportGenerator(final GitCheckoutHistory gitCheckoutHistory, final JiraDriver jiraDriver, final
			GoogleCalendarDriver calendarDriver) {
		this.gitCheckoutHistory = gitCheckoutHistory;
		this.jiraDriver = jiraDriver;
		this.calendarDriver = calendarDriver;
	}

	public Report generate(final LocalDate date) throws ExecutionException, InterruptedException {
		final List<ReportEntry> reportEntries = new ArrayList<>();

		final List<PeriodActivity> dailyActivities = computeDailyActivities(date);

		final Map<String, Duration> statistics = new HashMap<>();

		for (PeriodActivity activity : dailyActivities) {
			if (activity.getActivity() == ActivityType.MEETING) {
				reportEntries.add(new ReportEntry(
						activity.getPeriod().getBegin().toLocalTime(),
						activity.getPeriod().getEnd().toLocalTime(),
						"MEETING"));

				final var duration = Duration.between(activity.getPeriod().getBegin(), activity.getPeriod().getEnd());
				statistics.compute(MEETING, (k, v) -> v == null ? duration : duration.plus(v));
				continue;
			}

			final List<PeriodBranch> periods = this.gitCheckoutHistory.getBranchesDuringPeriod(activity.getPeriod()
					.getBegin(), activity.getPeriod().getEnd());
			for (final PeriodBranch period : periods) {
				final Ticket ticket = getCorrespondingTicket(period.getBranch());
				reportEntries.add(new ReportEntry(
						period.getPeriod().getBegin().toLocalTime(),
						period.getPeriod().getEnd().toLocalTime(),
						ticket == null
								? "(N/A) " + period.getBranch()
								: ticket.getTicketId() + " " + ticket.getTitle()
				));

				if (ticket != null) {
					final String statisticsKey =
							ticket.getOrigin().equals("R&D Internal") ? NEW_FEATURE_DEV : SUPPORT;
					final Duration duration = Duration.between(period.getPeriod().getBegin(), period.getPeriod()
							.getEnd());
					statistics.compute(statisticsKey, (k, v) -> v == null ? duration : duration.plus(v));
				}
			}
		}

		return new Report(date, reportEntries, statistics);
	}

	private List<PeriodActivity> computeDailyActivities(final LocalDate date)
			throws ExecutionException, InterruptedException {
		@Value
		final class TimelineEvent {
			LocalDateTime instant;
			boolean isOpening;
		}

		final List<CalendarEvent> calendarEvents = calendarDriver.getEventsForDate(date).get();
		final List<TimelineEvent> timeline = new ArrayList<>(calendarEvents.size() * 2 + 2);
		final LocalDateTime dayBegin = LocalDateTime.of(date, DAY_BEGIN);
		final LocalDateTime dayEnd = LocalDateTime.of(date, DAY_END);

		for (final CalendarEvent event : calendarEvents) {
			if (event.getBegin().isAfter(dayEnd) || event.getEnd().isBefore(dayBegin)) {
				continue;
			}

			timeline.add(new TimelineEvent(event.getBegin().isBefore(dayBegin) ? dayBegin : event.getBegin(), true));
			timeline.add(new TimelineEvent(event.getEnd().isAfter(dayEnd) ? dayEnd : event.getEnd(), false));
		}
		timeline.add(new TimelineEvent(dayBegin, false));
		timeline.add(new TimelineEvent(dayEnd, true));

		timeline.sort((lhs, rhs) -> {
			if (!lhs.instant.equals(rhs.instant)) {
				return lhs.instant.compareTo(rhs.instant);
			}
			return (lhs.isOpening ? 1 : 0) - (rhs.isOpening ? 1 : 0);
		});

		final List<PeriodActivity> result = new ArrayList<>();
		LocalDateTime lastPeriodBegin = dayBegin;
		int balance = 1;
		for (final TimelineEvent event : timeline) {
			if (event.isOpening) {
				if (balance == 0 && lastPeriodBegin.isBefore(event.instant)) {
					result.add(new PeriodActivity(new Range<>(lastPeriodBegin, event.instant), ActivityType.DEVELOPMENT));
					lastPeriodBegin = event.instant;
				}
				++balance;
			} else {
				--balance;
				if (balance == 0 && lastPeriodBegin.isBefore(event.instant)) {
					result.add(new PeriodActivity(new Range<>(lastPeriodBegin, event.instant), ActivityType.MEETING));
					lastPeriodBegin = event.instant;
				}
			}
		}

		return result;
	}

	private Ticket getCorrespondingTicket(final String branch) throws ExecutionException, InterruptedException {
		final Matcher m = TICKET_MATCHER.matcher(branch);
		if (m.find()) {
			return jiraDriver.getTicket(m.group()).get();
		} else {
			return null;
		}
	}

	public Timesheet generateTimesheet(final Range<LocalDate> period)
			throws ExecutionException, InterruptedException {
		final Timesheet timesheet = new Timesheet();

		for (LocalDate day = period.getBegin(); day.isBefore(period.getEnd()); day = day.plusDays(1)) {
			final var dayOfWeek = day.getDayOfWeek();
			if (isWeekend(dayOfWeek)) {
				continue;
			}

			final String project = DEFAULT_PROJECT;
			final String task = DEFAULT_TASK;
			final boolean onSite = isOnSite(dayOfWeek);

			final Report report = generate(day);
			for (final var kv : report.getStatistics().entrySet()) {
				final RowKey key = new RowKey(project, task, kv.getKey(), onSite);
				final Double value = kv.getValue().getSeconds() / 3600.0;
				if (!timesheet.hasRow(key)) {
					timesheet.appendRow(key);
				}
				timesheet.updateValue(key, day, __ -> value);
			}
		}

		return timesheet;
	}

	private static boolean isWeekend(final DayOfWeek dayOfWeek) {
		return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
	}

	private boolean isOnSite(final DayOfWeek dayOfWeek) {
		return dayOfWeek == DayOfWeek.TUESDAY || dayOfWeek == DayOfWeek.THURSDAY;
	}
}
