package eu.foxcom.gnss_scan;

class Util {
    static Integer parseNullableInteger(String s) {
        if (s == null) {
            return null;
        }
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}
