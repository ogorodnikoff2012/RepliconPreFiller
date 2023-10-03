package tk.xenon98.replicon.git;

import java.time.LocalDateTime;
import lombok.Value;
import tk.xenon98.replicon.range.Range;

@Value
public class PeriodBranch {
	Range<LocalDateTime> period;
	String branch;
}
