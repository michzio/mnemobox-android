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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

        
/**
 *
 * @author bochzio
 */
public class TaskActivity extends Activity {
    private LinearLayout layout; 
    private ScrollView sv; 
    private float scale; 
     
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);
        scale = this.getResources().getDisplayMetrics().density; 
        sv = (ScrollView) findViewById(R.id.taskScrollView); 
         GetTasksFromXML taskObjsReader = GetTasksFromXML.getTaskObjectsReader(this);
         if(taskObjsReader != null) { 
        generateTasks(taskObjsReader); 
         }
         
    }
    
    private void generateTasks(GetTasksFromXML taskObjsReader) { 
        layout = new LinearLayout(this); 
       
        if(taskObjsReader.tasks.size() > 0 ) { 
            for(GetTasksFromXML.TaskObject taskObj : taskObjsReader.tasks) { 
                generateListItem(taskObj); 
            }
        } else { 
            //brak zadan
            TextView taskDefault = (TextView) findViewById(R.id.taskDefault); 
            taskDefault.setVisibility(View.VISIBLE); 
        
        }
        layout.setOrientation(LinearLayout.VERTICAL);
        int pxPadding = (int) (10 *scale + 0.5f);
        layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);
        layout.setScrollContainer(true);
        sv.addView(layout);
    }
    
    @SuppressWarnings("deprecation")
	private void generateListItem(GetTasksFromXML.TaskObject task) { 
               
        RelativeLayout rl = new RelativeLayout(this);
        TextView taskText = new TextView(this); 
       
        if( (task.taskText.length() < 300) ) { 
          taskText.setText( task.taskText );  
        } else { 
          taskText.setText(task.taskText.substring(0, 300) + "..."); 
        }
       // Resources resources = this.getResources();
        //Drawable drawable = resources.getDrawable(R.drawable.buttonColor);
       // Drawable drawable = resources.getDrawable(R.drawable.button_shape);
       // wordsetName.setBackgroundDrawable(drawable);
       // Display display=getWindowManager().getDefaultDisplay();
       // int width=display.getWidth();
       // wordsetName.setWidth(width-20);
       // wordsetName.setHeight(80);
        taskText.setGravity(Gravity.LEFT);
        int pxPadding = (int) (5*scale + 0.5f);
        taskText.setPadding(0,pxPadding,0,0);     
        taskText.setTextSize(12);
        taskText.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        Resources resources = this.getResources();
        Drawable drawable = resources.getDrawable(R.drawable.button_shape);
        rl.setBackgroundDrawable(drawable);
        int pxMargin = (int) (5*scale + 0.5f);
        rlParams.setMargins(0, 10*pxMargin, 0, 0);
        rl.addView(taskText,rlParams);
        
        TextView taskInfo = new TextView(this); 
        taskInfo.setText(
                Html.fromHtml(getResources().getString(R.string.task_added) 
                + task.createdDate + "<br /> " + getResources().getString(R.string.task_category)
                +  task.taskCategory ));
        taskInfo.setGravity(Gravity.LEFT);
        taskInfo.setPadding(0,pxPadding,0,0);     
        taskInfo.setTextSize(14);
        taskInfo.setTextColor(Color.BLACK);
        RelativeLayout.LayoutParams rlParams3 = new RelativeLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlParams3.setMargins(0, pxMargin, 0, 0);
        rl.addView(taskInfo,rlParams3);
        
        
         
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int pxMargin2 = (int) (10*scale + 0.5f); 
        layoutParams.setMargins(pxMargin2, 0, pxMargin2, pxMargin2);
        
        layout.addView(rl, layoutParams);
    }
}
