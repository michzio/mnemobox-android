/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 *
 * @author Michał Ziobro
 */
public class GetStatsFromXML {
    private static final String ns = null;
    //private static String statsUrl = "http://www.mnemobox.com/webservices/userStats.xml.php?from=en&to=pl&";
    private LinkedHashMap<Long, Integer> badStats;
    private LinkedHashMap<Long, Integer> goodStats;
    private LinkedHashMap<Long, Integer> effStats;
    public static Context context; 
    
    
    GetStatsFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       badStats = new LinkedHashMap<Long, Integer>(); 
       goodStats = new LinkedHashMap<Long, Integer>(); 
       effStats = new LinkedHashMap<Long, Integer>(); 
      
       parser.require(XmlPullParser.START_TAG, ns, "stats");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the lektura tag
            if (name.equals("badStats")) {
                
               String badStatsStr = readText(parser);
               badStats = convertStats(badStatsStr);
               
                

            } else if (name.equals("goodStats")) {
                String goodStatsStr = readText(parser);
                goodStats = convertStats(goodStatsStr);
                
            } else if (name.equals("statsEff")) {
                String effStatsStr = readText(parser);
                effStats = convertStats(effStatsStr);
               
               
            }  else { 
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
    
    public LinkedHashMap<Long, Integer> getBadStats() { 
        return this.badStats;
    }
     public LinkedHashMap<Long, Integer> getGoodStats() { 
        return this.goodStats;
    }
     public LinkedHashMap<Long, Integer> getEffStats() { 
        return this.effStats;
    }
    private LinkedHashMap<Long, Integer> convertStats(String statsStr) { 
        String simplified = statsStr.replace("[ [", "[").replace("] ]", "]");
        
       /* Toast.makeText(context, simplified,
                                Toast.LENGTH_SHORT).show();*/
        String[] daysStats;
        daysStats = simplified.split("],");
        LinkedHashMap<Long, Integer> statsMap =  new LinkedHashMap<Long, Integer>(); 
         
        for(String dayStat : daysStats) { 
             String str = dayStat.replace("[", "").replace("]", "").trim().replace(", ",":"); 
             String[] dayAndStat = str.split(":");
             
              
            if(dayAndStat[1].contains(".")) { 
                dayAndStat[1] = dayAndStat[1].substring(0, dayAndStat[1].indexOf("."));
            }
           /* Toast.makeText(context, dayAndStat[0] +  " i " + dayAndStat[1],
                                Toast.LENGTH_SHORT).show();*/
             statsMap.put( Long.parseLong(dayAndStat[0].trim()), Integer.parseInt(dayAndStat[1].trim()));
            /* if(dayAndStat.length == 2) { 
         
             
            }*/
        }
        return statsMap; 
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
    
    public static GetStatsFromXML getStatsObject(Context ctx) { 
        GetStatsFromXML.context = ctx; 
        
        
        String nativeCode = Preferences.getAccountPreferences(ctx)
        		.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, ctx.getString(R.string.native_code_lower));
        String foreignCode = Preferences.getAccountPreferences(ctx)
        		.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, ctx.getString(R.string.foreign_code_lower));
        String email = Preferences.getString(ctx, Preferences.KEY_EMAIL, ""); 
        String pass = Preferences.getString(ctx, Preferences.KEY_SHA1_PASSWORD, "");
       
        String url = ctx.getString(R.string.stats_url, nativeCode, foreignCode, email, pass);
        Log.d(GetStatsFromXML.class.getName(), "Get stats url: " + url); 
         
         try  { 
            InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
            GetStatsFromXML userStats = new GetStatsFromXML(is);
           
            try { 
            is.close();
            } catch(java.io.IOException e) { } 
             return userStats; 
        } catch (Exception e) { }
        
        return null; 
        
    }

}


