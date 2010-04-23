package de.droidgames.lunarlanderextended;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/*
 *	Klasse, die ein Labyrinth aus einer txt Datei generiert, in dem 
 *  Blob die Diamanten sammeln muss.
 */
public class Maze {
	private final static int TILE_SIZE = 40;
	private final static int MAZE_COLS = 20;
    private final static int MAZE_ROWS = 26;
		
 // tile types
    public final static int PATH_TILE = 0;
    public final static int VOID_TILE = 1;
    public final static int EXIT_TILE = 2;

    // tile colors
    private final static int VOID_COLOR = Color.BLACK;

    // maze level data
    private static int[] mMazeData;

    // number of level
    public final static int MAX_LEVELS = 10;
    
 // current tile attributes
    private Rect mRect = new Rect();
    private int mRow;
    private int mCol;
    private int mX;
    private int mY;
    
	public Maze() {
		
	}
	
	void load(Activity activity, int newLevel) {
        // maze data is stored in the assets folder as level1.txt, level2.txt
        // etc....
        String mLevel = "level" + newLevel + ".txt";

        InputStream is = null;

        try {
            // construct our maze data array.
            mMazeData = new int[MAZE_ROWS * MAZE_COLS];
            // attempt to load maze data.
            is = activity.getAssets().open(mLevel);

            // we need to loop through the input stream and load each tile for
            // the current maze.
            for (int i = 0; i < mMazeData.length; i++) {
                // data is stored in unicode so we need to convert it.
                mMazeData[i] = Character.getNumericValue(is.read());

                // skip the "," and white space in our human readable file.
                is.read();
                is.read();
            }
        } catch (Exception e) {
            Log.i("Maze", "load exception: " + e);
        } finally {
            closeStream(is);
        }

    }
	
	/**
     * Draw the maze.
     * 
     * @param canvas
     *            Canvas object to draw too.
     * @param paint
     *            Paint object used to draw with.
     */
    public void draw(Canvas canvas, Paint paint) {
        // loop through our maze and draw each tile individually.
        for (int i = 0; i < mMazeData.length; i++) {
            // calculate the row and column of the current tile.
            mRow = i / MAZE_COLS;
            mCol = i % MAZE_COLS;

            // convert the row and column into actual x,y co-ordinates so we can
            // draw it on screen.
            mX = mCol * TILE_SIZE;
            mY = mRow * TILE_SIZE;

            // draw the actual tile based on type.
            if (mMazeData[i] == PATH_TILE) {
                //canvas.drawBitmap(mImgPath, mX, mY, paint);
            }
            else if (mMazeData[i] == EXIT_TILE) {
                //canvas.drawBitmap(mImgExit, mX, mY, paint);
            }
            else if (mMazeData[i] == VOID_TILE) {
                // since our "void" tile is purely black lets draw a rectangle
                // instead of using an image.

                // tile attributes we are going to paint.
                mRect.left = mX;
                mRect.top = mY;
                mRect.right = mX + TILE_SIZE;
                mRect.bottom = mY + TILE_SIZE;

                paint.setColor(VOID_COLOR);
                canvas.drawRect(mRect, paint);
            }
        }
    }
        
        /**
         * Closes the specified stream.
         * 
         * @param stream
         *            The stream to close.
         */
        private static void closeStream(Closeable stream) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        
    }
