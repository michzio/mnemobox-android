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
import android.media.audiofx.BassBoost.Settings;
import android.util.Log;

/**
 *
 * @author bochzio
 */
public class GetUserForgottenFromXML {
    private static final String ns = null;
    // private static String forgottenUrl = "http://www.mnemobox.com/webservices/userForgotten.xml.php?from=en&to=pl";
    public ArrayList<ForgottenWord> forgottenWords;

    GetUserForgottenFromXML(InputStream xmlStream) { 
    	 
      forgottenWords = new ArrayList<ForgottenWord>();
    	 
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       parser.require(XmlPullParser.START_TAG, ns, "forgotten");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the lektura tag
            if (name.equals("wordRow")) {
              
                readWordRow(parser);
                
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
    
  
    private void readWordRow(XmlPullParser parser) throws XmlPullParserException, IOException {
         
        ForgottenWord word = new ForgottenWord(); 
        word.forgottenId =  Integer.parseInt(parser.getAttributeValue(null, "wid"));
        word.translationId = Integer.parseInt(parser.getAttributeValue(null, "tid"));
        word.weight = Integer.parseInt(parser.getAttributeValue(null, "weight"));
        
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("enWord")) {
               word.englishName = readEnglishName(parser);       
            } else if (name.equals("plWord")) { 
               word.polishName = readPolishName(parser);  
            } else if (name.equals("recording")) { 
               word.recording = readRecording(parser); 
            } else if (name.equals("part")) { 
               word.part = readPart(parser); 
            } else { 
                skip(parser);
            }
         }
         forgottenWords.add(word);
                 
     }
   
     private String readEnglishName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "enWord");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "enWord");
        return title;
    }
     private String readPolishName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "plWord");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "plWord");
        return title;
    }
     private String readRecording(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "recording");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "recording");
        return title;
    }
      private String readPart(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "part");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "part");
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
    
    public class ForgottenWord { 
        public int forgottenId; 
        public int translationId;
        public int weight;
        public String englishName;
        public String polishName;
        public String recording; 
        public String part; 
    }
    
      public static GetUserForgottenFromXML getForgottenWordsReader(Context ctx) { 
    	
    	String nativeCode = Preferences.getAccountPreferences(ctx)
    			.getString(SettingsFragment.KEY_NATIVE_LANGUAGE_PREFERENCE, ctx.getString(R.string.native_code_lower)); 
    	String foreignCode = Preferences.getAccountPreferences(ctx)
    			.getString(SettingsFragment.KEY_FOREIGN_LANGUAGE_PREFERENCE, ctx.getString(R.string.foreign_code_lower)); 
    	String email = Preferences.getString(ctx, Preferences.KEY_EMAIL, ""); 
    	String pass = Preferences.getString(ctx, Preferences.KEY_SHA1_PASSWORD, "");
    	
        String forgottenUrl = ctx.getString(R.string.forgotten_url, nativeCode, foreignCode, email, pass);
        Log.w(GetUserForgottenFromXML.class.getName(), "Get user forgotten url: " + forgottenUrl); 
         
         try  { 
            InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(forgottenUrl);
            GetUserForgottenFromXML forgottenWordReader = new GetUserForgottenFromXML(is);
           
            try { 
            is.close();
            } catch(java.io.IOException e) { } 
             return forgottenWordReader; 
        } catch (Exception e) { }
        
        return null; 
        
    }
   
}
