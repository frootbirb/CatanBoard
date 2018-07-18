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

    // enums for material types
    public static final int BR = 0;
    public static final int OR = 1;
    public static final int WO = 2;
    public static final int SH = 3;
    public static final int WH = 4;
    public static final int DE = 5;
    public static final int TXT = 6;
    public static final int RED = 7;
    public static final int BCK = 8;

    public static final String BOARD_STATE = "boardState";

    private static int r, h, s;
    private static boolean exp;
    private static Paints p;
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

    public static int[][] getPorts(boolean expanded) {

        int[] small[] = {
                {0, 1, 3, 6, 11, 12, 15, 16, 17}, // locations
                {1, 0, 5, 0, 4, 5, 2, 3, 2}, // modes
                {TXT, BR, TXT, WO, TXT, SH, WH, TXT, OR} // types
        };
        int[] big[] = {
                {0, 1, 6, 7, 12, 17, 22, 23, 27, 28, 29}, // locations
                {1, 0, 0, 5, 3, 4, 2, 5, 3, 2, 4}, // modes
                {TXT, BR, WO, TXT, TXT, TXT, WH, SH, TXT, OR, SH} // types
        };

        return expanded ? big : small;
    }

    private void init() {
        setWillNotDraw(true);
        p = new Paints();
        res = new int[30];
        nums = new int[30];
        sums = new int[5];
    }

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

    private void circler(Canvas canvas, float x, float y, float mod, Paint color) {
        int dot = r / 17;
        canvas.drawCircle(x + mod * dot, (int) (y + r / 1.8), (float) (dot * 1.5), p.Resource(BCK));
        canvas.drawCircle(x - mod * dot, (int) (y + r / 1.8), (float) (dot * 1.5), p.Resource(BCK));
        canvas.drawCircle(x + mod * dot, (int) (y + r / 1.8), dot, color);
        canvas.drawCircle(x - mod * dot, (int) (y + r / 1.8), dot, color);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //TODO: overhaul drawing
        super.onDraw(canvas);
        // initialize params
        boolean tall = (((this.getHeight() * 1.0) / this.getWidth()) > 1.2);
        r = exp ? this.getWidth() / 13 : this.getWidth() / 11;
        h = (int) ((double) r * Math.sqrt(3) / 2);
        s = r * 3 / 4;
        p.setFont(r);

        int cx = this.getWidth() / 2;

        int cy = tall ? (int) Math.ceil(this.getHeight() * 0.56) : this.getHeight() / 2;
        int count = 0;
        int min = exp ? -3 : -2, max = exp ? 4 : 3;

        // draw count tiles
        if (tall) {
            for (int i = 0; i < 5; i++) {
                int x = r * (i - 2) * 2;
                canvas.drawPath(hex(x + cx, r * 2), p.Resource(i));
                canvas.drawText(Integer.toString(sums[i]), x + cx - r / 2, r * 2 + s / 2, p.Resource(TXT));
            }
        }

        // draw board
        for (int i = min; i < max; i++) { // what row?
            int nudge = 0;
            if (i % 2 != 0) {
                nudge = i * h * (exp ? 1 : -1);
            }
            if (Math.abs(i) > 2) {
                nudge += 2 * h * (i < 0 ? 1 : -1);
            }
            if (exp) {
                nudge += r;
            }

            // calculates bounds (how many blocks & where they are)
            int low = exp ? min + Math.abs((int) Math.floor(i / 2.0)) : min + Math.abs((int) Math.ceil(i / 2.0));
            int high = exp ? max - 1 - Math.abs((int) Math.ceil(i / 2.0)) : max - Math.abs((int) Math.floor(i / 2.0));

            for (int j = low; j < high; j++) { // which block?
                // make paint object
                Paint txt = p.Resource(TXT);

                int x = cx + r * j * 2 + nudge;
                int y = cy + r * 2 * i;
                // draw hex
                canvas.drawPath(hex(x, y), p.Resource(res[count]));
                // draw port if applicable
                int index = Arrays.binarySearch(getPorts(exp)[0], count);
                if (index >= 0) { // if this tile has a port
                    canvas.drawPath(port(x, y, getPorts(exp)[1][index]), p.Resource(getPorts(exp)[2][index]));
                }

                if (nums[count] != 7) { // if not the desert and populated
                    int val = Solver.numVal[nums[count] - 2];
                    //txt = val == 5 ? p.Resource(RED) : p.Resource(TXT);

                    // draw the dots
                    switch (val) {
                        case 4:
                            circler(canvas, x, y, 6, txt);
                        case 2:
                            circler(canvas, x, y, 2, txt);
                            break;
                        case 5:
                            txt = p.Resource(RED);
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

    public void setLists(String boardStateStr) {
        JSONObject boardState = null;
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

            exp = resArray.length == 30;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

class Paints {

    private static Paint brick, ore, wood, sheep, wheat, desert, text, white, red;


    public Paints() {
        brick = new Paint();
        brick.setColor(Color.rgb(244, 113, 66));
        brick.setStyle(Paint.Style.FILL);
        ore = new Paint();
        ore.setColor(Color.rgb(147, 147, 147));
        ore.setStyle(Paint.Style.FILL);
        wood = new Paint();
        wood.setColor(Color.rgb(57, 132, 80));
        wood.setStyle(Paint.Style.FILL);
        sheep = new Paint();
        sheep.setColor(Color.rgb(132, 193, 127));
        sheep.setStyle(Paint.Style.FILL);
        wheat = new Paint();
        wheat.setColor(Color.rgb(249, 221, 2));
        wheat.setStyle(Paint.Style.FILL);
        desert = new Paint();
        desert.setColor(Color.rgb(244, 220, 158));
        desert.setStyle(Paint.Style.FILL);
        text = new Paint();
        text.setColor(Color.BLACK);
        white = new Paint();
        white.setColor(Color.WHITE);
        red = new Paint();
        red.setColor(Color.RED);
    }

    public void setFont(int set) {
        text.setTextSize(set);
        white.setTextSize(set);
        red.setTextSize(set);
    }

    public Paint Resource(int c) {
        switch (c) {
            case Board.BR:
                return brick;
            case Board.OR:
                return ore;
            case Board.WO:
                return wood;
            case Board.WH:
                return wheat;
            case Board.SH:
                return sheep;
            case Board.DE:
                return desert;
            case Board.TXT:
                return text;
            case Board.RED:
                return red;
            case Board.BCK:
                return white;
            default:
                return text;
        }
    }
}