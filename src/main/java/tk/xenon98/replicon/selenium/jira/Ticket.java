package tk.xenon98.replicon.selenium.jira;

import lombok.Value;

@Value
public class Ticket {
	String ticketId;
	String title;
	String origin;
}
