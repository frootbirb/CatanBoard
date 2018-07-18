package catan.frootbirb.catanboard;

import android.content.SharedPreferences;
import android.util.Log;

import org.jacop.constraints.And;
import org.jacop.constraints.Eq;
import org.jacop.constraints.GCC;
import org.jacop.constraints.IfThen;
import org.jacop.constraints.Or;
import org.jacop.constraints.PrimitiveConstraint;
import org.jacop.constraints.SumInt;
import org.jacop.constraints.XeqC;
import org.jacop.constraints.XeqY;
import org.jacop.constraints.XneqC;
import org.jacop.constraints.XneqY;
import org.jacop.core.IntVar;
import org.jacop.core.Store;
import org.jacop.search.DepthFirstSearch;
import org.jacop.search.IndomainRandom;
import org.jacop.search.InputOrderSelect;
import org.jacop.search.Search;
import org.jacop.search.SelectChoicePoint;
import org.jacop.search.TimeOutListener;

import java.util.ArrayList;

/*
 * Created by frootbirb on 6/28/17.
 * JaCoP library licensed under AGPL
 */

class Solver {

    public static final int[] numProb = {1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1}; // probability of each number tile
    private static final Store s = new Store();
    private static int size, tol;
    private static boolean exp, bal, failed;
    private static IntVar resVars[], numVars[], resSums[];

    Solver(SharedPreferences prefs) {

        boolean port, distRes, distNum, distProb;

        // initialize lists and vars
        port = prefs.getBoolean("port_pref", false);
        tol = Integer.parseInt(prefs.getString("bal_tol_pref", "0"));
        exp = prefs.getBoolean("size_pref", false);
        bal = prefs.getBoolean("bal_pref", true);
        distRes = prefs.getBoolean("dist_res_pref", true);
        distNum = prefs.getBoolean("dist_num_pref", true);
        distProb = prefs.getBoolean("dist_prob_pref", true);
        size = exp ? 30 : 19;
        Board.setExp(exp);
        log("Expanded set: " + exp);
        failed = false;

        resVars = new IntVar[size];
        numVars = new IntVar[size];
        resSums = new IntVar[6];

        // define resource quantities
        IntVar resCount[] = getResCount();

        // initialize sum calculator
        IntVar[] resLoc[] = getResLoc();

        // define number quantities
        IntVar numCount[] = getNumCount();

        // get tile adjacency list
        int[] adj[] = Board.getAdj();

        // iterate through tiles
        IntVar probVars[] = new IntVar[size];
        for (int i = 0; i < size; i++) {

            // create variables
            resVars[i] = new IntVar(s, 0, 5);
            numVars[i] = new IntVar(s, 2, 12);
            probVars[i] = new IntVar(s, 0, 5);
            log("created tile " + i);

            // impose desert special case
            s.impose(new Eq(
                    new XeqC(resVars[i], RE.DE.i),
                    new XeqC(numVars[i], 7)
            ));
            logV(i, "imposed desert rule");


            for (int r = 0; r < 6; r++) {
                // impose correlation between probability and num lists
                PrimitiveConstraint or[] = {new XeqC(numVars[i], r + 2), new XeqC(numVars[i], 12 - r)};
                s.impose(new Eq(
                        new Or(or), // ith tile's number is n+2 or ith tile's number is 12-n (same prob)
                        new XeqC(probVars[i], numProb[r]) // ith tile's probability is equal to the probability of n+2
                ));
                logV(i, "if num is " + (r + 2) + " or " + (12 - r) + ", set prob to " + numProb[r]);

                if (r == 5)
                    break;

                // impose count
                PrimitiveConstraint or2[] = {new XeqC(resVars[i], r), new XeqC(resVars[i], RE.DE.i)};
                resLoc[r][i] = new IntVar(s, 0, 5);
                s.impose(new Eq(
                        new XeqY(resLoc[r][i], probVars[i]), // ith tile's resource prob count matches its probability
                        new Or(or2) // ith tile is of type r
                ));
                logV(i, "if res is " + RE.res(r).n + ", connect prob to sum for " + RE.res(r).n);
            }

            // impose distribution constraints
            for (int j : adj[i]) {
                if (distRes) {
                    s.impose(new XneqY(resVars[i], resVars[j]));
                    logV(i, "resource is not the same as tile " + j);
                }
                if (distNum) {
                    s.impose(new XneqY(numVars[i], numVars[j]));
                    logV(i, "number is not the same as tile " + j);
                }
                if (distProb) {
                    s.impose(new XneqY(probVars[i], probVars[j]));
                    logV(i, "probability is not the same as tile " + j);
                }

                // always impose special 6/8 rule
                PrimitiveConstraint or[] = {new XeqC(numVars[i], 6), new XeqC(numVars[i], 8)};
                PrimitiveConstraint and[] = {new XneqC(numVars[j], 6), new XneqC(numVars[j], 8)};
                s.impose(new IfThen(
                        new Or(or), // ith tile is a 6 or an 8
                        new And(and) // ith tile is not a 6 or an 8
                ));
                logV(i, "if 6 or 8, " + j + " is not 6 or 8");
            }
        }

        // impose port constraints
        if (port) {
            ArrayList<ArrayList<Integer>> portAdj = getPortContact();
            for (int res = 0; res < 5; res++) {
                for (int place : portAdj.get(res)) {
                    s.impose(new XneqC(resVars[place], res));
                    logV(place, "is not " + RE.res(res).n + "; adjacent port");
                }
            }
        }

        // impose balance constraints
        for (int r = 0; r < 5; r++) {
            s.impose(new SumInt(s, resLoc[r], "==", resSums[r])); // resSums[r] = sum(resLoc[r])
            log("linked sum for " + RE.res(r).n);
        }

        // impose cardinality constraints
        s.impose(new GCC(resVars, resCount));
        s.impose(new GCC(numVars, numCount));
        log("imposed cardinality constraints");

        // solve
        log("SOLVING...");
        Search<IntVar> resSearch = new DepthFirstSearch<>();
        Search<IntVar> numSearch = new DepthFirstSearch<>();

        resSearch.setTimeOut(2);
        numSearch.setTimeOut(2);

        SelectChoicePoint<IntVar> resSelect = new InputOrderSelect<>(
                s,
                resVars,
                new IndomainRandom<>()
        );
        SelectChoicePoint<IntVar> numSelect = new InputOrderSelect<>(
                s,
                numVars,
                new IndomainRandom<>()
        );

        numSearch.setTimeOutListener(new TimeOutListener() {
            @Override
            public void executedAtTimeOut(int i) {
                failed = true;
            }

            @Override
            public void setChildrenListeners(TimeOutListener[] timeOutListeners) {

            }

            @Override
            public void setChildrenListeners(TimeOutListener timeOutListener) {

            }
        });

        resSearch.setTimeOutListener(new TimeOutListener() {
            @Override
            public void executedAtTimeOut(int i) {
                failed = true;
            }

            @Override
            public void setChildrenListeners(TimeOutListener[] timeOutListeners) {

            }

            @Override
            public void setChildrenListeners(TimeOutListener timeOutListener) {

            }
        });

        numSearch.labeling(s, numSelect);
        resSearch.labeling(s, resSelect);
    }

