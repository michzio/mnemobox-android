/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import pl.elector.service.WordsLoaderService;
import pl.electoroffline.WordObject.BitmapSerializable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
/**
 *
 * @author Michał Ziobro
 */
public class WordObject implements  java.io.Serializable{
	
	public static final String KEY_WORD_ID = "KEY_WORD_ID"; 
	public static final String KEY_FROM_LANG = "KEY_FROM_LANG";
	public static final String KEY_TO_LANG = "KEY_TO_LANG"; 
	public static final String KEY_FOREIGN_WORD = "KEY_FOREIGN_WORD"; 
	public static final String KEY_NATIVE_WORD = "KEY_NATIVE_WORD"; 
	public static final String KEY_TRANSCRIPTION = "KEY_TRANSCRIPTION"; 
	public static final String KEY_RECORDING = "KEY_RECORDING"; 
	public static final String KEY_IMAGE = "KEY_IMAGE"; 
	public static final String KEY_IMAGE_BITMAP = "KEY_IMAGE_BITMAP"; 
	public static final String KEY_FOREIGN_SENTENCES = "KEY_FOREIGN_SENTENCES"; 
	public static final String KEY_NATIVE_SENTENCES = "KEY_NATIVE_SENTENCES";
	public static final String KEY_SENTENCES_RECORDINGS = "KEY_SENTENCES_RECORDINGS"; 
	
    private int tid; 
    private String fromLang; 
    private String toLang; 
    
    private String foreignWord; 
    private String nativeWord; 
    private String transcription; 
    private String recording; 
    private ArrayList<String> images;
    private transient Bitmap imageBitmap; 
    private LinkedHashMap<Integer, String> enSentences;
    private LinkedHashMap<Integer, String> plSentences; 
    private LinkedHashMap<Integer, String> sentRecordings; 
    private ArrayList<SentenceObject> sentences;
    private transient Context context; 
    
