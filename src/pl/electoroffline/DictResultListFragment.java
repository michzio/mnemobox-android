package pl.electoroffline;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class DictResultListFragment extends Fragment {
	
	public static final String TAG = "DICT_RESULT_LIST_FRAGMENT_TAG"; 
	
	private View view; 
	private ScrollView scrollview; 
	private RelativeLayout resultList; 
	
	private int marginTop = 0; 
	private float scale;
	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		
		view = inflater.inflate(R.layout.dict_result_list, container, false);
	  
        scrollview = (ScrollView) view.findViewById(R.id.dictScrollView);
        resultList = (RelativeLayout) view.findViewById(R.id.dictResultList);
        attachListeners();
        scale = getResources().getDisplayMetrics().density;
        
		return view;
	}
	
	private void attachListeners() {
		
	}
	
	public void displayWordsInScrollView(HashMap<Integer, WordObject> wordsDetails) {
		 resultList.removeAllViews();
	        marginTop = 0; 
	        for(int tid : wordsDetails.keySet() ) { 
	            makeWordIntro(wordsDetails.get(tid), tid);
	            makeImage(wordsDetails.get(tid).getImages(), tid);
	           
	            marginTop += (int) (80*scale + 0.5f);
	        }
	}
	
	 @SuppressWarnings("deprecation")
	private void makeWordIntro(WordObject wo, int tid) { 
		    Button button = new Button(getActivity());
	       
	        button.setText(
	                Html.fromHtml("<font color='#AB1E35'>"+ wo.getEnWord() + "</font><br/>"
	                + "<small>"+ wo.getPlWord() +"</small>"));
	        Resources resources = this.getResources();
	        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
	        button.setBackgroundDrawable(drawable);
	        
	        DisplayMetrics displaymetrics = new DisplayMetrics();
	        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	        int width = displaymetrics.widthPixels;
	        int pxWidth = (int) ( 80*scale + 0.5f); 
	        int pxOffset = (int) (20*scale + 0.5f);
	        int pxPadding = (int) (10*scale + 0.5f); 
	        int pxMarginLeft = (int) (85*scale + 0.5f); 
	        button.setWidth(width-pxOffset);
	        button.setHeight(pxWidth);
	        button.setGravity(Gravity.LEFT);
	        button.setPadding(0,pxPadding,0,0);
	        button.setTextSize(16);
	        button.setTextColor(Color.BLACK);
	        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
	                  RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        layoutParams.setMargins(pxMarginLeft, marginTop, pxPadding, 0);
	        
	        button.setTag(tid);
	        button.setOnClickListener(((DictActivity)getActivity()));
	        resultList.addView(button, layoutParams);
	 }
	 
	 
	  @SuppressWarnings("deprecation")
	private void makeImage(ArrayList<String> images, int tid) { 
	        ImageView imgview = new ImageView(getActivity());
	       
	        Resources resources = this.getResources();
	        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
	        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
	        imgview.setBackgroundDrawable(drawable);
	        
	        DisplayMetrics displaymetrics = new DisplayMetrics();
	        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	        // int width = displaymetrics.widthPixels;
	        
	        int pxPadding = (int) (5*scale + 0.5f);
	        imgview.setPadding(0,pxPadding,0,0);
	        if(images != null && images.size() > 0) {
	         UrlImageViewHelper.setUrlDrawable(imgview, 
	                    getString(R.string.images_url) + images.get(0));
	        }
	        int pxSize = (int) (70*scale + 0.5f); 
	        int pxMargin = (int) (10*scale + 0.5f); 
	        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
	                  pxSize, pxSize);
	        layoutParams.leftMargin = pxMargin;
	        layoutParams.topMargin = marginTop;
	       
	        
	        imgview.setTag(tid);
	        imgview.setOnClickListener((DictActivity)getActivity());
	        resultList.addView(imgview, layoutParams);
	    }
}
