package de.droidgames.lunarlanderextended;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class Sound {
    public static final int MAX_STREAMS = 5;
    
	private SoundPool mSoundPool;

	public int SOUND_BLING;
		
	public Sound(Context context) {
    	mSoundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
    	
    	// load sounds using convenient names
    	SOUND_BLING = mSoundPool.load(context, R.raw.bling, 1);
	}

	public int play(int soundID, float leftVolume, float rightVolume, int priority, int loop, float rate) {
		return mSoundPool.play(soundID, leftVolume, rightVolume, priority, loop, rate);
	}
}
