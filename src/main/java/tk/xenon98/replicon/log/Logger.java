package tk.xenon98.replicon.log;

import java.util.ResourceBundle;
import java.util.function.Supplier;

public interface Logger extends System.Logger {
	default void trace(final ResourceBundle bundle, final String s, final Object[] args) {
		log(Level.TRACE, bundle, s, args);
	}

	default void trace(final Supplier<String> supplier, final Throwable th) {
		log(Level.TRACE, supplier, th);
	}

	default void trace(final String s, final Object[] args) {
		log(Level.TRACE, s, args);
	}

	default void trace(final ResourceBundle bundle, final String s, final Throwable th) {
		log(Level.TRACE, bundle, s, th);
	}

	default void trace(final String s, final Throwable th) {
		log(Level.TRACE, s, th);
	}

	default void trace(final String s) {
		log(Level.TRACE, s);
	}

	default void trace(final Supplier<String> supplier) {
		log(Level.TRACE, supplier);
	}

	default void trace(final Object obj) {
		log(Level.TRACE, obj);
	}

	default void debug(final ResourceBundle bundle, final String s, final Object[] args) {
		log(Level.DEBUG, bundle, s, args);
	}

	default void debug(final Supplier<String> supplier, final Throwable th) {
		log(Level.DEBUG, supplier, th);
	}

	default void debug(final String s, final Object[] args) {
		log(Level.DEBUG, s, args);
	}

	default void debug(final ResourceBundle bundle, final String s, final Throwable th) {
		log(Level.DEBUG, bundle, s, th);
	}

	default void debug(final String s, final Throwable th) {
		log(Level.DEBUG, s, th);
	}

	default void debug(final String s) {
		log(Level.DEBUG, s);
	}

	default void debug(final Supplier<String> supplier) {
		log(Level.DEBUG, supplier);
	}

	default void debug(final Object obj) {
		log(Level.DEBUG, obj);
	}

	default void info(final ResourceBundle bundle, final String s, final Object[] args) {
		log(Level.INFO, bundle, s, args);
	}

	default void info(final Supplier<String> supplier, final Throwable th) {
		log(Level.INFO, supplier, th);
	}

	default void info(final String s, final Object[] args) {
		log(Level.INFO, s, args);
	}

	default void info(final ResourceBundle bundle, final String s, final Throwable th) {
		log(Level.INFO, bundle, s, th);
	}

	default void info(final String s, final Throwable th) {
		log(Level.INFO, s, th);
	}

	default void info(final String s) {
		log(Level.INFO, s);
	}

	default void info(final Supplier<String> supplier) {
		log(Level.INFO, supplier);
	}

	default void info(final Object obj) {
		log(Level.INFO, obj);
	}

	default void warning(final ResourceBundle bundle, final String s, final Object[] args) {
		log(Level.WARNING, bundle, s, args);
	}

	default void warning(final Supplier<String> supplier, final Throwable th) {
		log(Level.WARNING, supplier, th);
	}

	default void warning(final String s, final Object[] args) {
		log(Level.WARNING, s, args);
	}

	default void warning(final ResourceBundle bundle, final String s, final Throwable th) {
		log(Level.WARNING, bundle, s, th);
	}

	default void warning(final String s, final Throwable th) {
		log(Level.WARNING, s, th);
	}

	default void warning(final String s) {
		log(Level.WARNING, s);
	}

	default void warning(final Supplier<String> supplier) {
		log(Level.WARNING, supplier);
	}

	default void warning(final Object obj) {
		log(Level.WARNING, obj);
	}

	default void error(final ResourceBundle bundle, final String s, final Object[] args) {
		log(Level.ERROR, bundle, s, args);
	}

	default void error(final Supplier<String> supplier, final Throwable th) {
		log(Level.ERROR, supplier, th);
	}

	default void error(final String s, final Object[] args) {
		log(Level.ERROR, s, args);
	}

	default void error(final ResourceBundle bundle, final String s, final Throwable th) {
		log(Level.ERROR, bundle, s, th);
	}

	default void error(final String s, final Throwable th) {
		log(Level.ERROR, s, th);
	}

	default void error(final String s) {
		log(Level.ERROR, s);
	}

	default void error(final Supplier<String> supplier) {
		log(Level.ERROR, supplier);
	}

	default void error(final Object obj) {
		log(Level.ERROR, obj);
	}
}
