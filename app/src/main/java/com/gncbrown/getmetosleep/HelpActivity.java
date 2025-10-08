package com.gncbrown.getmetosleep;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.gncbrown.getmetosleep.Utilities.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpActivity extends AppCompatActivity {
    private static final String TAG = "HelpActivity";

    public static Context context;

    private String helpType = "help";

    private static RelativeLayout helpLayout;
    private static ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        try {
            helpType = getIntent().getStringExtra("type");
        } catch (Exception e) {
            Log.e(TAG, "HelpActivity.onCreate; error: " + e.getMessage());
            helpType = "help";
        }

        setContentView(R.layout.activity_help);
        helpLayout = findViewById(R.id.helpLayout);

        LoadActivity activityLoader = new LoadActivity();
        activityLoader.execute();
    }

    private class LoadActivity extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        protected void onPreExecute() {
            progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            helpLayout.addView(progressBar, params);
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupViews();
                }
            });
            return null;
        }
    }

    private void setupViews() {
        Log.d(TAG, "setupViews");

        String fullVersion = Utils.getFullVersion();
        TextView version = findViewById(R.id.version);
        version.setText(fullVersion);

        TextView helpText = findViewById(R.id.helpText);
        String releaseNotes = "";
        try {
            // Programmatically load text from an asset and place it into the
            // text view. Note that the text we are loading is ASCII, so we
            // need to convert it to UTF-16.
            InputStream is;
            if (helpType.equals("changeLog")) {
                releaseNotes = "<b>Change Log:</b><br/><br/>";
                List<String> assets = Arrays.asList(getAssets().list(""));
                Collections.sort(assets, Collections.reverseOrder());
                for (String asset : assets) {
                    if (asset.contains("release_") && asset.endsWith(".txt")) {
//						Log.d(TAG, "asset=" + asset);
                        is = getAssets().open(asset);

                        // We guarantee that the available method returns the
                        // total size of the asset... of course, this does
                        // mean that a single asset can't be more than 2 gigs.
                        int size = is.available();

                        // Read the entire asset into a local byte buffer.
                        byte[] buffer = new byte[size];
                        is.read(buffer);
                        is.close();

                        // Convert the buffer into a string.
                        releaseNotes += "<i>" + asset.replaceFirst(".txt", "") + "</i><br/>----------------------<br/>"
                                + new String(buffer) + "<br/><br/>";
                    }
                }
            } else {
                String helpFileName = "help_text.html";
                is = getAssets().open(helpFileName);

                // We guarantee that the available method returns the total
                // size of the asset... of course, this does mean that a single
                // asset can't be more than 2 gigs.
                int size = is.available();

                // Read the entire asset into a local byte buffer.
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                // Convert the buffer into a string.
                releaseNotes = new String(buffer);
            }

        } catch (IOException e) {
            // Should never happen!
            releaseNotes = e.toString();
            //throw new RuntimeException(e);
        }

        String newlinesReplaced = releaseNotes.replaceAll("\n", "<br>\n");
        String newReleaseNotes = newlinesReplaced;

        String regex = "<img src=\"(.*?\\.png)\">";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(newlinesReplaced);
        while (matcher.find()) {
            String imgString = matcher.group();
            String icon = imgString.replaceFirst(".*?\"", "")
                    .replaceFirst("\".*?>", "")
                    .replaceFirst("\\..*", "");
            int resId = this.getResources().getIdentifier(icon, "drawable", this.getPackageName());
            newReleaseNotes = newReleaseNotes.replaceFirst(String.format("<img src=\"%s.png\">", icon),
                    String.format("<img src=\"%s\"/>", resId));
        }

        helpText.setText(Html.fromHtml(newReleaseNotes, FROM_HTML_MODE_COMPACT, new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(final String source) {
                Drawable d = null;
                try {
                    d = getResources().getDrawable(Integer.parseInt(source));
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Image not found. Check the ID.", e);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Source string not a valid resource ID.", e);
                }

                return d;
            }
        }, null));

    }

    private void clickNavigate(View v) {
        Log.d(TAG, "clickNavigate");
    }

    @Override
    public void onPause() {
        //StopwatchActivity.debugHelper(TAG + ":onPause", "onPause", true);
        super.onPause();
    }

    @Override
    public void onResume() {
        //StopwatchActivity.debugHelper(TAG + ":onResume", "onResume", true);
        super.onResume();
    }
}