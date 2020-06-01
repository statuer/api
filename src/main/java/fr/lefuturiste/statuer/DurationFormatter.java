package fr.lefuturiste.statuer;

import java.time.Duration;

public class DurationFormatter {

  public static String format(Duration duration) {
    if (Duration.ZERO.equals(duration)) {
      return "0";
    } else {
      long seconds = duration.getSeconds();
      long days = seconds / (24 * 3_600);
      long hours = seconds / 3_600 % 24;
      long minutes = seconds / 60 % 60;
      // int millis = duration.getNano() / 1_000_000;
      // int nanos = duration.getNano() % 1_000_000;

      StringBuilder formattedDuration = new StringBuilder();
      appendTimeUnit(formattedDuration, days, "days", "day");
      appendTimeUnit(formattedDuration, hours, "hours", "hour");
      appendTimeUnit(formattedDuration, minutes, "minutes", "minute");
      appendTimeUnit(formattedDuration, seconds % 60, "seconds", "second");
      // appendTimeUnit( formattedDuration, millis, "milliseconds", "millisecond" );
      // appendTimeUnit( formattedDuration, nanos, "nanoseconds", "nanosecond" );

      return formattedDuration.toString();
    }
  }

  private static void appendTimeUnit(StringBuilder sb, long number, String pluralLabel, String singularLabel) {
    if (number == 0) {
      return;
    }
    if (sb.length() > 0) {
      sb.append(" ");
    }
    sb.append(number).append(" ").append(number == 1 ? singularLabel : pluralLabel);
  }
}