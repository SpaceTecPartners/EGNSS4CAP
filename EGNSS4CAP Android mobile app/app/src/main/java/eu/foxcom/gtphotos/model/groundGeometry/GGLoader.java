package eu.foxcom.gtphotos.model.groundGeometry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.foxcom.gtphotos.R;
import eu.foxcom.gtphotos.model.Requestor;

class GGLoader {
    private GGManager ggManager;

    private Requestor requestor;
    private DecimalFormat decimalFormat;

    public GGLoader(GGManager ggManager, Requestor requestor) {
        this.ggManager = ggManager;
        this.requestor = requestor;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        // ! server neumět česky
        symbols.setDecimalSeparator('.');
        // ! emty char
        symbols.setGroupingSeparator('\u0000');
        this.decimalFormat = new DecimalFormat("#.0###############", symbols);
    }

    void load(GGRegion ggRegion) {
            requestor.requestAuth("https://server/ws/comm_shapes.php", response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String status = jsonObject.getString("status");
                if (!status.equals("ok")) {
                    String errMgs = jsonObject.getString("error_msg");
                    ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                            ggManager.getContext().getString(R.string.map_exceptionAfterDownloadGG) + "\n\n" + errMgs, null);
                    return;
                }
                JSONArray shapes = jsonObject.getJSONArray("shapes");
                List<GGObject> ggObjects = GGObject.createListFromResponse(shapes);
                ggManager.loaderLoadGrounds(ggObjects);
            } catch (JSONException | GGObject.GGParseException e) {
                ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                        ggManager.getContext().getString(R.string.map_exceptionDuringParsingGG), e);
            }
        }, error -> {
            ggManager.exception(ggManager.getContext().getString(R.string.map_unexpectedExceptionGG),
                    ggManager.getContext().getString(R.string.map_exceptionDuringDownloadGG),
                    error);
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("max_lat", decimalFormat.format(ggRegion.getMaxLat()));
                params.put("min_lat", decimalFormat.format(ggRegion.getMinLat()));
                params.put("max_lng", decimalFormat.format(ggRegion.getMaxLng()));
                params.put("min_lng", decimalFormat.format(ggRegion.getMinLng()));
                return params;
            }
        });
    }

}
