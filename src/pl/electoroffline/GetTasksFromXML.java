/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 *
 * @author bochzio
 */
public class GetTasksFromXML {
    private static final String ns = null;
    //private static String tasksUrl = "http://www.mnemobox.com/webservices/userTasks.php?from=en&to=pl&";
    public ArrayList<TaskObject> tasks;

    GetTasksFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       tasks = new ArrayList<TaskObject>();

       
       parser.require(XmlPullParser.START_TAG, ns, "tasks");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the lektura tag
            if (name.equals("taskRow")) {
              
                readTaskRow(parser);
                
            } else {
                skip(parser);
            }
       }
       //txtview.setText(strLekturaTitle);
            
      } catch (Exception e) 
       {
           //insert something when error
       } finally {
       }
    
    }
    
  
    private void readTaskRow(XmlPullParser parser) throws XmlPullParserException, IOException {
         
        TaskObject task = new TaskObject(); 
        task.taskid =  Integer.parseInt(parser.getAttributeValue(null, "taskid"));
        task.categoryid = Integer.parseInt(parser.getAttributeValue(null, "categoryid"));
        task.solutionCount = Integer.parseInt(parser.getAttributeValue(null, "solutionCount"));
        
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("taskText")) {
               task.taskText = readTaskText(parser);       
            } else if (name.equals("taskCategory")) { 
               task.taskCategory = readTaskCategory(parser);  
            } else if (name.equals("createdDate")) { 
               task.createdDate = readCreatedDate(parser); 
            } else { 
                skip(parser);
            }
         }
         tasks.add(task);
                 
     }
   
     private String readTaskText(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "taskText");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "taskText");
        return title;
    }
     private String readTaskCategory(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "taskCategory");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "taskCategory");
        return title;
    }
     private String readCreatedDate(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "createdDate");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "createdDate");
        return title;
    }    
      // For the tags title and summary, extracts their text values.
    private static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
// Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                    depth--;
                    break;
            case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    
    public class TaskObject {  
        public int taskid;
        public int categoryid; 
        public int solutionCount;
        public String taskText;
        public String taskCategory;
        public String createdDate;
    }
    
      public static GetTasksFromXML getTaskObjectsReader(Context ctx) {  
       
       String nativeCode = Preferences.getAccountPreferences(ctx)
    		   .getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, ctx.getString(R.string.native_code_lower));
       String foreignCode = Preferences.getAccountPreferences(ctx)
    		   .getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, ctx.getString(R.string.foreign_code_lower)); 
       String email = Preferences.getString(ctx, Preferences.KEY_EMAIL, ""); 
       String pass = Preferences.getString(ctx, Preferences.KEY_SHA1_PASSWORD, ""); 
    
       String tasksUrl = ctx.getString(R.string.tasks_url, nativeCode, foreignCode, email, pass); 
       Log.d(GetTasksFromXML.class.getName(), "Get tasks url: " + tasksUrl); 
        
       try  { 
            InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(tasksUrl);
            GetTasksFromXML taskObjsReader = new GetTasksFromXML(is);
           
            try { 
            is.close();
            } catch(java.io.IOException e) { } 
             return taskObjsReader; 
        } catch (Exception e) { }
        
        return null; 
        
    }
}
