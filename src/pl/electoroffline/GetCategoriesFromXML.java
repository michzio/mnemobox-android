/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 *
 * @author Michał Ziobro
 */
public class GetCategoriesFromXML {
    private static final String ns = null;
    public HashMap<Integer,String> categoryPLTitles;
    public HashMap<Integer,String> categoryENTitles;
    
    GetCategoriesFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       categoryPLTitles = new HashMap<Integer,String>();
       categoryENTitles = new HashMap<Integer,String>();
       
       parser.require(XmlPullParser.START_TAG, ns, "categories");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the lektura tag
            if (name.equals("category")) {
                int cid =  Integer.parseInt(parser.getAttributeValue(null, "cid"));
                String[] output = readCategoryElement(parser);
                categoryPLTitles.put(cid, output[0]);
                categoryENTitles.put(cid, output[1]);
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
    
    public HashMap<Integer, String> getCategoryPLTitles() { 
        
        return this.categoryPLTitles;
    }
     
    public HashMap<Integer, String> getCategoryENTitles() { 
        
        return this.categoryENTitles;
    }
    
    private String[] readCategoryElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        String plTitle = "";
        String enTitle = "";
        
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("angname")) {
                enTitle = readENTitle(parser);
                //title = "przeczytalem";
                
            } else if (name.equals("plname")) { 
                //streszczenie = "znalaz��em element";
               plTitle = readPLTitle(parser);  
            } else { 
                skip(parser);
            }
         }
         
         String[] result;
         result = new String[2];
         result[0] = plTitle;
         result[1] = enTitle;
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
