package tk.xenon98.replicon.driver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.springframework.util.function.ThrowingFunction;

public class ResultTransformer {

	public static final Method INTERCEPT_METHOD;

	static {
		try {
			INTERCEPT_METHOD = ResultTransformer.class.getMethod("intercept", Object[].class, Class.class);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	private final Driver driver;
	private final Method method;
	private final ThrowingFunction<Object, Object> transform;

	public ResultTransformer(final Driver driver, final Method method, final ThrowingFunction<?, ?> transform) {
		this.driver = driver;
		this.method = method;
		this.transform = (ThrowingFunction<Object, Object>) transform;
	}

	public Type getReturnType() {
		return this.method.getReturnType();
	}

	public <T> T intercept(final Object[] args, final Class<T> clazz) throws Throwable {
		try {
			return (T) transform.apply(method.invoke(driver, args));
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}
}
