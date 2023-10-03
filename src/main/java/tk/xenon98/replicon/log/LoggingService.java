package tk.xenon98.replicon.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {

	@Value("${tk.xenon98.replicon.log.pageSize}")
	protected Integer pageSize;

	private final Map<String, List<String>> logsByTopic = new ConcurrentHashMap<>();

	public List<LogMessage> getMessages(final String topic, int beginIndex) {
		final List<String> log = getLog(topic);
		beginIndex = Math.max(beginIndex, 0);
		final int endIndex = Math.max(0, Math.min(log.size(), beginIndex + pageSize));
		return IntStream.range(beginIndex, endIndex)
				.mapToObj(idx -> new LogMessage(idx, log.get(idx)))
				.toList();
	}

	public List<String> getTopics() {
		return List.copyOf(logsByTopic.keySet());
	}

	public void log(final String topic, final String message) {
		getLog(topic).add(message);
	}

	public Logger getLogger(final String topic) {
		return new Logger() {
			@Override
			public String getName() {
				return topic;
			}

			@Override
			public boolean isLoggable(final Level level) {
				return true;
			}

			@Override
			public void log(final Level level, final ResourceBundle bundle, final String msg,
					final Throwable thrown) {
				final StringWriter writer = new StringWriter();
				writer.append('[').append(level.getName()).append("] ")
						.append(ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(' ')
						.append(msg).append('\n');
				try (final PrintWriter printWriter = new PrintWriter(writer)) {
					thrown.printStackTrace(printWriter);
				}
				LoggingService.this.log(topic, writer.toString());
			}

			@Override
			public void log(final Level level, final ResourceBundle bundle, final String format,
					final Object... params) {
				final StringWriter writer = new StringWriter();
				writer.append('[').append(level.getName()).append("] ")
						.append(ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(' ')
						.append(String.format(format, params));
				LoggingService.this.log(topic, writer.toString());
			}
		};
	}

	private List<String> getLog(final String topic) {
		return logsByTopic.computeIfAbsent(topic, unused -> new CopyOnWriteArrayList<>());
	}
}
