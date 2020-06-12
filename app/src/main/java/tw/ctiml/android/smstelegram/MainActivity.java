package tw.ctiml.android.smstelegram;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_SMS = 1;

    private EditText etChatId;
    private EditText etToken;
    private Button btnSave;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkForSmsPermission();

        sharedPref = getSharedPreferences(getString(R.string.pref_name), Context.MODE_PRIVATE);
        etChatId = findViewById(R.id.etChatId);
        etToken = findViewById(R.id.etToken);

        etChatId.setText(sharedPref.getString(getString(R.string.et_chat_id_hint), ""));
        etToken.setText(sharedPref.getString(getString(R.string.et_token_hint), ""));

        btnSave = findViewById(R.id.btnSave);
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

        QueueService.start(this);
    }

    private void checkForSmsPermission() {
        boolean noReceive = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED;
        boolean noRead = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED;

        if (noReceive || noRead) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS},
                    MY_PERMISSIONS_REQUEST_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // For the requestCode, check if permission was granted or not.
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_SMS: {
                if (permissions[0].equalsIgnoreCase(Manifest.permission.RECEIVE_SMS)
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // Permission denied.
                    Toast.makeText(this, "receive "+getString(R.string.failure_permission),
                            Toast.LENGTH_LONG).show();
                }
                if (permissions[1].equalsIgnoreCase(Manifest.permission.READ_SMS)
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // Permission denied.
                    Toast.makeText(this, getString(R.string.failure_permission),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
