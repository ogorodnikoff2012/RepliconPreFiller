package tk.xenon98.replicon.utils;

import java.util.List;
import java.util.function.Predicate;

public class BinarySearch {
	private BinarySearch() {}
	
	public static <T> int firstSatisfying(final List<? extends T> list, final Predicate<T> predicate) {
		int left = -1;
		int right = list.size();
		
		while (right - left > 1) {
			int middle = left + (right - left) / 2;
			if (predicate.test(list.get(middle))) {
				right = middle;
			} else {
				left = middle;
			}
		}
		
		return right;
	}
}
