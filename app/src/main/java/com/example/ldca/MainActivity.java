package com.example.ldca;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnUpload, btnURL, btnPayload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        btnURL = findViewById(R.id.btnURL);
        btnPayload = findViewById(R.id.btnPayload);

        btnUpload.setOnClickListener(v ->
                startActivity(new Intent(this, UploadCSVActivity.class)));

        btnURL.setOnClickListener(v ->
                startActivity(new Intent(this, URLCSVActivity.class)));

        btnPayload.setOnClickListener(v ->
                startActivity(new Intent(this, PayloadActivity.class)));
    }
}