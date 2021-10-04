/**
 *
 */
package es.juaalta.sonar.prometheus.utils;

/**
 * @author vagrant
 *
 */
public class ConvertUtils {

  private ConvertUtils() {

  };

  /**
   * Get the Double value from a string.
   *
   * @param strValue string to convert to double
   * @return Double with the value of strValue.
   */
  public static Double getDoubleValue(String strValue) {

    Double value;
    if (!"".equals(strValue)) {
      value = Double.valueOf(strValue);
    } else {
      value = Double.valueOf(0);
    }
    return value;
  }

}
