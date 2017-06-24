package pl.electoroffline;

public class PresentationRTLFragment extends PresentationFragment {
	
	@Override
	public void onSwipingBackward() { 
		super.onSwipingForward();
	}
	
	@Override 
	public void onSwipedBackward() { 
		super.onSwipedForward();
	}
	
	@Override
	public void onSwipedForward() { 
		super.onSwipedBackward(); 
	}
	
	@Override
	public void onSwipingForward() { 
		super.onSwipingBackward(); 
	}
	
}