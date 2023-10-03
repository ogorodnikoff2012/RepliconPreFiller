package tk.xenon98.replicon.selenium.replicon.timesheet;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Value;

@Value
public class Row {
	RowKey key;
	Map<LocalDate, Double> values;

	private Row(final RowKey key, final Map<LocalDate, Double> values) {
		this.key = key;
		this.values = values;
	}

	public Row(final RowKey key) {
		this(key, new HashMap<>());
	}

	public Row frozen() {
		return new Row(key, Collections.unmodifiableMap(values));
	}
}
