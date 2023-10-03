package tk.xenon98.replicon.git;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import tk.xenon98.replicon.range.Range;
import tk.xenon98.replicon.utils.BinarySearch;

public class GitCheckoutHistory {

	private final List<GitCheckoutEvent> eventList;

	public GitCheckoutHistory(final Collection<GitCheckoutEvent> events) {
		final ArrayList<GitCheckoutEvent> eventList = new ArrayList<>(events);
		eventList.sort(Comparator.comparing(GitCheckoutEvent::getTimestamp));
		// verifyIntegrity(eventList);
		this.eventList = eventList;
	}

	private void verifyIntegrity(final List<GitCheckoutEvent> eventList) {
		IntStream.range(1, eventList.size()).forEach(idx -> {
			final GitCheckoutEvent before = eventList.get(idx - 1);
			final GitCheckoutEvent after = eventList.get(idx);
			if (!Objects.equals(before.getNewBranch(), after.getOldBranch())) {
				throw new IllegalStateException("Missing checkout between " + before + " and " + after);
			}
		});
	}

	public String getBranchAtTime(final LocalDateTime instant) {
		final int idx = BinarySearch.firstSatisfying(this.eventList, evt -> evt.getTimestamp().isAfter(instant));
		return idx == this.eventList.size() ? this.eventList.get(idx - 1).getNewBranch() : this.eventList.get(idx).getOldBranch();
	}

	public List<PeriodBranch> getBranchesDuringPeriod(final LocalDateTime begin, final LocalDateTime end) {
		int firstEventIdx = BinarySearch.firstSatisfying(this.eventList, evt -> evt.getTimestamp().isAfter(begin));

		if (firstEventIdx == this.eventList.size()) {
			return List.of(new PeriodBranch(new Range<>(begin, end), this.eventList.get(firstEventIdx - 1).getNewBranch()));
		}

		String currentBranch = this.eventList.get(firstEventIdx).getOldBranch();
		LocalDateTime currentPeriodBegin = begin;
		final ArrayList<PeriodBranch> result = new ArrayList<>();

		for (int idx = firstEventIdx; idx < this.eventList.size() && this.eventList.get(idx).getTimestamp().isBefore(end); ++idx) {
			final var event = this.eventList.get(idx);
			final String nextBranch = event.getNewBranch();
			final LocalDateTime nextPeriodBegin = event.getTimestamp();
			result.add(new PeriodBranch(new Range<>(currentPeriodBegin, nextPeriodBegin), currentBranch));
			currentBranch = nextBranch;
			currentPeriodBegin = nextPeriodBegin;
		}

		result.add(new PeriodBranch(new Range<>(currentPeriodBegin, end), currentBranch));

		return result;
	}
}
