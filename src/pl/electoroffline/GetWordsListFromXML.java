/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

/**
 *
 * @author Michał Ziobro
 */
public class GetWordsListFromXML {

    private static final String ns = null;
    private static Context context = null; 
    
    public ArrayList<Integer> wordIds; 
    public LinkedHashMap<Integer,String> enWords;
    public LinkedHashMap<Integer,String> plWords;
    public LinkedHashMap<Integer,String> transcriptions;
    public LinkedHashMap<Integer,String> images;
    public LinkedHashMap<Integer,String> audios;
    public LinkedHashMap<Integer, LinkedHashMap<Integer, ArrayList<String>>> sentences;
    public LinkedHashMap<Integer, String> plArticles; 
    public LinkedHashMap<Integer, String> enArticles; 
    
    // wordId of currently read word element from XML
    private int wordId; 
    // sentenceId of currently read sentence element from XML
    private int sentenceId; 
    
    public static GetWordsListFromXML getWordsListReader(Context ctx, String url) {  
    	
    	 context = ctx; 
    	 
        try  { 
            InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
            GetWordsListFromXML wordsListReader = new GetWordsListFromXML(is);
            try { 
            	is.close();
            } catch(java.io.IOException e) { } 
             return wordsListReader; 
        } catch (Exception e) { }
        
        return null; 
        
    }
    
