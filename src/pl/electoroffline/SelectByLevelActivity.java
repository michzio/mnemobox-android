package pl.electoroffline;

import info.semsamot.actionbarrtlizer.ActionBarRtlizer;
import info.semsamot.actionbarrtlizer.RtlizeEverything;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class SelectByLevelActivity extends ActionBarActivity
{
     
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selectbylevel);
        
        // ActionBar modification 
   		ActionBar actionBar = getSupportActionBar();
   		actionBar.setTitle(getString(R.string.bylevels));
   		actionBar.setSubtitle(null);
 
        buttonEvents();
        
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
    	
		if(DrawerActivity.isRTL()) { 
			Log.d(DrawerActivity.class.getName(), "Configuring RTL action bar..."); 
	        // RTLizing ActionBar and it's children.
	        ActionBarRtlizer rtlizer = new ActionBarRtlizer(this);
	 
	        RtlizeEverything.rtlize(rtlizer.getActionBarView());
	 
	       if (rtlizer.getHomeViewContainer() instanceof ViewGroup) {
	            RtlizeEverything.rtlize((ViewGroup) rtlizer.getHomeViewContainer());
	        }
	 
	        ViewGroup homeView = (ViewGroup) rtlizer.getHomeView();
	        RtlizeEverything.rtlize(homeView);
	        rtlizer.flipActionBarUpIconIfAvailable(homeView);
	        
	        RtlizeEverything.rtlize((ViewGroup)rtlizer.getActionMenuView());
		}
		return super.onCreateOptionsMenu(menu);
	}
    
    private void buttonEvents() { 
        
        Button levelA1Btn = (Button)findViewById(R.id.levelA1Btn);
        levelA1Btn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
            runWordsetsByLevel("A1");
                
            }
        });
        Button levelA2Btn = (Button)findViewById(R.id.levelA2Btn);
        levelA2Btn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
            runWordsetsByLevel("A2");
            }
        });
       Button levelB1Btn = (Button)findViewById(R.id.levelB1Btn);
         levelB1Btn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
             runWordsetsByLevel("B1");   
                
            }
        });
         Button levelB2Btn = (Button)findViewById(R.id.levelB2Btn);
         levelB2Btn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
               runWordsetsByLevel("B2"); 
                
            }
        });
        Button levelC1Btn = (Button)findViewById(R.id.levelC1Btn);
         levelC1Btn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
               runWordsetsByLevel("C1"); 
                
            }
        });
         Button levelC2Btn = (Button)findViewById(R.id.levelC2Btn);
         levelC2Btn.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View arg0) {
            // TODO Auto-generated method stub
               runWordsetsByLevel("C2"); 
                
            }
        });
    }
    private void runWordsetsByLevel(String level) { 
           
            Intent levelIntent = new Intent(SelectByLevelActivity.this, WordsetsListActivity.class);
            levelIntent.putExtra(WordsetsListActivity.SELECTED_LEVEL, level);
            
            startActivity(levelIntent);    
                
    }
    
}
