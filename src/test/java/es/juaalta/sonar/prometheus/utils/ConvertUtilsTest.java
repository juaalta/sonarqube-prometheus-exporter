package es.juaalta.sonar.prometheus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConvertUtilsTest {

  @Test
  void getDoubleValueTest() {

    Double resultVal = new Double(5.0);
    String strVal = "5.0";

    assertEquals(resultVal, ConvertUtils.getDoubleValue(strVal));

  }

}
