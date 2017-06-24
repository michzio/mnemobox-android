package pl.electoroffline;

public class RepetitionRTLFragment extends RepetitionFragment {
	
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