    public GetWordsListFromXML(InputStream xmlStream) { 
       
      try { 
       
       XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
       XmlPullParser parser = factory.newPullParser();
       parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
       
       InputStream is = xmlStream;
       parser.setInput(is, null);
       parser.nextTag();
       
       wordIds = new ArrayList<Integer>();
       enWords = new LinkedHashMap<Integer,String>();
       plWords = new LinkedHashMap<Integer,String>();
       transcriptions = new LinkedHashMap<Integer,String>();
       images = new LinkedHashMap<Integer,String>();
       audios = new LinkedHashMap<Integer,String>();
       plArticles = new LinkedHashMap<Integer,String>();
       enArticles = new LinkedHashMap<Integer, String>(); 
       sentences = new LinkedHashMap<Integer, LinkedHashMap<Integer, ArrayList<String>>>(); 
       
       parser.require(XmlPullParser.START_TAG, ns, "set");
       while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
            if (name.equals("word")) {
                wordId =  Integer.parseInt(parser.getAttributeValue(null, "wid"));
                wordIds.add(wordId); 
                /* deprecated code:
                 * String[] output = 
                 */
                readWordElement(parser);
               
                /* deprecated code: 
                	plWords.put(wordId, output[0]);
                	enWords.put(wordId, output[1]);
                	images.put(wordId, output[2]);
                	audios.put(wordId, output[3]);
                	transcriptions.put(wordId, output[4]);
                	plArticles.put(wordId, output[5]);
                	enArticles.put(wordId, output[6]);
                */
                
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
    
    public int getSize() { 
    	return this.wordIds.size(); 
    }
    
    public ArrayList<Integer> getWordIds() { 
    	return this.wordIds;
    }
    
    public WordObject getWordObject( int pos) { 
    	
    	int wid = this.wordIds.get(pos);
    	
    	WordObject wordObject = new WordObject(context); 
    	wordObject.setWordId(wid);
    	wordObject.setForeignWord(this.enWords.get(wid));
    	wordObject.setNativeWord(this.plWords.get(wid));
    	wordObject.setRecording(this.audios.get(wid));
    	wordObject.setTranscription(this.transcriptions.get(wid));
    	wordObject.addImagePath(this.images.get(wid));
    	
    	return wordObject; 
    }
    
    public LinkedHashMap<Integer, String> getENWords() {  
        return this.enWords;
    }
     
    public LinkedHashMap<Integer, String> getPLWords() { 
        return this.plWords;
    }
    public LinkedHashMap<Integer, String> getImages() { 
        return this.images;
    }
    public LinkedHashMap<Integer, String> getAudios() { 
        return this.audios;
    }
    public LinkedHashMap<Integer, String> getTranscriptions() { 
        return this.transcriptions;
    }
    
    public LinkedHashMap<Integer, String> getENArticles() {
    	return this.enArticles; 
    }
    
    public LinkedHashMap<Integer, String> getPLArticles() { 
    	return this.plArticles; 
    }
    
    public LinkedHashMap<Integer, LinkedHashMap<Integer, ArrayList<String>>> getSentences() {
    	return this.sentences; 
    }
    
    private void readWordElement(XmlPullParser parser) throws XmlPullParserException, IOException {
       /* deprecated code: 
    	String plWord = "";
        String plArticle = "";
        String enWord = "";
        String enArticle = "";
        String image = "";
        String audio = "";
        String transcription = "";
        */
    	parser.require(XmlPullParser.START_TAG, ns, "word");
         while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            
           if (name.equals("angword")) {
        	    // reading <angword> element inner text and its attribute article
        	    String enArticle =  parser.getAttributeValue(null, "article");
                String enWord = readENWord(parser);
                
               // adding values to HashMaps:
                enWords.put(wordId, enWord);
                enArticles.put(wordId, enArticle); 
                
            } else if (name.equals("plword")) { 
            	// reading <plword> element inner text and its attribute article 
            	String plArticle =  parser.getAttributeValue(null, "article");
            	String plWord = readPLWord(parser);  
            	
            	// adding values to HashMaps:
            	plWords.put(wordId, plWord);
            	plArticles.put(wordId, plArticle); 
            	
            } else if (name.equals("image")) {
            	// reading <image> element inner text
                String image = readImage(parser); 
               
               // adding value to HashMap:
               images.put(wordId, image); 
               
            } else if (name.equals("audio")) { 
            	// reading <audio> element inner text
                String audio = readAudio(parser); 
                
                // adding value to HashMap:
                audios.put(wordId, audio);
                
           } else if (name.equals("transcription")) { 
        	   // reading <transcription> element inner text
               String transcription = readTranscription(parser); 
               
               // adding value to HashMap:
               transcriptions.put(wordId, transcription); 
               
            } else if(name.equals("sentences")) {
            	// reading <sentences> element 
            	// and parsing inner <sentence> elements
            	sentences.put(wordId, new LinkedHashMap<Integer, ArrayList<String>>());
            	readSentencesElement(parser); 

            } else { 
                skip(parser);
            }
         }
         
         /* deprecared code: 
         	String[] result;
         	result = new String[7];
         	result[0] = plWord;
         	result[1] = enWord;
         	result[2] = image;
         	result[3] = audio; 
         	result[4] = transcription; 
         	result[5] = plArticle;
         	result[6] = enArticle;
         	return result;
         */
                   
     }
    
     private String readENWord(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "angword");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "angword");
        return title;
    }
     private String readPLWord(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "plword");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "plword");
        return title;
    }
     private String readImage(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "image");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "image");
        return title;
    }
      private String readAudio(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "audio");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "audio");
        return title;
    }
      private String readTranscription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "transcription");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "transcription");
        return title;
    }
      // extracts inner text value from current TAG
      private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
    
    private void readSentencesElement(XmlPullParser parser) throws IOException, XmlPullParserException {
    	
    	parser.require(XmlPullParser.START_TAG, ns, "sentences");
        while (parser.next() != XmlPullParser.END_TAG) {
             
        	if (parser.getEventType() != XmlPullParser.START_TAG) {
                  continue;
              }
              
              String name = parser.getName();
              
              if (name.equals("sentence")) {
            	  
            	  // reading <sentence> element assign to current word with wordId
            	  // getting sentence id (sid) attribute
                  sentenceId =  Integer.valueOf(parser.getAttributeValue(null, "sid"));
                  
                  // placing in given word's sentences HashMap (for given wordId)
                  // ArrayList for current sentenceId where will be placed sentence's details
                  sentences.get(wordId).put(sentenceId, new ArrayList<String>());
                 
                  // calling method that reads nested elements with sentence details
                  readSentenceElement(parser);
                  
              } else {
                  skip(parser);
              }
         }
    }
    
    private void readSentenceElement(XmlPullParser parser) throws IOException, XmlPullParserException {
    	
    	parser.require(XmlPullParser.START_TAG, ns, "sentence"); 
    	while (parser.next() != XmlPullParser.END_TAG) {
            
        	if (parser.getEventType() != XmlPullParser.START_TAG) {
                  continue;
             }
              
             String name = parser.getName();
              
             if (name.equals("sentenceang")) {
            	// reading <sentenceang> element inner text
            	 String sentenceEN = readSentenceEN(parser); 
                  
                 // adding value to sentence's ArrayList for current sentenceId and wordId in sentences HashMap:
                 sentences.get(wordId).get(sentenceId).add(0, sentenceEN);
            	 
             } else if(name.equals("sentencepl")) {
            	// reading <sentencepl> element inner text
            	 String sentencePL = readSentencePL(parser); 
            	
            	// adding value to sentence's ArrayList for current senteceId and wordId in sentences HashMap:
            	 sentences.get(wordId).get(sentenceId).add(1, sentencePL);
            	 
             } else if(name.equals("sentenceaudio")) {
            	// reading <sentenceaudio> element inner text
            	 String sentenceAudio = readSentenceAudio(parser); 
            	 
            	 // adding value to sentence's ArrayList for current sentenceId and wordId in sentences HashMap:
            	 sentences.get(wordId).get(sentenceId).add(2, sentenceAudio); 
    
             } else {
                  skip(parser);
             }
         }
    }
    
   private String readSentenceEN(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "sentenceang");
        String sentenceEN = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "sentenceang");
        return sentenceEN;
   }
   
   private String readSentencePL(XmlPullParser parser) throws IOException, XmlPullParserException {
       parser.require(XmlPullParser.START_TAG, ns, "sentencepl");
       String sentencePL = readText(parser);
       parser.require(XmlPullParser.END_TAG, ns, "sentencepl");
       return sentencePL;
   }
   
   private String readSentenceAudio(XmlPullParser parser) throws IOException, XmlPullParserException {
       parser.require(XmlPullParser.START_TAG, ns, "sentenceaudio");
       String sentenceAudio = readText(parser);
       parser.require(XmlPullParser.END_TAG, ns, "sentenceaudio");
       return sentenceAudio;
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
