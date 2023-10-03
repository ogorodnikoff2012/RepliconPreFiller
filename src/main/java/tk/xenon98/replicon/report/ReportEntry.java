package tk.xenon98.replicon.report;

import java.time.LocalTime;
import lombok.Value;

@Value
public class ReportEntry {
	LocalTime begin;
	LocalTime end;
	String description;
}
