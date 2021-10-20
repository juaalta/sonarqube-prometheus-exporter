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

    Double value = Double.valueOf(0);
    if ((strValue != null) && (ConvertUtils.isNumeric(strValue)) && (!"".equals(strValue))) {
      value = Double.valueOf(strValue);
    }
    return value;
  }

  /**
   *
   * If these method don't throw any NumberFormatException, then it means that the parsing was successful and the String
   * is numeric
   *
   * @param strNum String to test
   * @return If the string is a number.
   */
  public static boolean isNumeric(String strNum) {

    if (strNum == null) {
      return false;
    }
    try {
      Double.parseDouble(strNum);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

}
