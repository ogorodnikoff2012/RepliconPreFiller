package tk.xenon98.replicon.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NameUtils {
	private NameUtils() {}

	public static String camelToSnake(final String name) {
		return Arrays.stream(name.split("(?=[A-Z])")).filter(word -> word.length() > 0).map(String::toLowerCase).collect(Collectors.joining("_"));
	}

	public static String sanitizeFilenameSafe(final String text) {
		final StringBuilder sb = new StringBuilder();
		text.codePoints().forEach(cp -> {
			if (isFilenameSafe(cp)) {
				sb.appendCodePoint(cp);
			} else if (cp < 256) {
				sb.append(String.format("_%1$02X", cp));
			} else if (cp < 65536) {
				sb.append(String.format("_%1$04X", cp));
			} else {
				sb.append(String.format("_%1$08X", cp));
			}
		});
		return sb.toString();
	}

	private static boolean isFilenameSafe(final int codePoint) {
		return (codePoint >= 'a' && codePoint <= 'z')
				|| (codePoint >= 'A' && codePoint <= 'Z')
				|| (codePoint >= '0' && codePoint <= '9')
				|| codePoint == '-' || codePoint == '_';
	}
}
