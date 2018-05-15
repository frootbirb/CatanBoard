package catan.frootbirb.catanboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by frootbirb on 6/28/17.
 */

public class MainActivity extends AppCompatActivity {

    private static int tolerance = Board.TIGHT;
    private static boolean expanded = false, bal = true, wasSet = true, port = false, dist[] = {true, true, true};
    private static SharedPreferences prefs;

    // creates and displays a solution
    private void make() {
        // get solution
        Solver s = new Solver(port, tolerance, expanded, bal, dist);

        // set board info and redraw
        Board d = (Board) findViewById(R.id.drawing);
        d.setLists(s.getRes(), s.getNums(), s.getSums());
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

            case R.id.btnMake:
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new Settings()).commit();
                prefs = getPreferences(MODE_PRIVATE);
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