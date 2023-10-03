package tk.xenon98.replicon.report;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class Report {
	LocalDate date;
	List<ReportEntry> entries;
	Map<String, Duration> statistics;

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Report for day ").append(date.format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n").append("===\n");

		for (final ReportEntry entry : entries) {
			sb
					.append(entry.getBegin().format(DateTimeFormatter.ISO_LOCAL_TIME))
					.append(" - ")
					.append(entry.getEnd().format(DateTimeFormatter.ISO_LOCAL_TIME))
					.append(": ")
					.append(entry.getDescription())
					.append('\n');
		}

		sb.append("===\n").append("Statistics:\n");

		for (final var kv : statistics.entrySet()) {
			sb.append(kv.getKey()).append("\t -> ").append(kv.getValue().toString()).append("\n");
		}

		return sb.toString();
	}
}
