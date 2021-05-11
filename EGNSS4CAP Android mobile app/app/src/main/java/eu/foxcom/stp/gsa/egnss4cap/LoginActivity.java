package eu.foxcom.stp.gsa.egnss4cap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import eu.foxcom.stp.gsa.egnss4cap.model.LoggedUser;
import eu.foxcom.stp.gsa.egnss4cap.model.Requestor;
import eu.foxcom.stp.gsa.egnss4cap.model.mock.UserMock;

public class LoginActivity extends BaseActivity {

    public final String TAG = LoginActivity.class.getName();

    private boolean isServerLastConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setContentView(R.layout.activity_login);

    }

    @Override
    public void serviceInit() {
        super.serviceInit();
    }

    private void showMessage(String message) {
        TextView messageTextView = findViewById(R.id.lg_textView_msg);
        if (message != null && !message.isEmpty()) {
            messageTextView.setVisibility(View.VISIBLE);
        } else {
            messageTextView.setVisibility(View.GONE);
        }
        messageTextView.setText(message);
    }

    public void tryLogin(View view) {
        beginLogin();
        isServerLastConnected = false;
        final TextView loginTextView = findViewById(R.id.lg_textInputEditText_login);
        final TextView passwordTextView = findViewById(R.id.lg_textInputEditText_password);
        MS.getRequestor().requestAuth("https://egnss4cap-uat.foxcom.eu/ws/comm_login.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                isServerLastConnected = true;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status").trim();
                    String login = loginTextView.getText().toString();
                    String password = passwordTextView.getText().toString();
                    LoggedUser loggedUser;
                    if (login.equals("mockUser") && password.equals("mocking")) {
                        loggedUser = UserMock.createUserMock();
                    } else if (!status.equals("ok")) {
                        String errorMsg = jsonObject.getString("error_msg");
                        endLogin();
                        return;
                    } else {
                        loggedUser = LoggedUser.createFromResponse(jsonObject.getJSONObject("user"), loginTextView.getText().toString(), new DateTime());
                    }
                    LoggedUser.login(MS.getAppDatabase(), loggedUser);
                    MS.syncAll();
                    goToMainActivity();
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                    endLogin();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                endLogin();
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("login", loginTextView.getText().toString());
                params.put("pswd", passwordTextView.getText().toString());
                return params;
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MS.getMainClass());
        startActivity(intent);
        finish();
    }

    private void beginLogin() {
        showMessage("");
        final ProgressBar progressBar = findViewById(R.id.lg_progressBar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private String createErrMsg() {
        String errMsg;
        if (isServerLastConnected) {
           errMsg = getString(R.string.lg_loginFailed, getString(R.string.lg_loginFailedWrongData));
        } else {
            errMsg = getString(R.string.lg_loginFailed, getString(R.string.lg_loginFailedNoServer));
        }
        return errMsg;
    }

    private void endLogin() {
        showMessage(createErrMsg());
        final ProgressBar progressBar = findViewById(R.id.lg_progressBar);
        progressBar.setVisibility(View.GONE);
    }
}
/**
 * Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
 */
