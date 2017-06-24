package pl.electoroffline;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

@SuppressLint("NewApi")
public class VolumeChangeObserver extends ContentObserver {
	
	public interface OnVolumeChangedListener { 
		public void onVolumeChanged(int newVolume);
	}
	
	private OnVolumeChangedListener volumeListener;  
	private final Context context; 
	private int volumeStreamType; 
	
	private int previousVolume;
	
	public VolumeChangeObserver(Context ctx, 
								Handler handler, 
								int volumeType, 
								OnVolumeChangedListener listener) {
		super(handler);
		
		// setting volume change listener object
		volumeListener = listener; 
		context = ctx; 
		volumeStreamType = volumeType; 
		
		AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	    previousVolume = audio.getStreamVolume(volumeStreamType);
	}

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }
	
	@Override
	public void onChange(boolean selfChange) {
		 super.onChange(selfChange);
	     this.onChange(selfChange, null);
	}    
	 
	@Override
	public void onChange(boolean selfChange, Uri uri) { 
		
		Log.d(VolumeChangeObserver.class.getName(), "Volume change detected in VolumeChangeObserver.onChange()."); 
		
		 AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	     int currentVolume = audio.getStreamVolume(volumeStreamType);
	     
	     int delta=previousVolume-currentVolume;

	      if(delta>0)
	      {
	            Log.d(VolumeChangeObserver.class.getName(), "Volume Changed: Decreased");
	            previousVolume=currentVolume;
	            
	            if(volumeListener != null) { 
	            	volumeListener.onVolumeChanged(currentVolume);
	            }
	      } else if(delta<0)
	      {
	            Log.d(VolumeChangeObserver.class.getName(), "Volume Changed: Increased");
	            previousVolume=currentVolume;
	            if(volumeListener != null) { 
	            	volumeListener.onVolumeChanged(currentVolume);
	            }
	      }
	}    
}
