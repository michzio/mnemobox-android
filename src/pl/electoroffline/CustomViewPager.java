package pl.electoroffline;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager {

	public CustomViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	float mStartDragX;
	OnSwipeOutListener mListener;


	public void setOnSwipeOutListener(OnSwipeOutListener listener) {
	    mListener = listener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev){
	    //if(getCurrentItem()==getAdapter().getCount()-1){
	        final int action = ev.getAction();
	        float x = ev.getX();
	        switch(action & MotionEventCompat.ACTION_MASK){
	        case MotionEvent.ACTION_DOWN:
	            mStartDragX = x;
	            break;
	        case MotionEvent.ACTION_MOVE:
	        	if (mStartDragX < x && getCurrentItem() == 0) {
	                mListener.onSwipeOutAtStart();
	            } else if (mStartDragX > x && getCurrentItem() == getAdapter().getCount() - 1) {
	                mListener.onSwipeOutAtEnd();
	            }
	            break;
	        case MotionEvent.ACTION_UP:
	        	/* deprecated
	            if (x<mStartDragX){
	                mListener.onSwipeOutAtEnd();
	            }else{
	                mStartDragX = 0;
	            }*/
	            break;
	        }
	    /* }else{
	        mStartDragX=0;
	       }
	    */
	    return super.onTouchEvent(ev);
	}  

	public interface OnSwipeOutListener {
	    public void onSwipeOutAtEnd();
	  
	    public void onSwipeOutAtStart(); 
	}
}
