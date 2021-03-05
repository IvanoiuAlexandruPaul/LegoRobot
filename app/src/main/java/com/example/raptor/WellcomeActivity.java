package com.example.raptor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class WellcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wellcome);

        Button start_button = (Button) findViewById(R.id.StartButton);
        start_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(WellcomeActivity.this, MainActivity.class);
                startActivity(i);
            }
        });

    }

    public void goToUrl(View view) {
        String url = "https://drive.google.com/file/d/1u7RssE6GsG0-AlLPxTD-6QszocohXEjg/view?usp=sharing";
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    public void goToBluetooth(View view) {
        Intent bluetoothPicker = new Intent("android.bluetooth.devicepicker.action.LAUNCH");
        startActivity(bluetoothPicker);
    }
}
