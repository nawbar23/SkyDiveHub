package com.skydive.skydivehub;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.skydive.skydivehub.hub.CommInterface;
import com.skydive.skydivehub.hub.Hub;
import com.skydive.skydivehub.internet.TcpClient;
import com.skydive.skydivehub.internet.TcpServer;

import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements Hub.Listener {

    private static final String DEBUG_TAG = MainActivity.class.getSimpleName();

    private SharedPreferences preferences;

    private Button buttonServer;
    private Button buttonClient;

    private EditText editIp;
    private EditText editPort;

    private Hub hub;

    private ProgressDialog progressDialog;

    private Pattern ipPattern = Pattern.compile("(?:[0-9]+\\.){3}[0-9]+");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.hub = new Hub(this, this);

        buttonServer = (Button) findViewById(R.id.buttonServer);
        editIp = (EditText) findViewById(R.id.editIp);
        editPort = (EditText) findViewById(R.id.editPort);
        buttonClient = (Button) findViewById(R.id.buttonClient);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String ip = preferences.getString("used_ip", "1.1.1.1");
        int port = preferences.getInt("used_port", 7777);

        editIp.setText(ip);
        editPort.setText(String.valueOf(port));

        buttonClient.setOnClickListener(onClientClickListener);

        setEnabled();
    }

    private void setEnabled() {
        buttonServer.setText("Server");
        buttonClient.setEnabled(true);
        editIp.setEnabled(true);
        editPort.setEnabled(true);
        buttonServer.setOnClickListener(onServerClickListener);
    }

    private void setDisabled() {
        buttonServer.setText("Disconnect");
        buttonClient.setEnabled(false);
        editIp.setEnabled(false);
        editPort.setEnabled(false);
        buttonServer.setOnClickListener(onDisconnectListener);
    }

    private void handleConnect(CommInterface commInterface) {
        Log.e(DEBUG_TAG, "handleConnect");
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connecting...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        setDisabled();
        hub.connect(commInterface);
    }

    private View.OnClickListener onServerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.e(DEBUG_TAG, "onServerClickListener");
            handleConnect(new TcpServer());
        }
    };

    private View.OnClickListener onClientClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.e(DEBUG_TAG, "onClientClickListener");

            if (editIp.length() > 0 && editPort.length() > 0 &&
                    ipPattern.matcher(editIp.getText().toString()).matches()) {

                String ip = editIp.getText().toString();
                int port = Integer.valueOf(editPort.getText().toString());

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("used_ip", ip);
                editor.putInt("used_port", port);
                editor.apply();

                handleConnect(new TcpClient(ip, port));
            } else {
                Toast.makeText(getApplicationContext(), "Incorrect IP or port", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener onDisconnectListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.e(DEBUG_TAG, "onDisconnectListener");
            hub.disconnect();
        }
    };

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
                setEnabled();
            }
        });
    }

    @Override
    public void onMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
