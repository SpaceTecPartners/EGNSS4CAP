package eu.foxcom.stp.gsa.egnss4cap.model.mock;

import eu.foxcom.stp.gsa.egnss4cap.model.ExifUtil;

public class ExifMock {
    public static void testOrientation(double pitch, double roll) {
        int orientation = ExifUtil.toExifOrientation(pitch, roll);
        int rotation = ExifUtil.getExifOrientationRotation(orientation);
        rotation = (360 - ((rotation + 270) % 360)) % 360;
        orientation = ExifUtil.toExifOrientation(rotation);
        System.out.println("");
    }
}
