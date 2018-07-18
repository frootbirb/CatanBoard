package catan.frootbirb.catanboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/*
 * Created by frootbirb on 6/28/17.
 * Hexagon-drawing code derived from https://stackoverflow.com/questions/30217450/how-to-draw-hexagons-in-android
 */

public class Board extends View {
    public static final String BOARD_STATE = "boardState";

    private static int r, h, s;
    private static boolean exp;
    private static int[] res, nums, sums;

    public Board(Context c) {
        super(c);
        init();
    }

    public Board(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public Board(Context c, AttributeSet a, int s) {
        super(c, a, s);
        init();
    }

    private void init() {
        setWillNotDraw(true);
        res = new int[30];
        nums = new int[30];
        sums = new int[5];
    }

    // helper function for drawing hex tiles
    private Path hex(int x, int y) {

        int cdx[] = {h, h, 0, -h, -h, 0};
        int cdy[] = {-s, s, r, s, -s, -r};

        Path p = new Path();
        p.moveTo(x, y - r);
        for (int k = 0; k < 6; k++) {
            p.lineTo(x + cdx[k], y + cdy[k]);
        }

        return p;
    }

    // helper function for drawing ports
    private Path port(int x, int y, int mode) {

        int mod = r / 13;

        int cdx[] = {h, h, 0};
        int cdy[] = {-s, (int) Math.floor(-r * 1.25), -r};

        switch (mode) {
            case 0: // top right
                y = y - mod;
                break;
            case 1: // top left
                y = y - mod;
                cdx[0] = -cdx[0];
                cdx[1] = -cdx[1];
                break;
            case 2: // bottom right
                y = y + mod;
                cdy[0] = -cdy[0];
                cdy[1] = -cdy[1];
                cdy[2] = -cdy[2];
                break;
            case 3: // bottom left
                y = y + mod;
                cdx[0] = -cdx[0];
                cdx[1] = -cdx[1];
                cdy[0] = -cdy[0];
                cdy[1] = -cdy[1];
                cdy[2] = -cdy[2];
                break;
            case 4: // true right
                x = x + mod;
                cdx[2] = (int) Math.floor(r * 1.25);
                cdy[1] = s;
                cdy[2] = 0;
                break;
            case 5: // true left
                x = x - mod;
                cdx[0] = -cdx[0];
                cdx[1] = -cdx[1];
                cdx[2] = (int) Math.floor(-r * 1.25);
                cdy[1] = s;
                cdy[2] = 0;
                break;
            default:
                break;
        }

        Path p = new Path();
        int max = 3;
        p.moveTo(x + cdx[max - 1], y + cdy[max - 1]);
        for (int k = 0; k < max; k++) {
            p.lineTo(x + cdx[k], y + cdy[k]);
        }

        return p;
    }

    // helper function for drawing dots
    private void circler(Canvas canvas, float x, float y, float mod, Paint color) {
        int dot = r / 17;
        canvas.drawCircle(x + mod * dot, (int) (y + r / 1.8), dot, color);
        canvas.drawCircle(x - mod * dot, (int) (y + r / 1.8), dot, color);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //TODO: overhaul drawing
        super.onDraw(canvas);
        // initialize params
        boolean tall = (((this.getHeight() * 1.0) / this.getWidth()) > 1.2); // evaluate phone height to determine showing sums
        r = exp ? this.getWidth() / 13 : this.getWidth() / 11; // hex radius
        h = (int) (r * Math.sqrt(3) / 2); // hex height
        s = r * 3 / 4; // hex side length

        // set font size
        RE.TXT.p.setTextSize(r);
        RE.RED.p.setTextSize(r);

        // find center of rendering space
        int cx = this.getWidth() / 2;
        int cy = tall ? (int) Math.ceil(this.getHeight() * 0.56) : this.getHeight() / 2;

        int count = 0;
        // set indices of top and bottom rows
        int min = exp ? -3 : -2, max = exp ? 4 : 3;

        // draw count tiles
        if (tall) {
            for (int i = 0; i < 5; i++) {
                int x = r * (i - 2) * 2;
                canvas.drawPath(hex(x + cx, r * 2), RE.res(i).p);
                canvas.drawText(Integer.toString(sums[i]), x + cx - r / 2, r * 2 + s / 2, RE.TXT.p);
            }
        }

        // draw board
        for (int i = min; i < max; i++) { // what row?
            int nudge = 0; // nudge tiles to the side because rows are staggered
            if (i % 2 != 0) {
                nudge = i * h * (exp ? 1 : -1);
            }
            if (Math.abs(i) > 2) {
                nudge += 2 * h * (i < 0 ? 1 : -1);
            }
            if (exp) {
                nudge += r;
            }

            // calculates bounds (how many tiles & where they are)
            int low = exp ? min + Math.abs((int) Math.floor(i / 2.0)) : min + Math.abs((int) Math.ceil(i / 2.0));
            int high = exp ? max - 1 - Math.abs((int) Math.ceil(i / 2.0)) : max - Math.abs((int) Math.floor(i / 2.0));

            for (int j = low; j < high; j++) { // which block?
                // make paint object
                Paint txt = RE.TXT.p;

                // calculate center of tile
                int x = cx + r * j * 2 + nudge;
                int y = cy + r * i * 2;
                // draw hex
                canvas.drawPath(hex(x, y), RE.res(res[count]).p);
                // draw port if applicable
                int index = Arrays.binarySearch(getPorts()[0], count);
                if (index >= 0) { // if this tile has a port
                    canvas.drawPath(port(x, y, getPorts()[1][index]), RE.res(getPorts()[2][index]).p);
                }

                if (nums[count] != 7) { // if not the desert
                    int prob = Solver.numProb[nums[count] - 2];
                    // draw the dots
                    switch (prob) {
                        case 4:
                            circler(canvas, x, y, 6, txt);
                        case 2:
                            circler(canvas, x, y, 2, txt);
                            break;
                        case 5:
                            txt = RE.RED.p;
                            circler(canvas, x, y, 8, txt);
                        case 3:
                            circler(canvas, x, y, 4, txt);

                        default:
                            circler(canvas, x, y, 0, txt);
                    }
                    // draw the numbers TODO: get this outlined
                    canvas.drawText(Integer.toString(nums[count]),
                            x - ((nums[count] > 9) ? r / 2 : r / 4),
                            y + s / 2,
                            txt);
                }
                count++;
            }
        }
    }

    // define port qualities
    static int[][] getPorts() {

        int[] small[] = {
                {0, 1, 3, 6, 11, 12, 15, 16, 17}, // locations
                {1, 0, 5, 0, 4, 5, 2, 3, 2}, // modes
                {RE.TXT.i, RE.BR.i, RE.TXT.i, RE.WO.i, RE.TXT.i, RE.SH.i, RE.WH.i, RE.TXT.i, RE.OR.i} // types
        };
        int[] big[] = {
                {0, 1, 6, 7, 12, 17, 22, 23, 27, 28, 29}, // locations
                {1, 0, 0, 5, 3, 4, 2, 5, 3, 2, 4}, // modes
                {RE.TXT.i, RE.BR.i, RE.WO.i, RE.TXT.i, RE.TXT.i, RE.TXT.i, RE.WH.i, RE.SH.i, RE.TXT.i, RE.OR.i, RE.SH.i} // types
        };

        return exp ? big : small;
    }

    // define adjacency list for tiles
    static int[][] getAdj() {

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

    // define adjacency list for ports
    static int[][] getPortAdj() {

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

    // populate board from input JSON of board state data
    public void setLists(String boardStateStr) {
        JSONObject boardState;
        String[] resArray, numArray, sumArray;
        try {
            boardState = new JSONObject(boardStateStr);

            resArray = boardState.getString("res").split(",");
            numArray = boardState.getString("num").split(",");
            sumArray = boardState.getString("sum").split(",");

            for (int i = 0; i < resArray.length; ++i) {
                res[i] = Integer.parseInt(resArray[i]);
                nums[i] = Integer.parseInt(numArray[i]);
            }
            for (int i = 0; i < sumArray.length; ++i) {
                sums[i] = Integer.parseInt(sumArray[i]);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // set board size
    static void setExp(boolean expIn) {
        exp = expIn;
    }
}

// enum for resource types
enum RE {
    BR(0, 3, Color.rgb(244, 113, 66), "brick"),
    OR(1, 3, Color.rgb(147, 147, 147), "ore"),
    WO(2, 4, Color.rgb(57, 132, 80), "wood"),
    SH(3, 4, Color.rgb(132, 193, 127), "sheep"),
    WH(4, 4, Color.rgb(249, 221, 2), "wheat"),
    DE(5, 1, Color.rgb(244, 220, 158), "desert"),
    TXT(6, 0, Color.BLACK, "text"),
    RED(7, 0, Color.RED, "red number"),
    BCK(8, 0, Color.WHITE, "background");

    private final int c;
    final int i;
    final Paint p;
    final String n;

    RE(int index, int count, int color, String name) {
        i = index;
        c = count;
        n = name;
        p = new Paint();
        p.setColor(color);
    }

    int c(boolean expanded) {
        int ret = c;
        if (expanded && c != 0)
            ret += c == 1 ? 1 : 2;
        return ret;
    }

    static RE res(int index) {
        return RE.values()[index];
    }
}