package catan.frootbirb.catanboard;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by frootbirb on 6/28/17.
 */

public class MainActivity extends AppCompatActivity {
    private static Settings mSettings;

    // creates and displays a solution
    private void make() {
        // get solution
        Solver s = new Solver(PreferenceManager.getDefaultSharedPreferences(this));

        // set board info and redraw
        Board d = findViewById(R.id.drawing);
        d.setWillNotDraw(false);
        d.setLists(s.getRes(), s.getNums(), s.getSums());
        d.invalidate();
    }

    private void hide() {
        Board d = findViewById(R.id.drawing);
        d.setWillNotDraw(true);
        d.invalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        make();
    }

    // handle menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.btnSettings:
                if (mSettings == null) {
                    mSettings = new Settings();
                    hide();
                    getFragmentManager().beginTransaction().replace(android.R.id.content, mSettings).commit();
                } else {
                    getFragmentManager().beginTransaction().remove(mSettings).commit();
                    mSettings = null;
                    make();
                }
                return true;
            default:
                return false;
        }
    }

    // initialize menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    static public class Settings extends PreferenceFragment {
        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            // Load the Preferences from the XML file
            addPreferencesFromResource(R.xml.prefs);
        }
    }
}