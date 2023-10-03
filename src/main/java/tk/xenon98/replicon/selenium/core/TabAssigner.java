package tk.xenon98.replicon.selenium.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

public class TabAssigner {
	private final Set<String> bindedHandles = new HashSet<>();
	private final Map<String, String> tabToHandle = new HashMap<>();
	private final Map<String, String> handleToTab = new HashMap<>();

	private final WebDriver webDriver;

	public TabAssigner(final WebDriver webDriver) {
		this.webDriver = webDriver;
	}

	public synchronized String getTab(String tabId) {
		if (this.tabToHandle.containsKey(tabId)) {
			return this.tabToHandle.get(tabId);
		}

		final String newHandle = allocateHandle();
		bindTabAndHandle(tabId, newHandle);
		return newHandle;
	}

	public synchronized void bindTabAndHandle(final String tabId, final String newHandle) {
		this.bindedHandles.add(newHandle);
		this.handleToTab.put(newHandle, tabId);
		this.tabToHandle.put(tabId, newHandle);
	}

	private String allocateHandle() {
		final Set<String> handles = this.webDriver.getWindowHandles();
		if (handles.size() > this.bindedHandles.size()) {
			for (final String handle : handles) {
				if (!this.bindedHandles.contains(handle)) {
					return handle;
				}
			}
			throw new IllegalStateException("Unreachable");
		}
		return this.webDriver.switchTo().newWindow(WindowType.TAB).getWindowHandle();
	}

	public synchronized boolean hasTab(final String tabId) {
		return this.tabToHandle.containsKey(tabId);
	}
}
