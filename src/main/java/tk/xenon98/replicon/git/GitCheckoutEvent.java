package tk.xenon98.replicon.git;

import java.time.LocalDateTime;
import lombok.Value;

@Value
public class GitCheckoutEvent {
	LocalDateTime timestamp;
	String oldBranch;
	String newBranch;
}
