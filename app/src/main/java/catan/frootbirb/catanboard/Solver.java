package catan.frootbirb.catanboard;

import android.content.SharedPreferences;

import org.jacop.constraints.Eq;
import org.jacop.constraints.GCC;
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

    public static final int[] numVal = {1, 2, 3, 4, 5, 0, 5, 4, 3, 2, 1};
    private static final Store s = new Store();
    private static int size, tol;
    private static boolean exp, bal, failed;
    private static IntVar resVars[], numVars[], resSums[];

    Solver(SharedPreferences prefs) {

        boolean port, distRes, distNum, distVal;

        // initialize lists and vars
        port = prefs.getBoolean("port_pref", false);
        tol = Integer.parseInt(prefs.getString("bal_tol_pref", "0"));
        exp = prefs.getBoolean("size_pref", false);
        bal = prefs.getBoolean("bal_pref", true);
        distRes = prefs.getBoolean("dist_res_pref", true);
        distNum = prefs.getBoolean("dist_num_pref", true);
        distVal = prefs.getBoolean("dist_val_pref", true);
        size = exp ? 30 : 19;
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
        int[] adj[] = getAdj();

        // iterate through solution lists
        IntVar valVars[] = new IntVar[size];
        for (int i = 0; i < size; i++) {
            // create variables
            resVars[i] = new IntVar(s, 0, 5);
            numVars[i] = new IntVar(s, 2, 12);
            valVars[i] = new IntVar(s, 0, 5);

            // impose desert special case
            s.impose(new Eq(
                    new XeqC(resVars[i], Board.DE),
                    new XeqC(numVars[i], 7)
            ));

            for (int r = 0; r < 6; r++) {
                // impose correlation between val and num lists
                PrimitiveConstraint or[] = {new XeqC(numVars[i], r + 2), new XeqC(numVars[i], 12 - r)};
                s.impose(new Eq(
                        new Or(or), // ith tile's number is n+2 or ith tile's number is 12-n (same prob)
                        new XeqC(valVars[i], numVal[r]) // ith tile's value is equal to the probability of n+2
                ));

                if (r == 5)
                    break;

                // impose count
                PrimitiveConstraint or2[] = {new XeqC(resVars[i], r), new XeqC(resVars[i], Board.DE)};
                resLoc[r][i] = new IntVar(s, 0, 5);
                s.impose(new Eq(
                        new XeqY(resLoc[r][i], valVars[i]), // ith tile's resource value count matches its value
                        new Or(or2) // ith tile is of type r
                ));
            }

            // impose distribution constraints
            if (distRes || distNum || distVal) {
                for (int j : adj[i]) {
                    if (distRes)
                        s.impose(new XneqY(resVars[i], resVars[j]));
                    if (distNum)
                        s.impose(new XneqY(numVars[i], numVars[j]));
                    if (distVal)
                        s.impose(new XneqY(valVars[i], valVars[j]));
                }
            }
        }

        // impose port constraints
        if (port) {
            ArrayList<ArrayList<Integer>> portAdj = getPortContact();
            for (int res = 0; res < 5; res++) {
                for (int place : portAdj.get(res)) {
                    s.impose(new XneqC(resVars[place], res));
                }
            }
        }

        // impose balance constraints
        for (int r = 0; r < 5; r++) {
            s.impose(new SumInt(s, resLoc[r], "==", resSums[r])); // resSums[r] = sum(resLoc[r])
        }

        // impose cardinality constraints
        s.impose(new GCC(resVars, resCount));
        s.impose(new GCC(numVars, numCount));

        // solve
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

    // define adjacency list for ports
    private static ArrayList<ArrayList<Integer>> getPortContact() {

        ArrayList<ArrayList<Integer>> ret = new ArrayList<>();
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());
        ret.add(new ArrayList<Integer>());

        int[] ports[] = Board.getPorts(exp);
        int[] portAdj[] = getPortAdj();

        // iterate through port list
        for (int i = 0; i < ports[0].length; i++) {
            if (ports[2][i] < Board.TXT) { // if port is not a 3:1
                int tile = ports[0][i];
                int mode = ports[1][i];
                int type = ports[2][i];

                ret.get(type).add(tile); // add port host tile
                ret.get(type).add(portAdj[tile][mode]); // add port neighbour using lookup
            }
        }

        return ret;
    }

    // define adjacency list for tiles
    private static int[][] getAdj() {

        int[] small[] = {
                {}, // 0
                {0}, // 1
                {1}, // 2
                {0}, // 3
                {0, 1, 3}, // 4
                {1, 2, 4}, // 5
                {2, 5}, // 6
                {3}, // 7
                {3, 4, 7}, // 8
                {4, 5, 8}, // 9
                {5, 6, 9}, // 10
                {6, 10}, // 11
                {7, 8}, // 12
                {8, 9, 12}, // 13
                {9, 10, 13}, // 14
                {10, 11, 14}, // 15
                {12, 13}, // 16
                {13, 14, 16}, // 17
                {14, 15, 17} // 18
        };
        int[] big[] = {
                {}, // 0
                {0}, // 1
                {1}, // 2
                {0}, // 3
                {0, 1, 3}, // 4
                {1, 2, 4}, // 5
                {2, 5}, // 6
                {3}, // 7
                {3, 4, 7}, // 8
                {4, 5, 8}, // 9
                {5, 6, 9}, // 10
                {6, 10}, // 11
                {7}, // 12
                {7, 8, 12}, // 13
                {8, 9, 13}, // 14
                {9, 10, 14}, // 15
                {10, 11, 15}, // 16
                {11, 16}, // 17
                {12, 13}, // 18
                {13, 14, 18}, // 19
                {14, 15, 19}, // 20
                {15, 16, 20}, // 21
                {16, 17, 21}, // 22
                {18, 19}, // 23
                {19, 20, 23}, // 24
                {20, 21, 24}, // 25
                {21, 22, 25}, // 26
                {23, 24}, // 27
                {24, 25, 27}, // 28
                {25, 26, 28}, // 29
        };

        return exp ? big : small;
    }

    // define adjacency list for tiles
    private static int[][] getPortAdj() {

        int[] small[] = {
                {1, -1, -1, -1, -1, 3}, // 0
                {2, 0, -1, -1, -1, -1}, // 1
                {-1, -1, 1, -1, 6, -1}, // 2
                {-1, 0, -1, -1, -1, 7}, // 3
                {}, // 4
                {}, // 5
                {2, -1, -1, -1, 11, -1}, // 6
                {-1, 3, -1, 12, -1, -1}, // 7
                {}, // 8
                {}, // 9
                {}, // 10
                {6, -1, 15, -1, -1, -1}, // 11
                {-1, -1, -1, 16, -1, 7}, // 12
                {}, // 13
                {}, // 14
                {-1, -1, 18, -1, 11, -1}, // 15
                {-1, -1, 17, -1, -1, 12}, // 16
                {-1, -1, 18, -1, -1, -1}, // 17
                {-1, -1, -1, 17, 15, -1} // 18
        };

        int[] big[] = {
                //    ur  ul  dr  dl  tr  tl
                {1, -1, -1, -1, -1, 3}, // 0
                {2, 0, -1, -1, -1, -1}, // 1
                {-1, -1, 1, -1, 6, -1}, // 2
                {-1, 0, -1, -1, -1, 7}, // 3
                {}, // 4
                {}, // 5
                {2, -1, -1, -1, 11, -1}, // 6
                {-1, 3, -1, -1, -1, 12}, // 7
                {}, // 8
                {}, // 9
                {}, // 10
                {6, -1, -1, -1, 17, -1}, // 11
                {-1, 7, -1, 18, -1, -1}, // 12
                {}, // 13
                {}, // 14
                {}, // 15
                {}, // 16
                {11, -1, 22, -1, -1, -1}, // 17
                {-1, -1, -1, 23, -1, 12}, // 18
                {}, // 19
                {}, // 20
                {}, // 21
                {-1, -1, 26, -1, 17, -1}, // 22
                {-1, -1, -1, 27, -1, 18}, // 23
                {}, // 24
                {}, // 25
                {-1, -1, 29, -1, 22, -1}, // 26
                {-1, -1, 28, -1, -1, 23}, // 27
                {-1, -1, 29, 27, -1, -1}, // 28
                {-1, -1, -1, 28, 26, -1} // 29
        };

        return exp ? big : small;
    }

    // define resource quantities
    private IntVar[] getResCount() {
        IntVar resCount[] = new IntVar[6];
        int base = exp ? 6 : 4;
        int desert = exp ? 2 : 1;
        for (int r = 0; r < 6; r++) {
            switch (r) {
                case Board.BR:
                case Board.OR:
                    resCount[r] = new IntVar(s, base - 1, base - 1);
                    break;
                case Board.WO:
                case Board.SH:
                case Board.WH:
                    resCount[r] = new IntVar(s, base, base);
                    break;
                case Board.DE:
                    resCount[r] = new IntVar(s, desert, desert);
            }
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

    // define number quantities
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
        return failed || silentFailure();
    }

    // TODO: remove this when I figure out why the sum doesn't always work
    private boolean silentFailure() {
        int sum = 0;

        for (int i : getSums()) {
            sum += i;
        }

        return sum != (exp ? 88 : 58);
    }
}