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
public class GetWordDetailsFromXML {
    private static final String ns = null;
    public String plWord;
    public String plArticle; 
    public int plId; 
    public String enWord; 
    public String enArticle; 
    public int enId; 
    public String transcription; 
    public String recording; 
    public LinkedHashMap<Integer, String> images; 
    public LinkedHashMap<Integer, String> enSentences; 
    public LinkedHashMap<Integer, String> plSentences; 
    public LinkedHashMap<Integer, String> sentRecordings; 
    
    public GetWordDetailsFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
      images = new LinkedHashMap<Integer, String>(); 
      enSentences = new LinkedHashMap<Integer, String>();
      plSentences = new LinkedHashMap<Integer, String>();
      sentRecordings = new LinkedHashMap<Integer, String>();
       
       parser.require(XmlPullParser.START_TAG, ns, "word");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
            if (name.equals("polish")) {
                this.plId =  Integer.parseInt(parser.getAttributeValue(null, "pid"));
               // this.plArticle = parser.getAttributeValue(null, "article"); 
                this.plArticle = ""; 
                this.plWord =  readPLWord(parser);    
            } else if(name.equals("english")) {
                this.enId =  Integer.parseInt(parser.getAttributeValue(null, "eid"));
               // this.enArticle = parser.getAttributeValue(null, "article");
                this.enArticle = ""; 
                this.enWord =  readENWord(parser);    
            } else if(name.equals("transcription")) { 
                this.transcription =  readTranscription(parser);
            } else if(name.equals("recording")) { 
                this.recording = readRecording(parser);
            } else if(name.equals("images")) { 
                 readImages(parser); 
            } else if(name.equals("sentences")) { 
                readSentences(parser); 
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
    
    private void readImages(XmlPullParser parser) throws XmlPullParserException, IOException {
         
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("image")) {
               int imgid =  Integer.parseInt(parser.getAttributeValue(null, "imgid"));
               String imageFileName  = readImage(parser);
               this.images.put(imgid, imageFileName);
            } else { 
                skip(parser);
            }
         }
                 
     }
    
      private void readSentences(XmlPullParser parser) throws XmlPullParserException, IOException {
         
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("sentence")) {
               int sid =  Integer.parseInt(parser.getAttributeValue(null, "sid"));
               String[] sent  = readSentence(parser); 
               this.enSentences.put(sid, sent[0]);
               this.plSentences.put(sid, sent[1]); 
               this.sentRecordings.put(sid, sent[2]);
            } else { 
                skip(parser);
            }
         }
                 
     }
      
         private String[] readSentence(XmlPullParser parser) throws XmlPullParserException, IOException {
         String sentRecording = ""; 
         String sentEN = ""; 
         String sentPL = "";
          
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("senteng")) {
               sentRecording =  parser.getAttributeValue(null, "recording");
               sentEN  = readSentEN(parser);  
            } else if(name.equals("sentpl") ) {
               sentPL  = readSentPL(parser);
            } else { 
                skip(parser);
            }
         }
         String[] result = new String[] { 
             sentEN, sentPL, sentRecording
         };     
         return result; 
     }
    private String readSentEN(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "senteng");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "senteng");
        return title;
    }
     private String readSentPL(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "sentpl");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "sentpl");
        return title;
    }
    
     private String readENWord(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "english");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "english");
        return title;
    }
     private String readPLWord(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "polish");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "polish");
        return title;
    }
     private String readImage(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "image");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "image");
        return title;
    }
      private String readRecording(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "recording");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "recording");
        return title;
    }
      private String readTranscription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "transcription");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "transcription");
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
