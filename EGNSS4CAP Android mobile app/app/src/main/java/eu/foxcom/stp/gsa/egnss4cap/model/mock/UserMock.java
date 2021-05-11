package eu.foxcom.stp.gsa.egnss4cap.model.mock;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import eu.foxcom.stp.gsa.egnss4cap.model.LoggedUser;

public class UserMock {
    public static LoggedUser createUserMock() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(LoggedUser.ID, "111");
        jsonObject.put(LoggedUser.NAME, "Mock");
        jsonObject.put(LoggedUser.SURNAME, "MockUser");
        jsonObject.put(LoggedUser.ID_NUMBER, "0987");
        jsonObject.put(LoggedUser.EMAIL, "mock@mail.com");
        jsonObject.put(LoggedUser.VAT, null);
        return LoggedUser.createFromResponse(jsonObject, "mockUser", DateTime.now());
    }
}
