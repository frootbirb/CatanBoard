package catan.frootbirb.catanboard;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by frootbirb on 6/28/17.
 */

public class MainActivity extends AppCompatActivity {

    private static Menu _menu;
    private static int tolerance = Board.TIGHT;
    private static boolean expanded = false, bal = true, wasSet = true, port = false, dist[] = {true, true, true};

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
                make();
                return true;

            case R.id.rBtnPorts:
                port = true;
                item.setChecked(true);
                return true;
            case R.id.rBtnNoPorts:
                port = false;
                item.setChecked(true);
                return true;

            case R.id.chkBal:
                bal = !item.isChecked();
                item.setChecked(bal);
                MenuItem t = _menu.findItem(R.id.rBtnTight);
                MenuItem m = _menu.findItem(R.id.rBtnMed);
                MenuItem l = _menu.findItem(R.id.rBtnLoose);
                t.setEnabled(bal);
                m.setEnabled(bal);
                l.setEnabled(bal);
                return true;
            case R.id.rBtnTight:
                tolerance = Board.TIGHT;
                item.setChecked(true);
                return true;
            case R.id.rBtnMed:
                tolerance = Board.MED;
                item.setChecked(true);
                return true;
            case R.id.rBtnLoose:
                tolerance = Board.LOOSE;
                item.setChecked(true);
                return true;

            case R.id.rBtnStd:
                expanded = false;
                item.setChecked(true);
                return true;
            case R.id.rBtnExp:
                expanded = true;
                item.setChecked(true);
                return true;

            case R.id.chkDistRes:
                dist[0] = !item.isChecked();
                item.setChecked(dist[0]);
                return true;
            case R.id.chkDistNum:
                dist[1] = !item.isChecked();
                item.setChecked(dist[1]);
                return true;
            case R.id.chkDistVal:
                dist[2] = !item.isChecked();
                item.setChecked(dist[2]);
                MenuItem n = _menu.findItem(R.id.chkDistNum);
                if (dist[2]) {
                    wasSet = n.isChecked();
                    n.setChecked(true);
                } else {
                    n.setChecked(wasSet);
                }
                n.setEnabled(!dist[2]);
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
        MenuItem a = menu.findItem(R.id.rBtnTight);
        MenuItem b = menu.findItem(R.id.rBtnStd);
        MenuItem c = menu.findItem(R.id.chkBal);
        MenuItem d = menu.findItem(R.id.chkDistRes);
        MenuItem e = menu.findItem(R.id.chkDistNum);
        MenuItem f = menu.findItem(R.id.chkDistVal);
        MenuItem g = menu.findItem(R.id.rBtnNoPorts);
        a.setChecked(true);
        b.setChecked(true);
        c.setChecked(true);
        d.setChecked(true);
        e.setChecked(true);
        e.setEnabled(false);
        f.setChecked(true);
        g.setChecked(true);
        _menu = menu;
        return true;
    }
}