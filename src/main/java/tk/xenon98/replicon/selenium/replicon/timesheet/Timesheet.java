package tk.xenon98.replicon.selenium.replicon.timesheet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import lombok.Data;
import lombok.ToString.Exclude;

@Data
public class Timesheet {
	private final List<Row> rows = new ArrayList<>();
	@Exclude
	private final Map<RowKey, Integer> rowIndex = new HashMap<>();

	public void appendRow(final RowKey key) {
		rows.add(new Row(key));
		rowIndex.put(key, rows.size() - 1);
	}

	public boolean hasRow(final RowKey key) {
		return rowIndex.containsKey(key);
	}

	public void updateValue(final RowKey rowKey, final LocalDate date, final BiFunction<LocalDate, Double, Double> updater) {
		rows.get(rowIndex.get(rowKey)).getValues().compute(date, updater);
	}

	public void updateValue(final RowKey rowKey, final LocalDate date, final Function<Double, Double> updater) {
		updateValue(rowKey, date, (__, oldValue) -> updater.apply(oldValue));
	}

	public void deleteRow(final RowKey key) {
		if (!rowIndex.containsKey(key)) {
			return;
		}
		rows.remove((int) rowIndex.get(key));
		reindex();
	}

	public void clear() {
		rows.clear();
		rowIndex.clear();
	}

	public List<Row> getRows() {
		return freeze(rows);
	}

	private static List<Row> freeze(final List<Row> rows) {
		return rows.stream().map(Row::frozen).toList();
	}

	private void reindex() {
		rowIndex.clear();
		IntStream.range(0, rows.size()).forEach(idx -> rowIndex.put(rows.get(idx).getKey(), idx));
	}
}
