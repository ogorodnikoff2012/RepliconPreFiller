package tk.xenon98.replicon.driver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Future;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition;
import net.bytebuddy.dynamic.DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable;
import net.bytebuddy.implementation.MethodCall;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.function.ThrowingFunction;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tk.xenon98.replicon.log.Logger;
import tk.xenon98.replicon.log.LoggingService;
import tk.xenon98.replicon.utils.NameUtils;

@Service
public class DriverRegistrationService {

	private final Logger logger;
	private final RequestMappingHandlerMapping requestMappingHandlerMapping;
	private final ConfigurableApplicationContext applicationContext;

	public DriverRegistrationService(final List<Driver> drivers, final LoggingService loggingService, final RequestMappingHandlerMapping requestMappingHandlerMapping, final
		ConfigurableApplicationContext applicationContext) {
		this.logger = loggingService.getLogger(this.getClass().getName());
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
		this.applicationContext = applicationContext;

		for (final Driver driver : drivers) {
			registerDriver(driver);
		}
	}

	public void registerDriver(final Driver driver) {
		logger.info("Registering driver " + driver.getClass().getName());
		final String driverName = getDriverName(driver);
		final String driverUri = buildDriverUri(driverName);

		final Class<?> driverClass = driver.getClass();
		Map<String, Method> driverMethods = new HashMap<>();
		for (final Method method : driverClass.getMethods()) {
			final DriverMethod annotation = method.getAnnotation(DriverMethod.class);
			if (annotation == null) {
				continue;
			}

			final String methodName = getDriverMethodName(method, annotation);

			logger.info("Discovered method " + methodName);
			driverMethods.put(driverUri + "/" + methodName, method);
		}

		final Object driverController = generateDriverController(driver, driverMethods);
		registerNewController(driverController);

		logger.info("Driver is registered as " + driverUri);
	}

	private void registerNewController(final Object driverController) {
		applicationContext.getBeanFactory().registerSingleton(NameUtils.sanitizeFilenameSafe(NameUtils.camelToSnake(driverController.getClass().getName())), driverController);

		applicationContext.getBeansOfType(RequestMappingHandlerMapping.class).forEach((name, requestMappingHandlerMapping) -> {
			requestMappingHandlerMapping.getHandlerMethods().keySet().forEach(requestMappingHandlerMapping::unregisterMapping);
			requestMappingHandlerMapping.afterPropertiesSet();
		});
	}

	private Object generateDriverController(final Driver driver, final Map<String, Method> driverMethods) {
		final Class<?> driverClass = driver.getClass();
		var driverControllerBuilder = new ByteBuddy()
				.subclass(Object.class)
				.name(String.format("%s$%s_%08X", getClass().getName(), driverClass.getSimpleName(), driverClass.hashCode()))
				.annotateType(AnnotationDescription.Builder.ofType(RestController.class).build());

		for (final Entry<String, Method> kv : driverMethods.entrySet()) {
			final String url = kv.getKey();
			final Method method = kv.getValue();

			final ResultTransformer resultTransformer = new ResultTransformer(driver, method, getTransformerFunction(method.getReturnType()));

			ParameterDefinition<Object> builderWithMethod =
			driverControllerBuilder.defineMethod(method.getName(), Object.class, Modifier.PUBLIC);

			for (final Parameter p : method.getParameters()) {
				var builderWithParameter =
						builderWithMethod.withParameter(p.getType(), p.getName())
								.annotateParameter(p.getAnnotations())
								.annotateParameter(AnnotationDescription.Builder.ofType(RequestParam.class).build());

				builderWithParameter = addTypeSpecificParameterAnnotations(builderWithParameter, p.getType());
				builderWithMethod = builderWithParameter;
			}

			driverControllerBuilder =
			builderWithMethod
					.intercept(MethodCall.invoke(ResultTransformer.INTERCEPT_METHOD).on(resultTransformer).withArgumentArray().withOwnType())
					.annotateMethod(AnnotationDescription.Builder.ofType(PostMapping.class).defineArray("path", url)
							.defineArray("produces", MediaType.APPLICATION_JSON_VALUE).build());
		}

		try {
			return driverControllerBuilder.make().load(getClass().getClassLoader()).getLoaded().newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Annotatable<Object> addTypeSpecificParameterAnnotations(
			Annotatable<Object> builderWithParameter, final Class<?> type) {
		if (LocalDate.class.isAssignableFrom(type)) {
			builderWithParameter = builderWithParameter.annotateParameter(AnnotationDescription.Builder.ofType(
					DateTimeFormat.class).define("iso", ISO.DATE).build());
		} else if (LocalDateTime.class.isAssignableFrom(type)) {
			builderWithParameter = builderWithParameter.annotateParameter(AnnotationDescription.Builder.ofType(
					DateTimeFormat.class).define("iso", ISO.DATE_TIME).build());
		}
		return builderWithParameter;
	}

	private ThrowingFunction<?, ?> getTransformerFunction(final Class<?> resultType) {
		if (Future.class.isAssignableFrom(resultType)) {
			return (ThrowingFunction<Future<?>, Object>) Future::get;
		}
		return (Object x) -> x;
	}

	private static String getDriverMethodName(final Method method, final DriverMethod annotation) {
		return NameUtils.sanitizeFilenameSafe(
				Objects.equals(annotation.name(), Driver.INFERRED)
						? NameUtils.camelToSnake(method.getName())
						: annotation.name());
	}

	private String buildDriverUri(final String driverName) {
		return "/api/drivers/" + driverName;
	}

	private String getDriverName(final Driver driver) {
		return NameUtils.sanitizeFilenameSafe(getUnverifiedDriverName(driver));
	}

	private String getUnverifiedDriverName(final Driver driver) {
		if (driver instanceof NamedDriver namedDriver) {
			return namedDriver.getDriverName();
		}
		return NameUtils.camelToSnake(driver.getClass().getSimpleName());
	}

}
