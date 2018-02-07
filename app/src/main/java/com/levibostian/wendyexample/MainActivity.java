package com.levibostian.wendyexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.curiosityio.wendyexample.R;
import com.levibostian.wendy.service.PendingTasks;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.activity_main_create_pending_task_button).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                PendingTasks.sharedInstance().addTask(new FooPendingTask());
            }
        });
    }

}
