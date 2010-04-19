/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.droidgames.lunarlanderextended;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import de.droidgames.lunarlanderextended.llextView.LunarThread;

/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
public class llext extends Activity implements SensorListener {
	
	SensorManager sm = null;
    private int deviceOrientation;
	
    private static final int MENU_PAUSE = 4;
    private static final int MENU_RESUME = 5;
    private static final int MENU_STOP = 7;

    /** A handle to the thread that's actually running the animation. */
    private LunarThread mLunarThread;

    /** A handle to the View in which the game is running. */
    private llextView mLunarView;

    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	return true;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newgame:
                mLunarThread.doStart();
                return true;
            case MENU_STOP:
                mLunarThread.setState(LunarThread.STATE_LOSE,
                        getText(R.string.message_stopped));
                return true;
            case MENU_PAUSE:
                mLunarThread.pause();
                return true;
            case MENU_RESUME:
                mLunarThread.unpause();
                return true;
            case R.id.about:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_EASY);
                return true;
            case R.id.help:
                mLunarThread.setDifficulty(LunarThread.DIFFICULTY_MEDIUM);
                return true;
            case R.id.preferences:
            	//mLunarThread.pause();
            	// Launch the preferences activity as a subactivity, so we
                // know when it returns.
                Intent pIntent = new Intent();
                pIntent.setClass(getBaseContext(), Preferences.class);               
                startActivity(pIntent);             
                break;
        }
        return true;
    }

    /**
     * Invoked when the Activity is created.
     * 
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.lunar_layout);

        // get handles to the LunarView from XML, and its LunarThread
        mLunarView = (llextView) findViewById(R.id.lunar);
        mLunarThread = mLunarView.getThread();

        // give the LunarView a handle to the TextView used for messages
        mLunarView.setTextView((TextView) findViewById(R.id.text));
        mLunarView.setTextView2((TextView) findViewById(R.id.DiamondsText));

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            mLunarThread.setState(LunarThread.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
            mLunarThread.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
        sm.registerListener(this, 
                SensorManager.SENSOR_ORIENTATION |
        		SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mLunarView.getThread().pause(); // pause game when Activity pauses
    }

    /**
     * Notification that something is about to happen, to give the Activity a
     * chance to save state.
     * 
     * @param outState a Bundle into which this Activity should save its state
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mLunarThread.saveState(outState);
        Log.w(this.getClass().getName(), "SIS called");
    }
    
    public void onSensorChanged(int sensor, float[] values) {
        synchronized (this) {
            Log.d("LOG", "onSensorChanged: " + sensor + ", x: " + values[0] + ", y: " + values[1] + ", z: " + values[2]);
            if (sensor != SensorManager.SENSOR_ACCELEROMETER || values.length < 3)
                return;                    
                       
            float x, y, z;
            if (deviceOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            	x = -values[1];
            	y = values[0];
            	z = values[2];
            } else {
            	x = values[0];
            	y = values[1];
            	z = values[2];
            }
            float m = (float) Math.sqrt(x*x + y*y + z*z);
            float tilt = m == 0 ? 0  : (float) Math.toDegrees(Math.asin(x / m));
            Log.v("LOG", "tilt: " + x + "," + y + "," + z + " -> " + tilt);

            // Amplify the user's movements.
            tilt *= 1;        
            float mTiltAngle = tilt < 0 ? tilt + 360 : tilt;           	
            mLunarThread.doAccelerate(mTiltAngle);          
        }
    }
    
    public void onAccuracyChanged(int sensor, int accuracy) {
    	Log.d("LOG","onAccuracyChanged: " + sensor + ", accuracy: " + accuracy);
        
    }
}

