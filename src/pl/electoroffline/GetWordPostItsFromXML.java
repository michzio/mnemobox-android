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
public class GetWordPostItsFromXML {
    private static final String ns = null;
    
    private Integer currentPostItId; 
    
    private LinkedHashMap<Integer, String> from; 
    private LinkedHashMap<Integer, String> to;
    private LinkedHashMap<Integer, Integer> authorId; 
    private LinkedHashMap<Integer, String> postText; 
    private LinkedHashMap<Integer, String> authorFirstName; 
    private LinkedHashMap<Integer, String> authorLastName; 
    
    public LinkedHashMap<Integer, String> from() {
    	return from; 
    }
    
    public LinkedHashMap<Integer, String> to() { 
    	return to; 
    }
    
    public LinkedHashMap<Integer, Integer> authorId() { 
    	return authorId; 
    }
    
    public LinkedHashMap<Integer, String> postText() { 
    	return postText; 
    }
    
    public LinkedHashMap<Integer, String> authorFirstName() { 
    	return authorFirstName; 
    }
    
    public LinkedHashMap<Integer, String> authorLastName() { 
    	return authorLastName;
    }
    
    public GetWordPostItsFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       from = new LinkedHashMap<Integer, String>(); 
       to = new LinkedHashMap<Integer, String>();
       postText = new LinkedHashMap<Integer, String>();
       authorFirstName = new LinkedHashMap<Integer, String>();
       authorLastName = new LinkedHashMap<Integer, String>();
       authorId = new LinkedHashMap<Integer, Integer>();
       
       parser.require(XmlPullParser.START_TAG, ns, "postits");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
           
            String name = parser.getName();
            
            // start by looking for postit tag 
            if (name.equals("postit")) {
                currentPostItId =  Integer.parseInt(parser.getAttributeValue(null, "pid"));
                from.put(currentPostItId, parser.getAttributeValue(null, "from"));
                to.put(currentPostItId, parser.getAttributeValue(null, "to"));
             
                readPostIt(parser);    
            } else { 
                skip(parser);
            }
       }
            
      } catch (Exception e)  {
           //insert something when error
      } finally { }
    
    }
    
    private void readPostIt(XmlPullParser parser)  throws XmlPullParserException, IOException {
		
    	parser.require(XmlPullParser.START_TAG, ns, "postit");
    	while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            
            String name = parser.getName();
            
            if (name.equals("postText")) {
                readPostText(parser);   
            } else if (name.equals("authorFirstName")) { 
            	readAuthorFirstName(parser);  
            } else if (name.equals("authorLastName")) { 
                readAuthorLastName(parser); 
            } else if (name.equals("authorId")) { 
                readAuthorId(parser); 
            }else { 
                skip(parser);
            }
    	}
    	
	}

    private void readPostText(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "postText");
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "postText");
        
        postText.put(currentPostItId, text);
    }
    
    private void readAuthorFirstName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "authorFirstName");
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "authorFirstName");
        
        authorFirstName.put(currentPostItId, text);
    }
    
    private void readAuthorLastName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "authorLastName");
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "authorLastName");
        
        authorLastName.put(currentPostItId, text);
    }
    
    private void readAuthorId(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "authorId");
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "authorId");
       
        authorId.put(currentPostItId, Integer.valueOf(text));
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
