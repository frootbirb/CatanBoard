package catan.frootbirb.catanboard;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by frootbirb on 6/28/17.
 */

public class MainActivity extends AppCompatActivity {
    private static Settings mSettings;
    private static Info mInfo;
    private Menu menu;
    private Board d;

    private void hide() {
        d.setWillNotDraw(true);
        d.invalidate();
    }

    private void getBestBoard(boolean solve) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.contains(Board.BOARD_STATE) || solve) {
            new Solve().execute(prefs);
            //TODO: drawn since settings update
        } else {
            String boardState = prefs.getString(Board.BOARD_STATE, "null");

            // set board info
            d.setLists(boardState);

            // stop spinner and resume drawing
            findViewById(R.id.spinner).setVisibility(View.GONE);
            d.setWillNotDraw(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        d = findViewById(R.id.drawing);
        getBestBoard(false);
    }

    // handle menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        boolean wasOnFragment = false;

        if (mInfo != null) {
            wasOnFragment = true;
            getFragmentManager().beginTransaction().remove(mInfo).commit();
            mInfo = null;
        }
        if (mSettings != null) {
            wasOnFragment = true;
            getFragmentManager().beginTransaction().remove(mSettings).commit();
            mSettings = null;
        }
        hide();

        int icon = R.drawable.ic_hex;

        switch (item.getItemId()) {
            case R.id.btnSettings:
                mSettings = new Settings();
                getFragmentManager().beginTransaction().replace(android.R.id.content, mSettings).commit();
                break;
            case R.id.btnInfo:
                mInfo = new Info();
                getFragmentManager().beginTransaction().replace(android.R.id.content, mInfo).commit();
                break;
            case R.id.btnMake:
                getBestBoard(!wasOnFragment);
                icon = R.drawable.ic_refresh;
                break;
            default:
                return false;
        }

        menu.getItem(0).setIcon(icon);

        return true;
    }

    // initialize menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    static public class Settings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            // Load the Preferences from the XML file
            addPreferencesFromResource(R.xml.prefs);

            Preference button = findPreference("default_pref");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    prefs.edit().clear().apply();
                    PreferenceManager.setDefaultValues(getActivity(), R.xml.prefs, true);
                    for (String key : prefs.getAll().keySet()) {
                        if (key.equals("bal_tol_pref"))
                            continue;
                        boolean val = prefs.getBoolean(key, false);
                        SwitchPreference pref = (SwitchPreference) findPreference(key);
                        pref.setChecked(val);
                    }
                    return true;
                }
            });

            final SwitchPreference val_dist = (SwitchPreference) findPreference("dist_val_pref");
            final SwitchPreference num_dist = (SwitchPreference) findPreference("dist_num_pref");
            num_dist.setEnabled(!val_dist.isChecked());
            num_dist.setChecked(num_dist.isChecked() || val_dist.isChecked());
            val_dist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    num_dist.setEnabled(!val_dist.isChecked());
                    num_dist.setChecked(num_dist.isChecked() || val_dist.isChecked());
                    return true;
                }
            });
        }
    }

    static public class Info extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View result = inflater.inflate(R.layout.fragment_info, container, false);
            // make links clickable
            TextView link = result.findViewById(R.id.infoLink);
            TextView credits = result.findViewById(R.id.infoCredits);
            link.setMovementMethod(LinkMovementMethod.getInstance());
            credits.setMovementMethod(LinkMovementMethod.getInstance());
            return result;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class Solve extends AsyncTask<SharedPreferences, Void, Solver> {

        ProgressBar spinner;
        Board d;
        SharedPreferences prefs;

        @Override
        protected void onPreExecute() {
            spinner = findViewById(R.id.spinner);
            d = findViewById(R.id.drawing);

            // start spinner and freeze drawing
            spinner.setVisibility(View.VISIBLE);
            d.setWillNotDraw(true);
        }

        @Override
        protected Solver doInBackground(SharedPreferences... sharedPreferences) {
            Solver s;
            prefs = sharedPreferences[0];
            do {
                s = new Solver(prefs);
            } while (s.failed());
            return s;
        }

        @Override
        protected void onPostExecute(Solver s) {
            super.onPostExecute(s);

            JSONObject boardState = new JSONObject();

            try {
                boardState.put("res", stringify(s.getRes()));
                boardState.put("num", stringify(s.getNums()));
                boardState.put("sum", stringify(s.getSums()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Board.BOARD_STATE, boardState.toString()).apply();

            // set board info
            d.setLists(boardState.toString());

            // stop spinner and resume drawing
            spinner.setVisibility(View.GONE);
            d.setWillNotDraw(false);
        }

        private String stringify(int[] in) {
            StringBuilder builder = new StringBuilder();
            int len = prefs.getBoolean("size_pref", false) && in.length > 18 ? 30 : in.length;
            for (int i = 0; i < len; i++)
                builder.append(in[i]).append(",");
            String ret = builder.toString();
            return ret;
        }
    }
}