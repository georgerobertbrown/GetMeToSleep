package com.gncbrown.getmetosleep.Utilities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.gncbrown.getmetosleep.R;

public class DisplayTextActivity extends AppCompatActivity {
    private static final String TAG = "DisplayTextActivity";

    private Context context;

    public static final String EXTRA_TEXT_CONTENT = "extra_text_content";
    public static final String EXTRA_TEXT_TITLE = "extra_text_title";

    private TextView largeTextView;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        String appName = getString(R.string.app_name);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_show_text);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Set title from intent or default
        if (getIntent() != null && getIntent().hasExtra(EXTRA_TEXT_TITLE)) {
            String title = getIntent().getStringExtra(EXTRA_TEXT_TITLE);
            if (getSupportActionBar() != null) { // Check again in case title was set before action bar
                getSupportActionBar().setTitle(appName + ":" + title);
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(appName + ":" + "Details"); // Default title
            }
        }
        // toolbar.setSubtitleTextColor(android.graphics.Color.WHITE); // This line might not be needed if using theme attributes

        largeTextView = findViewById(R.id.largeTextView);
        scrollView = findViewById(R.id.scrollView); 

        StringBuffer stringBufferContent = new StringBuffer();
        if (getIntent() != null && getIntent().hasExtra(EXTRA_TEXT_CONTENT)) {
            String text = getIntent().getStringExtra(EXTRA_TEXT_CONTENT);
            if (text != null) {
                stringBufferContent.append(text);
            }
        } else {
            stringBufferContent.append("No text content provided.");
        }

        largeTextView.setText(stringBufferContent.toString());
        //largeTextView.setTextColor(Color.BLACK); //Color.WHITE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // This will navigate up to the parent activity (MainActivity)
            // as specified in the AndroidManifest.xml.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
