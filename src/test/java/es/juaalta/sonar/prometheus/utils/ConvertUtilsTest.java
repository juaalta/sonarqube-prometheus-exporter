package es.juaalta.sonar.prometheus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConvertUtilsTest {

  @Test
  void getDoubleValueTest() {

    Double resultVal = Double.valueOf(5.0);
    Double resultVal0 = Double.valueOf(0);
    String strVal = "5.0";

    assertEquals(resultVal, ConvertUtils.getDoubleValue(strVal));
    strVal = "5";
    assertEquals(resultVal, ConvertUtils.getDoubleValue(strVal));

    strVal = "";
    assertEquals(resultVal0, ConvertUtils.getDoubleValue(strVal));
    strVal = "any string";
    assertEquals(resultVal0, ConvertUtils.getDoubleValue(strVal));

    strVal = null;
    assertEquals(resultVal0, ConvertUtils.getDoubleValue(strVal));

  }

  @Test
  void isNumericTest() {

    assertTrue(ConvertUtils.isNumeric("22"));
    assertTrue(ConvertUtils.isNumeric("5.05"));
    assertTrue(ConvertUtils.isNumeric("-200"));
    assertTrue(ConvertUtils.isNumeric("10.0d"));
    assertTrue(ConvertUtils.isNumeric("   22   "));

    assertFalse(ConvertUtils.isNumeric(null));
    assertFalse(ConvertUtils.isNumeric(""));
    assertFalse(ConvertUtils.isNumeric("abc"));

  }

}
