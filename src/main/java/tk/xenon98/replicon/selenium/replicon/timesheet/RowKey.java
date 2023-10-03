package tk.xenon98.replicon.selenium.replicon.timesheet;

import lombok.Value;

@Value
public class RowKey {
	String project;
	String task;
	String activity;
	boolean onSite;
}
