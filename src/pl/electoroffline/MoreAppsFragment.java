/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
/**
 *
 * @author Michał Ziobro 
 */
public class MoreAppsFragment extends Fragment implements SwipeInterface {
	
	View view; 

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
    	view = inflater.inflate(R.layout.moreapps, container, false);
        
    	/**
    	 * DEPRECATED - MainPagerFragment introduced 07.11.2014 
    	 *
	        ActivitySwipeDetector swipe = new ActivitySwipeDetector(this);
	        RelativeLayout swipe_layout = (RelativeLayout) view.findViewById(R.id.moreappsSwipeLayout);
	        swipe_layout.setOnTouchListener(swipe); 
	        buttonEvents();
        **/
    	
    	buttonEvents(); 
        
        return view; 
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
	}
    
    @Override
    public void left2right(View v) { 
    	FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        fragmentTransaction.replace(R.id.main_content_frame, new MainFragment()); 
        
        fragmentTransaction.commit();
        // startActivity(new Intent(this, MainActivity.class));
    }
    @Override 
    public void right2left(View v) { 
    	FragmentManager fragmentManager = getActivity().getSupportFragmentManager(); 
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        fragmentTransaction.replace(R.id.main_content_frame, new MainFragment()); 
        
        fragmentTransaction.commit();
        // startActivity(new Intent(this, MainActivity.class));  
    }
    @Override 
    public void top2bottom(View v) { 
        
    }
    @Override 
    public void bottom2top(View v) { 
        
    }
    
    private void goToApp(String uri) { 
    	
    	Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(uri));
        startActivity(intent);
    }
    
	private void buttonEvents() { 
        
        ImageButton englishBtn = (ImageButton)view.findViewById(R.id.downloadEnglishBtn);
        // Do this for each view added to the grid
        englishBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View arg0) {        	
            	goToApp("market://details?id=pl.electoroffline"); 
            }
        });
        ImageButton germanBtn = (ImageButton)view.findViewById(R.id.downloadGermanBtn);
        germanBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorniemieckioffline");                
            }
        });
        ImageButton spanishBtn = (ImageButton)view.findViewById(R.id.downloadSpanishBtn);
        spanishBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorspanishoffline");  
            }
        });
        ImageButton frenchBtn = (ImageButton)view.findViewById(R.id.downloadFrenchBtn);
        frenchBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorfrenchoffline");
            }
        });
        ImageButton italianBtn = (ImageButton)view.findViewById(R.id.downloadItalianBtn);
        italianBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electoritalianoffline"); 
            }
        });
        ImageButton polishBtn = (ImageButton)view.findViewById(R.id.downloadPolishBtn);
        polishBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorpolishoffline");
            }
        });
        ImageButton portugueseBtn = (ImageButton)view.findViewById(R.id.downloadPortugueseBtn);
        portugueseBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorportugueseoffline");
            }
        });
        ImageButton russianBtn = (ImageButton)view.findViewById(R.id.downloadRussianBtn);
        russianBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorrussianoffline");
            }
        });
        ImageButton dutchBtn = (ImageButton)view.findViewById(R.id.downloadDutchBtn);
        dutchBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electordutchoffline");
            }
        });
        ImageButton romanianBtn = (ImageButton)view.findViewById(R.id.downloadRomanianBtn);
        romanianBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorromanianoffline");
            }
        });
        ImageButton turkishBtn = (ImageButton)view.findViewById(R.id.downloadTurkishBtn);
        turkishBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorturkishoffline");
            }
        });
        ImageButton arabicBtn = (ImageButton)view.findViewById(R.id.downloadArabicBtn);
        arabicBtn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View arg0) {
            	goToApp("market://details?id=pl.electorarabicoffline");
            }
        });
    }
}
