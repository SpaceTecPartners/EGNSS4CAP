package eu.foxcom.gtphotos.model.groundGeometry;

import android.content.Context;

import com.android.volley.VolleyError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import eu.foxcom.gtphotos.model.Util;

public class GGManager {
    private GGMapActivity ggMapActivity;
    private Context context;

    private GGWatcher ggWatcher;
    private GGLoader ggLoader;
    private Object ggObjectsMutex = new Object();
    private List<GGObject> ggObjects = new ArrayList<>();
    private GGDrawer ggDrawer;

    public GGManager(GGMapActivity ggMapActivity) {
        this.ggMapActivity = ggMapActivity;
        this.context = (Context) ggMapActivity;

        this.ggWatcher = new GGWatcher(this);
        this.ggLoader = new GGLoader(this, ggMapActivity.getRequestor());
        this.ggDrawer = new GGDrawer(this);
    }

    void watcherDrawGrounds(GGRegion ggRegion) {
        if (Util.isInternetAvailable()) {
            ggLoader.load(ggRegion);
        }
    }

    void watcherHideGrounds() {
        synchronized (ggObjectsMutex) {
            deleteGrounds();
            drawGrounds();
        }
    }

    void loaderLoadGrounds(List<GGObject> ggObjects) {
        synchronized (ggObjectsMutex) {
            deleteGrounds();
            this.ggObjects = ggObjects;
            drawGrounds();
        }
    }

    private void deleteGrounds() {
        synchronized (ggObjectsMutex) {
            ggDrawer.delete();
            ggObjects.clear();
        }
    }

    private void drawGrounds() {
        synchronized (ggObjectsMutex) {
            ggDrawer.draw();
        }
    }

    void exception(String title, String message, Exception exception) {
        String netMsg = null;
        if (exception != null && exception instanceof VolleyError && ((VolleyError) exception).networkResponse != null) {
            netMsg = "Network status code: " + ((VolleyError) exception).networkResponse.statusCode;
        }
        String text;
        if (exception != null) {
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            text = message + "\n\n" + exception.getMessage() + "\n\n" + stringWriter.toString();
        } else {
            text = message;
        }
        if (netMsg != null) {
            text += "\n\n" + netMsg;
        }
        ggMapActivity.alert(title, text);
    }


    // region get, set

    GGMapActivity getGgMapActivity() {
        return ggMapActivity;
    }

    Context getContext() {
        return context;
    }

    List<GGObject> getGgObjects() {
        return ggObjects;
    }

    public Object getGgObjectsMutex() {
        return ggObjectsMutex;
    }

    // endregion

}

/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
