package tk.xenon98.replicon.log;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogMessage {
	int id;
	String text;
}
