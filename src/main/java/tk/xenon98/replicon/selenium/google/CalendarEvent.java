package tk.xenon98.replicon.selenium.google;

import java.time.LocalDateTime;
import lombok.Value;

@Value
public class CalendarEvent {
	LocalDateTime begin;
	LocalDateTime end;
	String title;
}
