/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

        
/**
 *
 * @author bochzio
 */
public class MoneyHistoryActivity extends Activity {
    private LinearLayout layout; 
    private ScrollView sv; 
    private float scale; 
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.moneyhistory);
        scale = this.getResources().getDisplayMetrics().density; 
        sv = (ScrollView) findViewById(R.id.moneyHistoryScrollView); 
        generateListOfHistory(); 
    }
    
    private void generateListOfHistory() { 
        layout = new LinearLayout(this); 
        GetMoneyHistoryFromXML moneyHistoryObjReader = GetMoneyHistoryFromXML.getHistoryObjectReader(this);
        if(moneyHistoryObjReader != null) { 
        for(GetMoneyHistoryFromXML.MoneyHistoryObject mhistoryObj : moneyHistoryObjReader.moneyHistoriesObjects) { 
            generateListItem(mhistoryObj); 
        }
        layout.setOrientation(LinearLayout.VERTICAL);
        int pxPadding = (int) (10*scale + 0.5f); 
        layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
        layout.setScrollContainer(true);
        sv.addView(layout);
        } else { 
            Toast.makeText(this, R.string.internet_lost,
                                Toast.LENGTH_SHORT).show();
        }
    }
    
    @SuppressWarnings("deprecation")
	private void generateListItem(GetMoneyHistoryFromXML.MoneyHistoryObject mhistory) { 
               
        RelativeLayout rl = new RelativeLayout(this);
        TextView wordsetName = new TextView(this); 
        wordsetName.setText(mhistory.description);           
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
                Html.fromHtml(getResources().getString(R.string.money_transaction) + mhistory.transactionType 
                + ", " + getResources().getString(R.string.money_amount)
                +  mhistory.moneyTransfer ));
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
                Html.fromHtml("<font color='#AB1E35'>"+ mhistory.wordsetTitleEN + "</font><br />"
                + getResources().getString(R.string.money_date) + mhistory.moneyHistoryDate ));
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
        
        layout.addView(rl, layoutParams);
    }
}
