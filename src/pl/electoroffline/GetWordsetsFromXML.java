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

/**
 *
 * @author Michał Ziobro
 */
public class GetWordsetsFromXML {
    private static final String ns = null;
    public LinkedHashMap<Integer,String> wordsetsPLTitles;
    public LinkedHashMap<Integer,String> wordsetsENTitles;
    public LinkedHashMap<Integer,String> wordsetsLevels;
    public LinkedHashMap<Integer,String> wordsetsDescriptions;
    public LinkedHashMap<Integer, String> wordsetsCategoryIds;
    
    GetWordsetsFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       wordsetsPLTitles = new LinkedHashMap<Integer,String>();
       wordsetsENTitles = new LinkedHashMap<Integer,String>();
       wordsetsLevels = new LinkedHashMap<Integer,String>();
       wordsetsDescriptions = new LinkedHashMap<Integer,String>();
       wordsetsCategoryIds = new LinkedHashMap<Integer,String>();
       
       parser.require(XmlPullParser.START_TAG, ns, "wordsets");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the wordset tag
            if (name.equals("wordset")) {
                int wid =  Integer.parseInt(parser.getAttributeValue(null, "wid"));
                String[] output = readWordsetElement(parser);
                wordsetsPLTitles.put(wid, output[0]);
                wordsetsENTitles.put(wid, output[1]);
                wordsetsLevels.put(wid, output[2]);
                wordsetsDescriptions.put(wid, output[3]);
                wordsetsCategoryIds.put(wid, output[4]);
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
    
    public LinkedHashMap<Integer, String> getWordsetsPLTitles() { 
        
        return wordsetsPLTitles;
    }
     
    public LinkedHashMap<Integer, String> getWordsetsENTitles() { 
        
        return wordsetsENTitles;
    }
    public LinkedHashMap<Integer, String> getWordsetsLevels() { 
        
        return wordsetsLevels;
    }
    public LinkedHashMap<Integer, String> getWordsetsDescriptions() { 
        
        return wordsetsDescriptions;
    }
    public LinkedHashMap<Integer, String> getWordsetsCategoryIds() {
    	return wordsetsCategoryIds;
    }
    
    private String[] readWordsetElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        String plTitle = "";
        String enTitle = "";
        String level = "";
        String desc = "";
        String categoryId = "";
        
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("angname")) {
                enTitle = readENTitle(parser);   
            } else if (name.equals("plname")) { 
               plTitle = readPLTitle(parser);  
            } else if (name.equals("level")) { 
               level = readLevel(parser); 
            } else if (name.equals("description")) { 
               desc = readDescription(parser); 
            } else if (name.equals("category")) { 
            	categoryId =  parser.getAttributeValue(null, "cid");
            	skip(parser); //?
            }else { 
                skip(parser);
            }
         }
         
         String[] result;
         result = new String[5];
         result[0] = plTitle;
         result[1] = enTitle;
         result[2] = level;
         result[3] = desc; 
         result[4] = categoryId; 
         return result;          
     }
    
     private String readENTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "angname");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "angname");
        return title;
    }
     private String readPLTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "plname");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "plname");
        return title;
    }
     private String readLevel(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "level");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "level");
        return title;
    }
      private String readDescription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "description");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "description");
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
}
