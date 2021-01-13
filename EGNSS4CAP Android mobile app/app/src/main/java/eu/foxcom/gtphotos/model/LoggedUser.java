package eu.foxcom.gtphotos.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

@Entity
public class LoggedUser {

    public static final int LOGIN_MAX_TIME_SEC = -1; // 3 h

    public static final String ID = "id";
    public static final String LOGIN = "login";
    public static final String NAME = "name";
    public static final String SURNAME = "surname";
    public static final String ID_NUMBER = "idNumber";
    public static final String EMAIL = "email";
    public static final String VAT = "VAT";
    public static final String LAST_LOGGED = "lastLogged";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = ID)
    private String id;
    @ColumnInfo(name = LOGIN)
    private String login;
    @ColumnInfo(name = NAME)
    private String name;
    @ColumnInfo(name = SURNAME)
    private String surname;
    @ColumnInfo(name = ID_NUMBER)
    private String identificationNumber;
    @ColumnInfo(name = EMAIL)
    private String email;
    @ColumnInfo(name = VAT)
    private String vat;
    @ColumnInfo(name = LAST_LOGGED)
    private DateTime lastLogged;

    public static LoggedUser createFromResponse(JSONObject jsonObject, String login, DateTime lastLogged) throws JSONException {
        jsonObject.put(LOGIN, login);
        jsonObject.put(LAST_LOGGED, lastLogged.toDate().getTime());
        return new LoggedUser(jsonObject);
    }

    public static LoggedUser createFromLocal(JSONObject jsonObject) throws JSONException {
        return new LoggedUser(jsonObject);
    }

    public static LoggedUser createFromAppDatabase(AppDatabase appDatabase) {
        return appDatabase.loggedUserDao().loadLoggedUser();
    }

    public static boolean isLogged(AppDatabase appDatabase) {
        LoggedUser loggedUser = appDatabase.loggedUserDao().loadLoggedUser();
        if (loggedUser == null) {
            return false;
        }
        if (LOGIN_MAX_TIME_SEC < 0) {
            return true;
        }
        Duration duration = new Duration(loggedUser.lastLogged, DateTime.now());
        return duration.getStandardSeconds() <= LOGIN_MAX_TIME_SEC;
    }

    public static void login(AppDatabase appDatabase, LoggedUser loggedUser) {
        appDatabase.loggedUserDao().refreshLoggedUser(loggedUser);
    }

    public static void logout(AppDatabase appDatabase) {
        appDatabase.loggedUserDao().deleteLoggedUser();
    }

    private LoggedUser(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getString(ID);
        login = jsonObject.getString(LOGIN);
        name = Util.JSONgetStringNullable(jsonObject, NAME);
        surname = Util.JSONgetStringNullable(jsonObject, SURNAME);
        identificationNumber = Util.JSONgetStringNullable(jsonObject, ID_NUMBER);
        email = Util.JSONgetStringNullable(jsonObject, EMAIL);
        vat = Util.JSONgetStringNullable(jsonObject, VAT);
        lastLogged = new DateTime(new Date(jsonObject.getLong(LAST_LOGGED)));
    }

    public LoggedUser() {

    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(ID, id);
        jsonObject.put(LOGIN, login);
        jsonObject.put(NAME, name);
        jsonObject.put(SURNAME, surname);
        jsonObject.put(ID_NUMBER, identificationNumber);
        jsonObject.put(EMAIL, email);
        jsonObject.put(VAT, vat);
        jsonObject.put(LAST_LOGGED, lastLogged.toDate().getTime());
        return jsonObject;
    }

    // region get, set

    public String getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getVat() {
        return vat;
    }

    public DateTime getLastLogged() {
        return lastLogged;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVat(String vat) {
        this.vat = vat;
    }

    public void setLastLogged(DateTime lastLogged) {
        this.lastLogged = lastLogged;
    }

    // endregion
}
