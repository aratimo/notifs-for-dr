package com.example.alarm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

public class FormActivity extends AppCompatActivity {
    private EditText participantIdEditText;
    private Spinner conditionSpinner;
    private Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        participantIdEditText = findViewById(R.id.participant_id);
        conditionSpinner = findViewById(R.id.condition_spinner);
        startButton = findViewById(R.id.start_button);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.conditions_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        conditionSpinner.setAdapter(adapter);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String participantId = participantIdEditText.getText().toString();
                String condition = conditionSpinner.getSelectedItem().toString();

                Intent intent = new Intent(FormActivity.this, MainActivity.class);
                intent.putExtra("participant_id", participantId);
                intent.putExtra("condition", condition);
                startActivity(intent);
            }
        });
    }
}
