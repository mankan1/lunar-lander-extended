

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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;


/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * 
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class llextView extends SurfaceView implements SurfaceHolder.Callback {
    class LunarThread extends Thread {
        /*
         * Difficulty setting constants
         */
        public static final int DIFFICULTY_EASY = 0;
        public static final int DIFFICULTY_HARD = 1;
        public static final int DIFFICULTY_MEDIUM = 2;
        public static final int NUMBER_OF_DIAMONDS = 20;
        public static final int NUMBER_OF_CRATERS = 8;
        /*
         * Physics constants
         */
        public static final int PHYS_DOWN_ACCEL_SEC = 300;
        public static final int PHYS_ACCEL_FACTOR_X = 2;
        public static final int INITIAL_JUMP_SPEED = -250;
        public static final int PHYS_FIRE_ACCEL_SEC = 80;
        public static final int PHYS_FUEL_INIT = 60;
        public static final int PHYS_FUEL_MAX = 100;
        public static final int PHYS_FUEL_SEC = 10;
        public static final int PHYS_SLEW_SEC = 120; // degrees/second rotate
        public static final int PHYS_SPEED_INIT = 30;
        public static final int PHYS_SPEED_MAX = 180;
        /*
         * State-tracking constants
         */
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;
        
        /*
         * Goal condition constants
         */
        public static final int TARGET_ANGLE = 18; // > this angle means crash
        public static final int TARGET_BOTTOM_PADDING = 17; // px below gear
        public static final int TARGET_PAD_HEIGHT = 8; // how high above ground
        public static final int TARGET_SPEED = 28; // > this speed means crash
        public static final double TARGET_WIDTH = 1.6; // width of target
        /*
         * UI constants (i.e. the speed & fuel bars)
         */
        private static final String KEY_DIFFICULTY = "mDifficulty";
        private static final String KEY_DX = "mDX";
        private static final String KEY_DY = "mDY";
        private static final String KEY_BALL_HEIGHT = "mBallHeight";
        private static final String KEY_BALL_WIDTH = "mBallWidth";
        private static final String KEY_DIAMONDS = "mDiamonds";

        private static final String KEY_X = "mXB";
        private static final String KEY_Y = "mYB";

        /*
         * Member (state) fields
         */
        /** The drawable to use as the background of the animation canvas */
        private Bitmap mBackgroundImage;

        /**
         * Current height of the surface/canvas.
         * 
         * @see #setSurfaceSize
         */
        private int mCanvasHeight = 1;

        /**
         * Current width of the surface/canvas.
         * 
         * @see #setSurfaceSize
         */
        private int mCanvasWidth = 1;
        /**
         * Current difficulty -- amount of fuel, allowed angle, etc. Default is
         * MEDIUM.
         */
        private int mDifficulty;

        /** Velocity dx. */
        private double mDX;

        /** Velocity dy. */
        private double mDY;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** What to draw for Blob */
        private Drawable mBallImage;
        private int mBallWidth;
        private int mBallHeight;
        
        /** What to draw for Ground */
        private Drawable mGroundImage;
        private int mGWidth;
        private int mGHeight;
        
        /** What to draw for a diamond */
        private Drawable mDiamondImage;
        private int mDWidth;
        private int mDHeight;
        
        /** What to draw for a crater */
        private Drawable mCraterImage;
        private int mCWidth;
        private int mCHeight;
        private Drawable mMidImage;
        
         /** Used to figure out elapsed time between frames */
        private long mLastTime;

        /** Paint to draw the lines on screen. */
        private Paint mLinePaint;

        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Scratch rect object. */
        private RectF mScratchRect;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;
        
        /** Is the ball jumping? */
        private boolean mJumping;
        private double mTiltAngle = 0;
        
        private double[] mXCrater = new double[NUMBER_OF_CRATERS];
        
        /** Number of Diamond collected */
        private int mDiamonds;
  
        /** X of Diamond */
        private double mXDiamond;
        private double[] mXDiamonds = new double[NUMBER_OF_DIAMONDS];
        private boolean[] mDiamondsViz = new boolean[NUMBER_OF_DIAMONDS];
        
        /**Y of Diamond */
        private double mYDiamond;
        private double[] mYDiamonds = new double[NUMBER_OF_DIAMONDS];;
        
        /** X of Blob */
        private double mXB;
        
        /** Y of Blob */
        private double mYB;
        
        /** Rotation Angle of Blob */
        private double mHeading=0;
        
        public LunarThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;
            
            mLinePaint = new Paint();
            mLinePaint.setAntiAlias(true);
            mLinePaint.setARGB(255, 100, 255, 10);
            mLinePaint.setStyle(Paint.Style.STROKE);
            mLinePaint.setStrokeWidth(5);
            
            Resources res = context.getResources();
            // cache handles to our key sprites & other drawables
           
            mBallImage = context.getResources().getDrawable(R.drawable.ball);
            mGroundImage = context.getResources().getDrawable(R.drawable.ground);
            mDiamondImage = context.getResources().getDrawable(R.drawable.diamond2);
            mCraterImage = context.getResources().getDrawable(R.drawable.krater);
            mMidImage = context.getResources().getDrawable(R.drawable.mid);
            
            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this way
            mBackgroundImage = BitmapFactory.decodeResource(res,R.drawable.earthrise);

            // Use the regular lander image as the model size for all sprites
            mBallWidth = mBallImage.getIntrinsicWidth();
            mBallHeight = mBallImage.getIntrinsicHeight();
            mDWidth = mDiamondImage.getIntrinsicWidth();
            mDHeight = mDiamondImage.getIntrinsicHeight();
            mCWidth = mCraterImage.getIntrinsicWidth();
            mCHeight = mCraterImage.getIntrinsicHeight();
            mGWidth = mGroundImage.getIntrinsicWidth();
            mGHeight = mGroundImage.getIntrinsicHeight();
            
            // Initialize paints for speedometer
            mLinePaint = new Paint();
            mLinePaint.setTextSize(20);
            mLinePaint.setAntiAlias(true);
            mLinePaint.setARGB(255, 0, 255, 0);
          
            mScratchRect = new RectF(0, 0, 0, 0);

            mDiamonds = 0;
            mDifficulty = DIFFICULTY_EASY;

            // initial show-up of lander (not yet playing)
            mXB = 1;
            mYB = 1;
            mDX = 0;
            mDY = 0;
        }

        public int getMode() {
        	return mMode;
        }
        
        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
            	                
            	// pick a convenient initial location for the lander sprite
                mXB = mCanvasWidth / 4;
                mYB = mCanvasHeight-mBallHeight/4;
                mDX = 0;
                mDY = 0;
                
                mXDiamonds[0]= mCanvasWidth /3;
                mYDiamonds[0] = mCanvasHeight-mBallHeight-Math.random()*40;
                mDiamondsViz[0] = true;
                for (int i=1 ;i<NUMBER_OF_DIAMONDS;i++) {
                	mXDiamonds[i] = mXDiamonds[i-1] + Math.random()*200;
                	mYDiamonds[i] = mCanvasHeight-mBallHeight-Math.random()*40;
                	mDiamondsViz[i] = true;
                }
                mXCrater[0] = mCanvasWidth / 2;
                for (int i=1 ;i<NUMBER_OF_CRATERS;i++) {
                	mXCrater[i] = mXCrater[i-1] + Math.random()*350 + mCWidth;                	
                }
                
                mXDiamond = mCanvasWidth/2;
                mYDiamond = mCanvasHeight-mBallHeight;
                mLastTime = System.currentTimeMillis() + 100;
                setState(STATE_RUNNING);
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         * 
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
                setState(STATE_PAUSE);
                
                mDifficulty = savedState.getInt(KEY_DIFFICULTY);
                mXB = savedState.getDouble(KEY_X);
                mYB = savedState.getDouble(KEY_Y);
                mDX = savedState.getDouble(KEY_DX);
                mDY = savedState.getDouble(KEY_DY);
                
                mBallWidth = savedState.getInt(KEY_BALL_WIDTH);
                mBallHeight = savedState.getInt(KEY_BALL_HEIGHT);
   
                mDiamonds = savedState.getInt(KEY_DIAMONDS);
       
            }
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        doDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         * 
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
                    map.putDouble(KEY_X, Double.valueOf(mXB));
                    map.putDouble(KEY_Y, Double.valueOf(mYB));
                    map.putDouble(KEY_DX, Double.valueOf(mDX));
                    map.putDouble(KEY_DY, Double.valueOf(mDY));
                    map.putInt(KEY_BALL_WIDTH, Integer.valueOf(mBallWidth));
                    map.putInt(KEY_BALL_HEIGHT, Integer.valueOf(mBallHeight));
                    map.putInt(KEY_DIAMONDS, Integer.valueOf(mDiamonds));
                }
            }
            return map;
        }

        /**
         * Sets the current difficulty.
         * 
         * @param difficulty
         */
        public void setDifficulty(int difficulty) {
            synchronized (mSurfaceHolder) {
                mDifficulty = difficulty;
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         * 
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         * 
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {
            /*
             * This method optionally can cause a text message to be displayed
             * to the user when the mode changes. Since the View that actually
             * renders that text is part of the main View hierarchy and not
             * owned by this thread, we can't touch the state of that View.
             * Instead we use a Message + Handler to relay commands to the main
             * thread, which updates the user-text View.
             */
            synchronized (mSurfaceHolder) {
                mMode = mode;

                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    b.putInt("Diamonds", mDiamonds);
                    msg.setData(b);
                    mHandler.sendMessage(msg);                    
                } else {
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    if (mMode == STATE_READY)
                        str = res.getText(R.string.mode_ready);
                    else if (mMode == STATE_PAUSE)
                        str = res.getText(R.string.mode_pause);
                    else if (mMode == STATE_LOSE)
                        str = res.getText(R.string.mode_lose);
                    else if (mMode == STATE_WIN) {
                        str = res.getString(R.string.mode_win_prefix)
                                + mDiamonds + " "
                                + res.getString(R.string.mode_win_suffix);
                        mDiamonds = 0;
                    }
                    if (message != null) {
                        str = message + "\n" + str;
                    }

                    if (mMode == STATE_LOSE) mDiamonds = 0;

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }
        
        
        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

                // don't forget to resize the background image
                mBackgroundImage = mBackgroundImage.createScaledBitmap(
                        mBackgroundImage, width, height, true);
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
            setState(STATE_RUNNING);
        }
       
        boolean doTouchEvent(MotionEvent event) {
            int action = event.getAction();
            // If we're stopped, touch screen to start.
            if (mMode != STATE_RUNNING) {
            	if (action == MotionEvent.ACTION_DOWN) {
            		if (mMode == 0)
            			unpause();                    	
            		else
            			doStart();
            		return true;
            	}
            } else {
            	switch (action) {
            	case MotionEvent.ACTION_DOWN:
            	case MotionEvent.ACTION_UP:
            		doJump();
            		return true;
            	case MotionEvent.ACTION_MOVE:
            	case MotionEvent.ACTION_CANCEL:
            	default:
            		break;
            	}
            }

            return false;

        }
        
        public void doJump() {
        	// Jump only, if we are on the ground
        	if (mYB >= mCanvasHeight) {
        		mDY=INITIAL_JUMP_SPEED;
            	mJumping = true;
        	}        	
        }
  
        public void doAccelerate(double mTiltAngle2) {
        	mTiltAngle = -mTiltAngle2;        	
        }
        
        /**
         * Draws the ball and diamonds to the provided Canvas.
         */
        private void doDraw(Canvas canvas) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawBitmap(mBackgroundImage, 0, 0, null);
            String txt = new String("Diamanten: "+Integer.toString(mDiamonds));
            canvas.drawText(txt, 5, 20, mLinePaint);

            canvas.save();            
            mGroundImage.setBounds((int)(mXDiamond/3-100), (int)(mCanvasHeight-2*mGHeight),
            		(int)(mXDiamond/3+3*mCanvasWidth), (int)(mCanvasHeight));
            mGroundImage.draw(canvas);

            for (int i=0;i<NUMBER_OF_CRATERS;i++) {
            	int left = (int)(mXCrater[i]+mXDiamond-mCWidth/3);
        		int right =(int)(mXCrater[i]+mXDiamond+mCWidth/3);
        		int top = (int)(mCanvasHeight-mCHeight/2);
        		int bottom = (int)(mCanvasHeight);
        		
        		if (i<NUMBER_OF_CRATERS-1) {
        			int leftm = (int)(mXCrater[i]+mXDiamond);
        			int rightm = (int)(mXCrater[i+1]+mXDiamond-mCWidth/3);
        			mMidImage.setBounds(leftm, top+10, rightm, bottom);
        			mMidImage.draw(canvas);
        		}	
        		if (left+mCWidth>0 && right-mCWidth<mCanvasWidth) {
        			mCraterImage.setBounds(left, top, right, bottom);
        			mCraterImage.draw(canvas);
        		}
            }
            
            for (int i=0;i<NUMBER_OF_DIAMONDS;i++) {
            	if (mDiamondsViz[i] == true) {
            		int left = (int)(mXDiamonds[i]+mXDiamond-mDWidth/2);
            		int right =(int)(mXDiamonds[i]+mXDiamond+mDWidth/2);
            		int top = (int)(mYDiamonds[i]-mDHeight/2);
            		int bottom = (int)(mYDiamonds[i]+mDHeight/2);
            		if (left+mDWidth>0 && right-mDWidth<mCanvasWidth) {
            			mDiamondImage.setBounds(left, top, right, bottom);
            			mDiamondImage.draw(canvas);
            		}
            	}
            }
            //canvas.restore();
            //canvas.save();
            canvas.rotate((float)mHeading, (float)(mXB), (float)(mYB-mBallHeight/4));
            // Draw Blob on his location      
            mBallImage.setBounds((int)mXB-mBallWidth/4,(int)mYB-mBallHeight/2,
            		(int)mXB+mBallWidth/4, (int)mYB);
            mBallImage.draw(canvas);
            canvas.restore();            	
        }

        /**
         * Figures the lander state (x, y, fuel, ...) based on the passage of
         * realtime. Does not invalidate(). Called at the start of draw().
         * Detects the end-of-game and sets the UI to the next state.
         */
        private void updatePhysics() {
            long now = System.currentTimeMillis();

            // Do nothing if mLastTime is in the future.
            // This allows the game-start to delay the start of the physics
            // by 100ms or whatever.
            if (mLastTime > now) return;

            double elapsed = (now - mLastTime) / 1000.0;

            // Base accelerations -- 0 for x, gravity for y
            double ddx = PHYS_DOWN_ACCEL_SEC * PHYS_ACCEL_FACTOR_X * elapsed * Math.sin(Math.PI*mTiltAngle/180) * PHYS_ACCEL_FACTOR_X;
            double ddy = PHYS_DOWN_ACCEL_SEC * (SensorManager.GRAVITY_MOON / 1.6) * elapsed;

          
            if (mJumping) {
                // taking 0 as up, 90 as to the right
                // cos(deg) is ddy component, sin(deg) is ddx component
                
                // have this much acceleration from the jump
                double accel = PHYS_FIRE_ACCEL_SEC * 1010;

                double radians = 2 * Math.PI * 0 / 360;
                //ddx = Math.sin(radians) * accel;
                //ddy += Math.cos(radians) * accel;
                mJumping = false;
            }

            double dxOld = mDX;
            double dyOld = mDY;

            // figure speeds for the end of the period
            mDX += ddx;
            if (mDX > PHYS_SPEED_MAX) {
            	mDX = PHYS_SPEED_MAX;
            } else if (mDX < -PHYS_SPEED_MAX) {
            	mDX = -PHYS_SPEED_MAX;
            }
            
            mDY += ddy;

            // figure position based on average speed during the period
            mXDiamond += elapsed * (mDX + dxOld) / 2;
            mYB += elapsed * (mDY + dyOld) / 2;
            if (mXDiamond > 150) {
            	mXDiamond = 150;
            	mDX = 0;
            }
            if (mXDiamond < -4*mCanvasWidth) {
            	mXDiamond = -4*mCanvasWidth;
            	mDX = 0;
            }
            if (mYB<0) {
            	mYB = 0;
            } else{
            	if (mYB>=mCanvasHeight) {
            		mYB = mCanvasHeight;
            	}
            }
            mHeading = mXDiamond / 223;
            mHeading = 360-(mHeading - Math.floor(mHeading))*360;
            mLastTime = now;
            
            // Evaluate if we have touched a Diamond ... Count
            // or if we have fallen into a crater       
            mScratchRect.set(mBallImage.getBounds());
            CharSequence message = "";
            //Resources res = mContext.getResources();
            int result = STATE_LOSE;
            if (mDiamonds<NUMBER_OF_DIAMONDS) {
            	result = STATE_RUNNING;
            	for (int i=0;i<NUMBER_OF_DIAMONDS;i++) {
            		if (mScratchRect.contains((float)(mXDiamonds[i]+mXDiamond), (float)mYDiamonds[i]) && mDiamondsViz[i]==true) {
            			mDiamonds++;            		
            			mDiamondsViz[i]=false;
            		}
            	}
            	
            	/*
            	for (int i=0;i<NUMBER_OF_CRATERS;i++) {
            		if (mScratchRect..contains((float)(mXDiamonds[i]+mXDiamond), (float)mYDiamonds[i]) && mDiamondsViz[i]==true) {
            			mDiamonds++;            		
            			mDiamondsViz[i]=false;
            		}
            	}
            	*/
            	message = "Test";
            	//setDiamondsText(message);
            } else {
            	result = STATE_WIN;
            }
            setState(result, message);

        }
    }

    /** Handle to the application context, used to e.g. fetch Drawables. */
    private Context mContext;

    /** Pointer to the text view to display "Paused.." etc. */
    private TextView mStatusText;

    /** Pointer to the text view to display "Diamanten:.." */
    public TextView mDiamondsText;
    
    /** The thread that actually draws the animation */
    private LunarThread thread;

    public llextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new LunarThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
                mDiamondsText.setText(m.getData().getString("Diamonds"));
            }
        });
        
        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     * 
     * @return the animation thread
     */
    public LunarThread getThread() {
        return thread;
    }

    /**
     * Handle MotionEvent events.
     * 
     * @param   event           The motion event.
     * @return                  True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return thread.doTouchEvent(event);
    }
    
    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

    /**
     * Installs a pointer to the text view used for messages.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }
    
    /**
     * Installs a pointer to the text view used for Diamonds message.
     */
    public void setTextView2(TextView textView) {
        mDiamondsText = textView;
    }
    public void setDiamondsText(CharSequence message) {
    	mDiamondsText.setText(message);
    }


    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}

