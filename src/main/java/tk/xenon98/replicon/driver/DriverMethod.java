package tk.xenon98.replicon.driver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DriverMethod {
	String name() default Driver.INFERRED;
}