    // populate adjacency list for ports
    private static ArrayList<ArrayList<Integer>> getPortContact() {

        ArrayList<ArrayList<Integer>> ret = new ArrayList<>();
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());

        int[] ports[] = Board.getPorts();
        int[] portAdj[] = Board.getPortAdj();

        // iterate through port list
        for (int i = 0; i < ports[0].length; i++) {
            if (ports[2][i] < RE.TXT.i) { // if port is not a 3:1
                int tile = ports[0][i];
                int mode = ports[1][i];
                int type = ports[2][i];

                ret.get(type).add(tile); // add port host tile
                ret.get(type).add(portAdj[tile][mode]); // add port neighbour using lookup
            }
        }

        return ret;
    }

    // populate number quantities
    private IntVar[] getNumCount() {
        IntVar numCount[] = new IntVar[11];
        int base = exp ? 3 : 2;
        int minor = base - 1;
        for (int n = 1; n < 5; n++) {
            numCount[n] = new IntVar(s, base, base);
            numCount[10 - n] = new IntVar(s, base, base);
        }
        for (int n = 0; n < 11; n += 5) {
            numCount[n] = new IntVar(s, minor, minor);
        }
        return numCount;
    }

    // populate resource quantities
    private IntVar[] getResCount() {
        IntVar resCount[] = new IntVar[6];
        for (int r = 0; r < 6; r++) {
            int val = RE.res(r).c(exp);
            resCount[r] = new IntVar(s, val, val);
        }
        return resCount;
    }

    // initialize sum calculator
    private IntVar[][] getResLoc() {
        IntVar[] resLoc[] = new IntVar[5][];
        for (int r = 0; r < 5; r++) {
            // create variables
            resLoc[r] = new IntVar[size];
            resSums[r] = new IntVar(s);

            // set domains
            if (bal) {
                int base = exp ? 17 : 11;
                resSums[r].setDomain(base - tol, base + 1 + tol);
            } else {
                resSums[r].setDomain(0, 100);
            }
        }
        return resLoc;
    }

    // get solution for number placement
    public int[] getNums() {
        int ret[] = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = numVars[i].value();
        }
        return ret;
    }

    // get solution for resource placement
    public int[] getRes() {
        int ret[] = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = resVars[i].value();
        }
        return ret;
    }

    // get net probability of each resource
    public int[] getSums() {
        int ret[] = new int[5];
        for (int r = 0; r < 5; r++) {
            ret[r] = resSums[r].value();
        }
        return ret;
    }

    public boolean failed() {
        return failed;
    }

    private void logV(int curVar, String input) {
        log("\tTile " + curVar + ": " + input);
    }

    private void log(String input) {
        Log.d("CATANSOLVER", input);
    }
}