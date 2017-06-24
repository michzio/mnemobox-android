/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

/**
 *
 * @author bochzio
 */
public class ProfileStatsActivity extends Activity {
     
       @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profilestats);
      
       LinearLayout statsLayout = (LinearLayout) findViewById(R.id.statsLayout);
       

        GraphView graphView = new LineGraphView(  
            this // context  
            , getResources().getString(R.string.stats_header) // heading  
        ){  
                @Override  
                protected String formatLabel(double value, boolean isValueX) {  
                    if (isValueX) {  
                        // convert unix time to human time  
                        SimpleDateFormat sdf = new SimpleDateFormat("M/dd");
                        String date = sdf.format((long) value);
                        return date; 
                    } else return super.formatLabel(value, isValueX); // let the y-value be normal-formatted  
                    }  
         } ;  
        try  { 
       graphView.addSeries(getBadStatsSeries());
       graphView.addSeries(getGoodStatsSeries());
       graphView.addSeries(getEffStatsSeries());// data  
        } catch(NullPointerException e) {
            Toast.makeText(this, R.string.internet_lost,
                                Toast.LENGTH_SHORT).show();
        }
        graphView.setScalable(true);
        graphView.setShowLegend(true);
        statsLayout.addView(graphView);  
    }
    
    private GraphViewSeries getBadStatsSeries() throws NullPointerException { 
        
        GetStatsFromXML stats = GetStatsFromXML.getStatsObject(this); 
        LinkedHashMap<Long, Integer> badStats = stats.getBadStats();
         
        GraphViewSeries statsSeries = new GraphViewSeries(
                getResources().getString(R.string.stats_badans), new GraphViewSeries.GraphViewStyle(0xff333333, 2) , 
                getStatsData(badStats));
       
        return statsSeries; 
    }
      private GraphViewSeries getGoodStatsSeries() throws NullPointerException { 
        
        GetStatsFromXML stats = GetStatsFromXML.getStatsObject(this);
        LinkedHashMap<Long, Integer> goodStats = stats.getGoodStats();
        
        GraphViewSeries statsSeries = new GraphViewSeries(
                getResources().getString(R.string.stats_goodans), new GraphViewSeries.GraphViewStyle(0xffAB1E35, 2),
                   getStatsData(goodStats));
        return statsSeries; 
    }
      private GraphViewSeries getEffStatsSeries() throws NullPointerException { 
        
        GetStatsFromXML stats = GetStatsFromXML.getStatsObject(this);
        LinkedHashMap<Long, Integer> effStats = stats.getEffStats();
        GraphViewSeries statsSeries = new GraphViewSeries(
                getResources().getString(R.string.stats_effectivness), null, getStatsData(effStats));
        return statsSeries; 
    }
    
    private GraphViewData[] getStatsData(LinkedHashMap<Long, Integer> statsMap) {
        GraphViewData[] graphData;
        if(statsMap.size() > 0) { 
             graphData = new GraphViewData[statsMap.size()];
            int i = 0; 
            for(long timestamp : statsMap.keySet()) { 
            
             graphData[i] = new GraphViewData(timestamp,  statsMap.get(timestamp));
            /* Toast.makeText(this, String.valueOf(timestamp) + " - " + String.valueOf(badStats.get(timestamp)),
                                Toast.LENGTH_SHORT).show();*/
             i++; 
             }
        } else {
            graphData = new GraphViewData[1];
            graphData[0] = new GraphViewData(0,0);
        }
        
        return graphData; 
    }
   
       
}
