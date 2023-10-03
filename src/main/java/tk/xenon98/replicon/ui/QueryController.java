package tk.xenon98.replicon.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tk.xenon98.replicon.git.GitCheckoutHistory;
import tk.xenon98.replicon.git.PeriodBranch;
import tk.xenon98.replicon.log.LogMessage;
import tk.xenon98.replicon.log.LoggingService;
import tk.xenon98.replicon.range.Range;
import tk.xenon98.replicon.report.SimpleReportGenerator;
import tk.xenon98.replicon.selenium.okta.OktaDriver;
import tk.xenon98.replicon.selenium.replicon.RepliconDriver;
import tk.xenon98.replicon.selenium.replicon.timesheet.Timesheet;

@RestController
@RequestMapping(path = "/api")
public class QueryController {

	protected final LoggingService loggingService;
	protected final GitCheckoutHistory gitCheckoutHistory;
	protected final SimpleReportGenerator simpleReportGenerator;
	private final RepliconDriver repliconDriver;
	private final OktaDriver oktaDriver;

	public QueryController(
			final LoggingService loggingService,
			final GitCheckoutHistory gitCheckoutHistory,
			final SimpleReportGenerator simpleReportGenerator,
			final RepliconDriver repliconDriver,
			final OktaDriver oktaDriver) {
		this.loggingService = loggingService;
		this.gitCheckoutHistory = gitCheckoutHistory;
		this.simpleReportGenerator = simpleReportGenerator;
		this.repliconDriver = repliconDriver;
		this.oktaDriver = oktaDriver;
	}

	@GetMapping(path = "/logs/{topic}")
	public List<LogMessage> logs(@PathVariable final String topic, @RequestParam final Integer lastSeen) {
		return loggingService.getMessages(topic, lastSeen + 1);
	}

	@GetMapping(path = "/logs")
	public CompletableFuture<List<String>> logTopics() {
		return CompletableFuture.completedFuture(loggingService.getTopics());
	}

	@GetMapping(path = "/git/branches_during_period")
	public List<PeriodBranch> getBranchesDuringPeriod(
			@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) final LocalDateTime begin,
			@RequestParam @DateTimeFormat(iso = ISO.DATE_TIME) final LocalDateTime end) {
		return gitCheckoutHistory.getBranchesDuringPeriod(begin, end);
	}

	@GetMapping(path = "/report/simple")
	public String getSimpleReport(@RequestParam @DateTimeFormat(iso = ISO.DATE) final LocalDate date) {
		try {
			return simpleReportGenerator.generate(date).toString();
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@PostMapping(path = "/prefill")
	public void prefill() throws ExecutionException, InterruptedException {
		oktaDriver.openApp(RepliconDriver.APP_ID).get();
		final List<Range<LocalDate>> weeks = repliconDriver.retrieveNotSubmittedDates().get();

		for (final Range<LocalDate> week : weeks) {
			final Timesheet timesheet = simpleReportGenerator.generateTimesheet(week);

			repliconDriver.editTimesheet(week.getBegin(), editor -> {
				editor.submitModel(timesheet);
			}).get();
		}
	}
}
