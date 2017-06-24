/**
 * 
 */
package pl.electoroffline;



import com.google.android.gms.plus.PlusOneButton;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * @author Michał Ziobro
 *
 */
public class MainFragment extends TrackedFragment implements SwipeInterface {

	public static final String TAG = "MAIN_FRAGMENT_TAG"; 
	private View view; 
	private PlusOneButton mPlusOneButton;
	// The request code must be 0 or greater.
	private static final int PLUS_ONE_REQUEST_CODE = 0;
	
	/** 
	 * DEPRECATED MainPagerFragment introduced  @date 07.11.2014
	 *
	@Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    // Retain this fragment across configuration changes.
	    // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
	    setRetainInstance(true);
	    
	}
	**/

	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		view = inflater.inflate(R.layout.main, container, false);
		mPlusOneButton = (PlusOneButton) view.findViewById(R.id.plus_one_button);
		
		/**
		 * DEPRECATED MainPagerFragment - 07.11.2014
		 * getActivity().getSupportFragmentManager().addOnBackStackChangedListener(getListener());
		 */
		
		buttonEvents();
	     
        /**
         *  DEPRECATED MainPagerFragment - 07.11.2014
		 *	ActivitySwipeDetector swipe = new ActivitySwipeDetector(this);
	     *  RelativeLayout swipe_layout = (RelativeLayout) view.findViewById(R.id.mainSwipeLayout);
	     *  swipe_layout.setOnTouchListener(swipe);
        */
        
		return view;
	}
	
	@Override
	public void onResume() 
	{
		super.onResume();
		onFragmentResume();	
	}
	
	private void onFragmentResume() {
		ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
		actionBar.setTitle(R.string.english);
		actionBar.setSubtitle(R.string.domain_name);
		
		// Refresh the state of the +1 button each time the activity receives focus.
		if(mPlusOneButton != null)
			mPlusOneButton.initialize(getActivity().getString(R.string.app_market_url), PLUS_ONE_REQUEST_CODE);
	}
	
	/**
	 * DEPRECATED - MainPagerFragment introduced - 07.11.2014
	 * 
	 *
		private OnBackStackChangedListener getListener()
	    {
	        OnBackStackChangedListener result = new OnBackStackChangedListener()
	        {
	            public void onBackStackChanged() 
	            {   
	                FragmentManager manager = getActivity().getSupportFragmentManager();
	
	                if (manager != null)
	                {
	                    Fragment fragment = manager.findFragmentById(R.id.main_content_frame);
	                    if(fragment.getClass().getName().equals(MainFragment.class.getName())) 
	                    	((MainFragment)fragment).onFragmentResume();
	                }                   
	            }
	        };
	
	        return result;
	    }
    
    ***/

	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}
	
	private void buttonEvents() { 
        
        Button vocabularyBtn = (Button)view.findViewById(R.id.vocabularyBtn);
        // Do this for each view added to the grid
        vocabularyBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
           
            Intent vocabularyIntent = new Intent(getActivity(), WordsetCategoriesActivity.class);
            startActivity(vocabularyIntent);   
                
            }
        });
        Button dictBtn = (Button)view.findViewById(R.id.dictBtn);
        dictBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
            
            	Intent dictIntent = new Intent(getActivity(), DictActivity.class);
            	startActivity(dictIntent); 
            
            /* FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
             * fragmentManager.beginTransaction()
             *   			   .add(R.id.main_content_frame, new DictFragment(), DictFragment.TAG)
             *  			   .addToBackStack("dictFragmentBack")
             *  			   .hide(MainFragment.this)
             *  			   .commit();  
             */
            }
        });
          Button learnedBtn = (Button)view.findViewById(R.id.learnedBtn);
          learnedBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            	
            	Intent learnedWordsIntent = new Intent(getActivity(), LearnedWordsActivity.class);
                startActivity(learnedWordsIntent); 
            
               /** 
            	FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new LearnedWordsFragment(), LearnedWordsFragment.TAG)
                			   .addToBackStack("learnedWordsFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                */
                
            }
        });
         Button profileBtn = (Button)view.findViewById(R.id.profileBtn);
         profileBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
           
                Intent profileIntent = new Intent(getActivity(), ProfileInfoActivity.class);
                startActivity(profileIntent); 
                
            }
        });
        Button forgottenBtn = (Button)view.findViewById(R.id.forgottenBtn);
         forgottenBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            	
               Intent forgottenIntent = new Intent(getActivity(), ForgottenActivity.class);
               startActivity(forgottenIntent); 
               
              /** 
               FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
               fragmentManager.beginTransaction()
              				 .add(R.id.main_content_frame, new ForgottenFragment(),ForgottenFragment.TAG)
              				 .addToBackStack("forgottenFragmentBack")
              				 .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
              				 .commit();
               */
             
            }
        });
         Button toRememberBtn = (Button)view.findViewById(R.id.toRememberBtn);
         toRememberBtn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View v) {
           
                Intent rememberMeIntent = new Intent(getActivity(), RememberMeActivity.class);
                startActivity(rememberMeIntent);  
            	
                /**
            	FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
                fragmentManager.beginTransaction()
                			   .add(R.id.main_content_frame, new RememberMeFragment(), RememberMeFragment.TAG)
                			   .addToBackStack("rememberMeFragmentBack")
                			   .hide(fragmentManager.findFragmentById(R.id.main_content_frame))
                			   .commit();
                */
            }
        });
    }
	
	@Override
    public void left2right(View v) { 
		
		FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        fragmentTransaction.replace(R.id.main_content_frame, new MoreAppsFragment()); 
        
        fragmentTransaction.commit(); 
        // startActivity(new Intent(getActivity(), MoreAppsActivity.class));
    }
    @Override 
    public void right2left(View v) { 
    	FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        fragmentTransaction.replace(R.id.main_content_frame, new MoreAppsFragment()); 
        
        fragmentTransaction.commit();
        // startActivity(new Intent(getActivity(), MoreAppsActivity.class));
    }
    @Override 
    public void top2bottom(View v) { 
        
    }
    @Override 
    public void bottom2top(View v) { 
        
    }
}
