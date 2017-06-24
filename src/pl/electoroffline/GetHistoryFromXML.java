/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import pl.elector.service.LearningHistoryItem;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * @author Michał Ziobro
 */
public class GetHistoryFromXML implements Iterable<LearningHistoryItem> {
	
    private static final String ns = null;
    
    // learning history items read from XML web service
    public List<LearningHistoryItem> learningHistoryItems;
    
    public int size() { 
    	return learningHistoryItems.size();
    }
    
    public LearningHistoryItem item(int pos) { 
    	return learningHistoryItems.get(pos); 
    }
    
    @Override
	public Iterator<LearningHistoryItem> iterator() {

		return learningHistoryItems.iterator(); 
	}
    
    /**
     * Factory method used to create learning history items reader based on 
     * data received from XML web service. 
     */
    public static GetHistoryFromXML getHistoryReader(String url) { 
    	
    	 try  { 
             InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
             GetHistoryFromXML learningHistoryReader = new GetHistoryFromXML(is);
             try { 
             	is.close();
             } catch(java.io.IOException e) { } 
              return learningHistoryReader; 
         } catch (Exception e) { }
         
         return null; 
    }

    /**
     * Constructor of learning history items reader from XML input stream.
     */
    public GetHistoryFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       learningHistoryItems = new ArrayList<LearningHistoryItem>();
       
       parser.require(XmlPullParser.START_TAG, ns, "history");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
           
            if (name.equals("historyRow")) {   
                readHistoryRowElement(parser);     
            } else {
                skip(parser);
            }
       }
            
      } catch (Exception e) {
           e.printStackTrace();
      } finally { }
    }
    
    /**
     * Helper method used to parse each XML historyRow element.
     */
    private void readHistoryRowElement(XmlPullParser parser) throws XmlPullParserException, IOException {
         
        LearningHistoryItem learningHistoryItem = new LearningHistoryItem(); 
        
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            
            // reading each child element with info about historyRow
            if(name.equals("profileId")) { 
               learningHistoryItem.setProfileId(readProfileId(parser)); 
            } else if(name.equals("wordsetId")) { 
               learningHistoryItem.setWordsetId(readWordsetId(parser));
            } else if(name.equals("wordsetTitle")) {
               readWordsetTitle(parser); //skip wordsetTitle information
            } else if (name.equals("modeId")) { 
               learningHistoryItem.setModeId(readModeId(parser));  
            } else if (name.equals("modeTitle")) { 
               readModeTitle(parser); // skip modeTitle information
            } else if (name.equals("wordsetTypeId")) { 
               learningHistoryItem.setWordsetTypeId(readWordsetTypeId(parser)); 
            } else if (name.equals("badAnswer")) { 
               learningHistoryItem.setBadAnswers(readBadAnswer(parser)); 
            } else if (name.equals("goodAnswer")) { 
               learningHistoryItem.setGoodAnswers(readGoodAnswer(parser)); 
            } else if (name.equals("effectivness")) { 
               readEffectivness(parser); // skip effectiveness information 
            } else if (name.equals("improvement")) { 
               learningHistoryItem.setImprovement(readImprovement(parser));
            } else if (name.equals("hits")) { 
               learningHistoryItem.setHits(readHits(parser)); 
            } else if (name.equals("lastAccess")) { 
               learningHistoryItem.setLastAccessDate(readLastAccess(parser)); 
            } else { 
                skip(parser);
            }
         }
         
         learningHistoryItems.add(learningHistoryItem);
                 
     }
   
    private int readProfileId(XmlPullParser parser) throws IOException, XmlPullParserException { 
    	parser.require(XmlPullParser.START_TAG, ns, "profileId"); 
    	String profileId = readText(parser); 
    	parser.require(XmlPullParser.END_TAG, ns, "profileId"); 
    	return Integer.valueOf(profileId); 
    }
    
    private int readWordsetId(XmlPullParser parser) throws IOException, XmlPullParserException { 
    	parser.require(XmlPullParser.START_TAG, ns, "wordsetId"); 
    	String wordsetId = readText(parser); 
    	parser.require(XmlPullParser.END_TAG, ns, "wordsetId"); 
    	return Integer.valueOf(wordsetId); 
    }
    private String readWordsetTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "wordsetTitle");
        String wordsetTitle = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "wordsetTitle");
        return wordsetTitle;
    }
    private int readModeId(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "modeId");
        String modeId = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "modeId");
        return Integer.valueOf(modeId);
    }
     private String readModeTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "modeTitle");
        String modeTitle = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "modeTitle");
        return modeTitle;
    }
    private int readWordsetTypeId(XmlPullParser parser) throws IOException, XmlPullParserException { 
    	parser.require(XmlPullParser.START_TAG, ns, "wordsetTypeId"); 
    	String wordsetTitleId = readText(parser); 
    	parser.require(XmlPullParser.END_TAG, ns, "wordsetTypeId"); 
    	return Integer.valueOf(wordsetTitleId); 
    }
    private int readBadAnswer(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "badAnswer");
        String badAnswer = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "badAnswer");
        return Integer.valueOf(badAnswer);
    }
    private int readGoodAnswer(XmlPullParser parser) throws IOException, XmlPullParserException { 
    	parser.require(XmlPullParser.START_TAG, ns, "goodAnswer");
    	String goodAnswer = readText(parser); 
    	parser.require(XmlPullParser.END_TAG, ns, "goodAnswer"); 
    	return Integer.valueOf(goodAnswer);
    }
    private float readEffectivness(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "effectivness");
        String effectivness = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "effectivness");
        return Float.valueOf(effectivness);
    }
    private float readImprovement(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "improvement");
        String improvement = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "improvement");
        return Float.valueOf(improvement);
    }
    private int readHits(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "hits");
        String hits = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "hits");
        return Integer.valueOf(hits);
    }
    private String readLastAccess(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "lastAccess");
        String lastAccess = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "lastAccess");
        return lastAccess;
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
