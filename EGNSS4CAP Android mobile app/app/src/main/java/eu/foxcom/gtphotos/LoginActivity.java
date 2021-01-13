package eu.foxcom.gtphotos;

import android.content.Intent;
import android.os.Bundle;
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

import eu.foxcom.gtphotos.model.LoggedUser;
import eu.foxcom.gtphotos.model.Requestor;
import eu.foxcom.gtphotos.model.mock.UserMock;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
    }

    private void showMessage(String message) {
        TextView messageTextView = findViewById(R.id.lg_textView_msg);
        messageTextView.setText(message);
    }

    @Override
    protected void refreshTasks(Intent intent) {
        if (true) {
            super.refreshTasks(intent);
            return;
        }
        if(intent.getBooleanExtra(MainService.BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)) {
            goToMainActivity();
        } else {
            endLogin(getString(R.string.lg_loginFailed, intent.getStringExtra(MainService.BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID)));
        }
    }

    public void tryLogin(View view) {
        beginLogin();
        final TextView loginTextView = findViewById(R.id.lg_textInputEditText_login);
        final TextView passwordTextView = findViewById(R.id.lg_textInputEditText_password);
        MS.getRequestor().requestAuth("https://server/ws/comm_login.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
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
                        endLogin(getString(R.string.lg_loginFailed, errorMsg));
                        return;
                    } else {
                        loggedUser = LoggedUser.createFromResponse(jsonObject.getJSONObject("user"), loginTextView.getText().toString(), new DateTime());
                    }
                    LoggedUser.login(MS.getAppDatabase(), loggedUser);
                    MS.syncAll();
                    goToMainActivity();
                } catch (JSONException e) {
                    endLogin(getString(R.string.lg_loginFailed, e.getMessage()));
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                endLogin(getString(R.string.lg_loginFailed, error.getMessage()));
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

    private void endLogin(String msg) {
        showMessage(msg);
        final ProgressBar progressBar = findViewById(R.id.lg_progressBar);
        progressBar.setVisibility(View.GONE);
    }
}
