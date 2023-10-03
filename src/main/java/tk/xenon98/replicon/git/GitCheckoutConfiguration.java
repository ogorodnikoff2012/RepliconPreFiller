package tk.xenon98.replicon.git;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitCheckoutConfiguration {

	public static final String REMOTES_ORIGIN = "remotes/origin";
	@Value("${tk.xenon98.replicon.git.checkoutHistoryPath}")
	protected String checkoutHistoryPath;

	@Bean
	public GitCheckoutHistory gitCheckoutHistory() {
		try (final var reader = new BufferedReader(new InputStreamReader(new FileInputStream(checkoutHistoryPath)))) {
			final var mapper = new ObjectMapper();
			final var events = reader.lines().map(line -> {
				try {
					final Map<String, Object> dict = mapper.readValue(line, Map.class);
					final LocalDateTime timestamp = LocalDateTime.parse((String) dict.get("timestamp"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
					final String oldBranch = preprocessBranchName((String) dict.get("prevHEAD"));
					final String newBranch = preprocessBranchName((String) dict.get("newHEAD"));
					return new GitCheckoutEvent(timestamp, oldBranch, newBranch);
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
			}).toList();
			return new GitCheckoutHistory(events);
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to read checkout history file", e);
		}
	}

	private String preprocessBranchName(final String branchName) {
		if (branchName.startsWith(REMOTES_ORIGIN)) {
			return preprocessBranchName(branchName.substring(REMOTES_ORIGIN.length() + 1));
		}
		return branchName;
	}
}
