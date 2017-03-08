package tw.ctiml.android.smstelegram;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText etChatId;
    private EditText etToken;
    private Button btnSave;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        etChatId = (EditText) findViewById(R.id.etChatId);
        etToken = (EditText) findViewById(R.id.etToken);

        etChatId.setText(sharedPref.getString(getString(R.string.et_chat_id_hint), ""));
        etToken.setText(sharedPref.getString(getString(R.string.et_token_hint), ""));

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.et_chat_id_hint), etChatId.getText().toString());
                editor.putString(getString(R.string.et_token_hint), etToken.getText().toString());
                editor.apply();
                Toast.makeText(getApplicationContext(), "saved", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