    /** Included for serialization - write this layer to the output stream. */
    private void writeObject(ObjectOutputStream out) throws IOException{
    		
    		// default serialization 
    		out.defaultWriteObject();
    		
    		// custom serialization of Bitmap object 
    		BitmapSerializable bitmapSerializable = new BitmapSerializable();
    		
    		if(imageBitmap != null) { 
	    		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
	    		imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayStream);
	    		 
	    		bitmapSerializable.imageByteArray = byteArrayStream.toByteArray();
    		}
    		out.writeObject(bitmapSerializable);
    		
    }
    
    /** Included for serialization - read this object from the supplied input stream. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException{
    	
    		// default deSerialization
    		in.defaultReadObject();
    		
    		// custom deSerialization of Bitmap object 
    		BitmapSerializable bitmapSerializable = (BitmapSerializable) in.readObject();
    		imageBitmap = BitmapFactory.decodeByteArray(bitmapSerializable.imageByteArray, 
    										0, bitmapSerializable.imageByteArray.length);
    }
    
    /**
     * Helper class that is used to serialize Bitmap as byte array
     */
    protected class BitmapSerializable implements java.io.Serializable { 
    	public byte[] imageByteArray;
    }
    
    public class SentenceObject implements java.io.Serializable { 
    	private int sentenceId; 
    	private String foreignSentence; 
    	private String nativeSentence; 
    	private String sentenceRecording; 
    	
    	public SentenceObject(int sentenceId, String foreignSentence, String nativeSentence, String sentenceRecording)
    	{
    		this.sentenceId = sentenceId; 
    		this.foreignSentence = foreignSentence; 
    		this.nativeSentence = nativeSentence; 
    		this.sentenceRecording = sentenceRecording; 
    	}
    	
    	public int getSentenceId() { 
    		return sentenceId; 
    	}
    	
    	public String getForeignSentence() {
    		return foreignSentence; 
    	}
    	
    	public String getNativeSentence() { 
    		return nativeSentence; 
    	}
    	
    	public String getRecording()  {
    		return sentenceRecording; 
    	}
    }
    
    
    private transient String url; 
    private transient GetWordDetailsFromXML wordDetailsReader; 
    
    public WordObject() {
    	//nothing
    }
    
    public WordObject(Context context) { 
    	this(); 
    	this.context = context; 
    }
   
    public WordObject(int translationId, Context context) { 
    	this(translationId, "pl", "en", context);  
    }
    
    public WordObject(int translationId, String from, String to, Context context) {
    	this(context);
        this.tid = translationId;
    	this.fromLang = from; 
    	this.toLang = to; 
    	retrieveWordDetails();
    	
    }
    
    public WordObject(int wordId, String foreignName, String nativeName, String transcription, String imagePath, Bitmap imageBitmap) 
    {
    	tid = wordId;
    	foreignWord = foreignName; 
    	nativeWord = nativeName; 
    	this.transcription = transcription; 
    	images = new ArrayList<String>();
    	if(imagePath!=null) images.add(imagePath);
    	this.imageBitmap = imageBitmap; 
    	
    	
    }
    
    public void setWordId(int wordId) {
    	this.tid = wordId; 
    }
    
    public void setForeignWord(String foreignName) {
    	this.foreignWord = foreignName;
    }
    
    public void setNativeWord(String nativeName) { 
    	this.nativeWord = nativeName; 
    }
    
    public void setTranscription(String transcription) {
    	this.transcription = transcription; 
    }
    
    public void setImageBitmap(Bitmap imageBitmap) { 
    	this.imageBitmap = imageBitmap;
    }
    
    public void addImagePath(String imagePath) { 
    	if(imagePath == null) return; 
    	
    	if(this.images == null) 
    		this.images = new ArrayList<String>();
    	
    	this.images.add(imagePath);
    }
    
    public void setRecording(String recording) 
    {
    	this.recording = recording;
    }
    
    public void addForeignSentence(Integer sentenceId, String foreignSentence) 
    {
    	if(sentenceId == null || foreignSentence == null) return; 
    	
    	if(this.enSentences == null)  
    		this.enSentences = new LinkedHashMap<Integer, String>();
    	
    	this.enSentences.put(sentenceId, foreignSentence);
    }
    
    public void addNativeSentence(Integer sentenceId, String nativeSentence)
    {
    	if(sentenceId == null || nativeSentence == null) return; 
    	
    	if(this.plSentences == null)
    		this.plSentences = new LinkedHashMap<Integer, String>();
    	
    	this.plSentences.put(sentenceId, nativeSentence);
    }
    
    public void addSentenceRecording(Integer sentenceId, String sentenceRecording)
    {
    	if(sentenceId == null || sentenceRecording == null) return; 
    	
    	if(this.sentRecordings == null)
    		this.sentRecordings = new LinkedHashMap<Integer, String>();
    	
    	this.sentRecordings.put(sentenceId, sentenceRecording);
    }
    
    public void addSentence(SentenceObject sentenceObject) 
    {
    	if(sentenceObject == null) return; 
    	
    	if(this.sentences == null)
    		this.sentences = new ArrayList<SentenceObject>(); 
    	
    	this.sentences.add(sentenceObject); 
    	
    	addForeignSentence(sentenceObject.getSentenceId(), sentenceObject.getForeignSentence());
    	addNativeSentence(sentenceObject.getSentenceId(), sentenceObject.getNativeSentence());
    	addSentenceRecording(sentenceObject.getSentenceId(), sentenceObject.getRecording());
    }
    
    public void addSentence(int sentenceId, String foreignSentence, String nativeSentence, String sentenceRecording)
    {
    	addSentence(new SentenceObject(sentenceId, foreignSentence, nativeSentence, sentenceRecording));
    }
    
    private void retrieveWordDetails() { 
        
        this.url = context.getResources().getString(R.string.gettranslation_url, fromLang, toLang, tid);
       
        try  { 
                InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(this.url);
                wordDetailsReader = new GetWordDetailsFromXML(is);  
                
                try { 
                	is.close();
                } catch(java.io.IOException e) { } 
        } catch (Exception e) { 
        	
        	Log.w(WordObject.class.getName(), "Exception while loading word details from: " + this.url); 
        }
    }
    
    public int getWordId() {
    	return tid; 
    }
    
    public String getPlWord() { 
        if(this.wordDetailsReader != null && this.nativeWord == null) { 
            this.nativeWord = wordDetailsReader.plWord;
        }
        return this.nativeWord; 
    }
    
    public String getNativeWord() 
    {
    	return getPlWord();
    }
    
    public String getEnWord() { 
        if(this.wordDetailsReader != null && this.foreignWord == null) { 
            this.foreignWord = wordDetailsReader.enWord;
        }
        return this.foreignWord; 
    }
    
    public String getForeignWord() {
    	return getEnWord();
    }
    
    public String getTranscription() { 
        if(this.wordDetailsReader != null && this.transcription == null) { 
            this.transcription = wordDetailsReader.transcription;
        }
        return this.transcription; 
    }
    
     public String getRecording() { 
        if(this.wordDetailsReader != null && this.recording == null) { 
            this.recording = wordDetailsReader.recording;
        }
        return this.recording; 
    }
    
     public ArrayList<String> getImages() { 
        if(images == null) { 
        	if(wordDetailsReader != null && wordDetailsReader.images != null)
        		images = new ArrayList<String>(wordDetailsReader.images.values());
        }
        return this.images; 
    }
     
    public Bitmap getImageBitmap() {
    	if(this.imageBitmap == null) { 
    		ArrayList<String> imgs = getImages();
    		if(imgs != null) { 
	    		byte[] imageBlob = new byte[] {};
	    		if(imgs.size() > 0) { 
	    			Log.w(WordObject.class.getName(), "Getting Image Bitmap for: " + imgs.get(0));
	    			imageBlob = WordsLoaderService.getImageBlob(context,  imgs.get(0));
	    		} 
	    		if(imageBlob != null)
	    			this.imageBitmap = BitmapFactory.decodeByteArray(imageBlob, 0, imageBlob.length);
    		}
    	}
    	return this.imageBitmap;
    }
    
    public LinkedHashMap<Integer, String> getNativeSentences()
    {
    	return getPlSentences();
    }
    
    public LinkedHashMap<Integer, String> getPlSentences() { 
        if(this.wordDetailsReader != null && this.plSentences == null) { 
            this.plSentences = wordDetailsReader.plSentences;
        }
        return this.plSentences; 
    }
    
    public LinkedHashMap<Integer, String> getForeignSentences() { 
    	return getEnSentences();
    }
    
    public LinkedHashMap<Integer, String> getEnSentences() { 
        if(this.wordDetailsReader != null && this.enSentences == null) { 
            this.enSentences = wordDetailsReader.enSentences;
        }
        return this.enSentences; 
    }
    

    public LinkedHashMap<Integer, String> getSentencesRecordings() { 
        if(this.wordDetailsReader != null && this.sentRecordings == null) { 
            this.sentRecordings = wordDetailsReader.sentRecordings;
        }
        return this.sentRecordings; 
    }
    
    public ArrayList<SentenceObject> getSentences() { 
    	
    	// if sentences object not loaded lazy instantiate it
    	if(this.sentences == null) { 
    		// load foreign, native sentences and its recording previously
    		getForeignSentences();
    		getNativeSentences();
    		getSentencesRecordings();
    		
    		sentences = new ArrayList<SentenceObject>();
    		
    		if(enSentences == null) return this.sentences;
    		
    		for(Integer sentenceId : enSentences.keySet()) { 
    			
    			sentences.add(new SentenceObject(sentenceId, 
    											 enSentences.get(sentenceId),
    											 plSentences.get(sentenceId),
    											 sentRecordings.get(sentenceId) 
    											));	
    		}
    	}
    	
    	return this.sentences;
    }
}
