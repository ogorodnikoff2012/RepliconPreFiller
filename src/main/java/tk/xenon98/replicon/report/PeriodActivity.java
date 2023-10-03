package tk.xenon98.replicon.report;

import java.time.LocalDateTime;
import lombok.Value;
import tk.xenon98.replicon.range.Range;

@Value
public class PeriodActivity {
	Range<LocalDateTime> period;
	ActivityType activity;
}
