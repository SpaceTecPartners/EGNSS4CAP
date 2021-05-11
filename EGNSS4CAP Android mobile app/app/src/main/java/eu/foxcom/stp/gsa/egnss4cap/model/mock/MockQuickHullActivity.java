package eu.foxcom.stp.gsa.egnss4cap.model.mock;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import eu.foxcom.stp.gsa.egnss4cap.R;

public class MockQuickHullActivity extends AppCompatActivity {

    public static final String TAG = MockQuickHullActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_hull2);

        test();
    }

    public void test() {
        /*
        QuickHull quickHull = new QuickHull();
        List<Point> hull = quickHull.quickHull(new ArrayList<Point>(){{
            add(new Point(8,2));
            add(new Point(5,7));
            add(new Point(2,6));
            add(new Point(7,3));
            add(new Point(6,2));
            add(new Point(4,1));
            add(new Point(2,7));
            add(new Point(8,5));
            add(new Point(1,6));
            add(new Point(6,5));
        }});
        for (Point point : hull) {
            Log.d(TAG, "(" + point.getX() + "; " + point.getY() + ")");
        }
        */
    }
}