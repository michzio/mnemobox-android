package pl.electoroffline;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SoundVolumePreference extends SeekBarPreference implements VolumeChangeObserver.OnVolumeChangedListener {

	public SoundVolumePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SoundVolumePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override 
	protected void initValuesDynamically()  {
		super.initValuesDynamically(); 
		
		// get mMinValue and mMaxValue dynamically based on OS media volume 
		// configuration (AudioManager) rather then setting it by XML attributes. 
		AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
		mMaxValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mMinValue = 0; 
	}
	
	@Override
	protected void updateView(View view) {
		super.updateView(view);
		mStatusText.setVisibility(View.GONE);
		TextView unitsRight = (TextView)view.findViewById(R.id.seekBarPrefUnitsRight);
		unitsRight.setVisibility(View.GONE);
		
		TextView unitsLeft = (TextView)view.findViewById(R.id.seekBarPrefUnitsLeft);
		unitsLeft.setVisibility(View.GONE);
	}
	
	/**
	 * Method overridden to take default value from system media volume settings rather then 
	 * android:defaultValue preference attribute
	 */
	@Override 
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		return getCurrentMediaVolume();
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		Log.d(SoundVolumePreference.class.getName(), "onSetInitialVolume() called..."); 
		
		mCurrentValue = getCurrentMediaVolume();
	
	}
	
	private Integer getCurrentMediaVolume() { 
		
		AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
		return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		
		int newValue = progress + mMinValue;
		
		if(newValue > mMaxValue)
			newValue = mMaxValue;
		else if(newValue < mMinValue)
			newValue = mMinValue;
		else if(mInterval != 1 && newValue % mInterval != 0) { 
			newValue = Math.round(((float)newValue)/mInterval)*mInterval;  
		}
		
		// change rejected, revert to the previous value
		if(!callChangeListener(newValue)){
			seekBar.setProgress(mCurrentValue - mMinValue); 
			return; 
		}
		
		// change accepted, store it
		mCurrentValue = newValue;
		mStatusText.setText(String.valueOf(newValue));
		Log.d(SoundVolumePreference.class.getName(), "Progress changed to: " + mCurrentValue); 
		
		// reflecting (persisting) sound volume change in operating system
		AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentValue, 0);
	}
	
	/**
	 * Callback method used to get notification about volume change detected by VolumeChangeObserver 
	 * in external sound system. Used by SoundVolumePreference object to reflect this changes in 
	 * SeekBar control state. 
	 */
	@Override
	public void onVolumeChanged(int newValue) { 
		
		Log.d(SoundVolumePreference.class.getName(), "Volume Changed in system settings to: " + newValue); 
		
		if(newValue > mMaxValue)
			newValue = mMaxValue;
		else if(newValue < mMinValue)
			newValue = mMinValue;
		else if(mInterval != 1 && newValue % mInterval != 0) { 
			newValue = Math.round(((float)newValue)/mInterval)*mInterval;  
		}
		
		mCurrentValue = newValue; 
		notifyChanged();
	}
}
