package tk.xenon98.replicon.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NameUtilsTest {

	@Test
	void camelToSnake() {
		Assertions.assertEquals("method_name", NameUtils.camelToSnake("methodName"));
		Assertions.assertEquals("class_name", NameUtils.camelToSnake("ClassName"));
	}
}