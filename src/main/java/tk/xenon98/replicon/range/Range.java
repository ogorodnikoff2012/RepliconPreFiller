package tk.xenon98.replicon.range;

import lombok.Data;

@Data
public class Range<T> {
	final T begin;
	final T end;
}
