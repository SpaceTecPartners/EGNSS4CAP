package eu.foxcom.gtphotos.model;

import org.apache.commons.math3.fraction.Fraction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

public class ExifUtil {
    public static final String DATETIME_FORMAT = "yyyy:MM:dd HH:mm:ss";

    public static String toDateTime(DateTime dateTime) {
        return dateTime.toString(DateTimeFormat.forPattern(DATETIME_FORMAT));
    }

    public static DateTime fromDateTime(String dateTime, DateTimeZone zone) {
        return DateTimeFormat.forPattern(DATETIME_FORMAT).withZone(zone).parseDateTime(dateTime);
    }

    public static String toGPSFormat(double degrees) {
        if (degrees < 0) {
            degrees *= -1;
        }
        int deg = (int) Math.floor(degrees);
        double minDec = (degrees - deg) * 60;
        int min = (int) Math.floor(minDec);
        double secDec = (minDec - min) * 60;
        Fraction secFraction = new Fraction(secDec);
        String ret = deg + "/1," + min + "/1," + secFraction.getNumerator() + "/" + secFraction.getDenominator() + "";
        return ret;
    }

    public static String toRational64u(double number) {
        Fraction altitudeFraction = new Fraction(number);
        return altitudeFraction.getNumerator() + "/" + altitudeFraction.getDenominator();
    }

    public static double fromRational64u(String number) {
        String[] frac = number.split("/");
        return Double.valueOf(frac[0]) / Double.valueOf(frac[1]);
    }

    public static String toLatitudeRef(double latitude) {
        if (latitude >= 0) {
            return "N";
        } else {
            return "S";
        }
    }

    public static String toLongitudeRef(double longitude) {
        if (longitude >= 0) {
            return "E";
        } else {
            return "W";
        }
    }

    public static String toAltitudeRef(double altitude) {
        if (altitude >= 0) {
            return "1";
        } else {
            return "0";
        }
    }

    public static int toExifOrientation(double pitch, double roll) {
        double pitchAbs = pitch >= 270 ? Math.abs(270 - pitch) : Math.abs(90 - pitch);
        double rollAbs = roll >= 180 ? Math.abs(270 - roll) : Math.abs(90 - roll);
        if (pitchAbs <= rollAbs) {
            if (pitch >= 270 && pitch <= 360) {
                // top, left side
                return 1;
            } else {
                // bottom, right side
                return 3;
            }
        } else {

            if (roll >= 0 && roll <= 180) {
                // left side, bottom
                return 6;
            } else {
                // right side, top
                return 8;
            }
        }
    }

    public static int toExifOrientation(int rotation) {
        switch (rotation) {
            case 0:
                return 1;
            case 180:
                return 3;
            case 270:
                return 8;
            case 90:
                return 6;
            default:
                return 0;
        }
    }

    public static int getExifOrientationRotation(int orientation) {
        switch (orientation) {
            case 1:
                return 0;
            case 3:
                return 180;
            case 8:
                return 270;
            case 6:
                return 90;
            default:
                return 0;
        }
    }
}
