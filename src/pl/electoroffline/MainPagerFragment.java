package pl.electoroffline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

public class MainPagerFragment extends Fragment {

	public static final String TAG = "MAIN_PAGER_FRAGMENT_TAG"; 
	
	private View view; 
	
	// When requested, this adapter returns a Fragment,
    // representing an object in the collection.
	private MainPagerAdapter mainPagerAdapter;
	private ViewPager viewPager;
	
	@Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    // Retain this fragment across configuration changes.
	    // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
	    setRetainInstance(true);
	    
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
	}
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.main_pager, container, false);
		
		getActivity().getSupportFragmentManager().addOnBackStackChangedListener(getListener());
		
		// ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
		mainPagerAdapter =  new MainPagerAdapter(getActivity().getSupportFragmentManager());
		viewPager = (ViewPager) view.findViewById(R.id.mainpager);
        viewPager.setAdapter(mainPagerAdapter);
        
        if(DrawerActivity.isRTL()) { 
        	viewPager.setCurrentItem(mainPagerAdapter.getCount()-1);
        } else { 
        	viewPager.setCurrentItem(0);
        }
        
        viewPager.setOnPageChangeListener(onPageChangeListener); 
		
		return view; 
	}
	
	private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
		
		@Override
		public void onPageSelected(int arg0) {
			hideKeyboard();
		}

		@Override
		public void onPageScrollStateChanged(int arg0) { }

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) { }
	
	};
	
	private void hideKeyboard() {   
	    // Check if no view has focus:
	    View view = getActivity().getCurrentFocus();
	    if (view != null) {
	        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	        inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	    }
	}
	
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
                    if(fragment.getClass().getName().equals(MainPagerFragment.class.getName())) 
                    	((MainPagerFragment)fragment).onFragmentResume();
                }                   
            }
        };

        return result;
    }
	
	
	private class MainPagerAdapter extends FragmentPagerAdapter {
		
		private List<String> pagerFragments;	
		
		public MainPagerAdapter(FragmentManager fm) {
			super(fm);
			
			pagerFragments = Arrays.asList(getResources().getStringArray(R.array.pager_fragments));
			
			if(DrawerActivity.isRTL()) { 
				Collections.reverse(pagerFragments);
			}
		}

		@Override
		public Fragment getItem(int idx) {
		
			Fragment fragment= null; 
			
			if(idx < pagerFragments.size() && idx > -1) { 
				fragment = Fragment.instantiate(getActivity(), pagerFragments.get(idx)); 
			}
			
			/** DEPRECATED
				switch(idx) {
					case 0:
						fragment = Fragment.instantiate(getActivity(), MainFragment.class.getName());
						break;
					case 1: 
						fragment = Fragment.instantiate(getActivity(), MoreAppsFragment.class.getName());
						break; 
					case 2: 
						fragment = Fragment.instantiate(getActivity(), FeedbackFragment.class.getName());
						break;
				}
			*/
			return fragment;
		}

		@Override
		public int getCount() {
			
			return pagerFragments.size();
		} 
		
	}
	
	
}
