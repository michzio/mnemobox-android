/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import pl.elector.service.LearningHistoryItem;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

        
/**
 * @author Michał Ziobro
 */
public class HistoryActivity extends DrawerActivity {
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
    private void generateListOfHistory() { 
  /*      layout = new LinearLayout(this); 
        GetHistoryFromXML historyObjReader = GetHistoryFromXML.getHistoryObject(this);
        if(historyObjReader != null) { 
        for(GetHistoryFromXML.HistoryObject historyObj : historyObjReader.historiesObjects) { 
            generateListItem(historyObj); 
            marginTop += (int) (80*scale + 0.5f); 
        }
        int pxPadding = (int) (10*scale + 0.5f); 
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
        layout.setScrollContainer(true);
        sv.addView(layout);
        } else { 
            Toast.makeText(this, getResources().getString(R.string.internet_lost), 
                                Toast.LENGTH_SHORT).show();
        } */
    }
    
    @SuppressWarnings("deprecation")
	private void generateListItem(LearningHistoryItem history) { 
               
  /*      RelativeLayout rl = new RelativeLayout(this);
        TextView wordsetName = new TextView(this); 
        wordsetName.setText(history.wordsetTitle);           
       // Resources resources = this.getResources();
        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
       // Drawable drawable = resources.getDrawable(R.drawable.button_shape);
       // wordsetName.setBackgroundDrawable(drawable);
       // Display display=getWindowManager().getDefaultDisplay();
       // int width=display.getWidth();
       // wordsetName.setWidth(width-20);
       // wordsetName.setHeight(80);
        wordsetName.setGravity(Gravity.LEFT);
        int pxPadding = (int) (5*scale + 0.5f);
        wordsetName.setPadding(0,pxPadding,0,0);     
        wordsetName.setTextSize(12);
        wordsetName.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        Resources resources = this.getResources();
        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
        rl.setBackgroundDrawable(drawable);
        rl.addView(wordsetName,rlParams);
        
        TextView stats = new TextView(this); 
        stats.setText(
                Html.fromHtml(getResources().getString(R.string.history_times) + history.hits 
                + ", " + getResources().getString(R.string.history_effectivness)
                +  history.effectivness ));
        stats.setGravity(Gravity.LEFT);
        stats.setPadding(0,pxPadding,0,0);     
        stats.setTextSize(14);
        stats.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams rlParams3 = new RelativeLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int pxMargin = (int) (28*scale + 0.5f); 
        rlParams3.setMargins(0, pxMargin, 0, 0);
        rl.addView(stats,rlParams3);
        
        TextView modeName = new TextView(this); 
        modeName.setText(
                Html.fromHtml("<font color='#AB1E35'>"+ history.modeTitle + "</font><br />"
                + getResources().getString(R.string.history_last_access) + history.lastAccess ));
        modeName.setGravity(Gravity.LEFT);
        modeName.setPadding(0,pxPadding,0,0);     
        modeName.setTextSize(14);
        modeName.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams rlParams2 = new RelativeLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int pxMargin2 = (int) (45*scale + 0.5f); 
        rlParams2.setMargins(0, pxMargin2, 0, 0);
        rl.addView(modeName,rlParams2);
         
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int pxMargin3 = (int) (10*scale + 0.5f); 
        layoutParams.setMargins(pxMargin3, 0, pxMargin3, pxMargin3);
        
        layout.addView(rl, layoutParams); */
    }
    
    private HistoryFragment historyFragment; 
    private User user; 
    

	@Override
	protected void onCreateDrawerActivity(Bundle savedInstanceState) {
		  
		setContentView(R.layout.main_drawer); 

		// adding initial fragment using Fragment Transaction
        FragmentManager fragmentManager =  getSupportFragmentManager(); 
        historyFragment = (HistoryFragment) fragmentManager.findFragmentByTag(HistoryFragment.TAG);
        
        // IMPORTANT TO RETAIN CURRENT FRAGMENT ON SCREEN WHILE ex. ROTATING DEVICE
        if(historyFragment == null) { 
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        historyFragment = new HistoryFragment();
	        fragmentTransaction.replace(R.id.main_content_frame, historyFragment, HistoryFragment.TAG); 
	        
	        fragmentTransaction.commit();
        }
        
        user = new User(this);
    	if(!user.isLoggedIn()) { 
    		
    	} 
	}

	@Override
	protected int getRightDrawerMenuButtonId() {
	
		return R.id.taskNotificationsBtn;
	}
}
