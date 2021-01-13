package eu.foxcom.gnss_scan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

abstract class Holder {

    protected JSONObject toJSONObject_debug() throws IllegalAccessException, JSONException {
        JSONObject jsonObject = new JSONObject();
        Field[] fields = this.getClass().getFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String name = field.getName();
            Class type = field.getType();
            Object value = field.get(this);
            if (Holder.class.isAssignableFrom(type)){
                Holder holder = (Holder) value;
                jsonObject.put(name, holder.toJSONObject_debug());
            } else if (List.class.isAssignableFrom(type)) {
                JSONArray jsonArray = new JSONArray();
                List<Holder> list = (List<Holder>) value;
                for (Holder holder : list) {
                    jsonArray.put(holder.toJSONObject_debug());
                }
                jsonObject.put(name, jsonArray);
            } else {
                jsonObject.put(name, field.get(this));
            }
        }
        return jsonObject;
    }

    public abstract JSONObject toJSONObject() throws JSONException;

    // get, set region

    // endregion
}
