/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.electoroffline;

import java.util.ArrayList;
import java.util.Collections;

import pl.elector.database.LearningHistoryProvider.Mode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

/**
 * @author Michał Ziobro
 */
public class CartonsActivity extends LearningMethodActivity implements WordsetWordsAccessor.Callbacks {
   
	// private String url;
    // private GetWordsListFromXML wordsListObject;
    
    // learning method variables
    private ArrayList<CardObject> cardsObjects;
    private ArrayList<Integer> cardsElementsIds; 
    private ArrayList<Integer> cardsTxtElementsIds; 
   
    private boolean uncoverCard = false; 
    private ImageView lastUncoveredCard; 
    
    private static final int NUMBER_OF_WORDS = 6;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }
    
	@Override
	protected boolean isUsingViewPager() {
		
		return false;
	}

	@Override
	protected Class<?> getLearningFragmentClass() {
		
		return null;
	}
	
	@Override
	protected Mode getLearningMode() { 
		return Mode.ANDROID_MEMORY_GAME; 
	}
    
    @Override
    protected void onCreateDrawerActivity(Bundle savedInstanceState) {
        setContentView(R.layout.cartons_drawer);
        super.onCreateDrawerActivity(savedInstanceState);
    }
    
    @Override
    protected void createLayout() { 
    	super.createLayout();
    	// setting up action bar title
        ActionBar actionBar =  getSupportActionBar();
        actionBar.setTitle(getString(R.string.cartons));
    }
     
     @Override
     protected void startLearning() { 
    	 // start learning 
    	 generateCardsOfWords();
     }
     
     private void generateCardsOfWords() { 
         randomizeWords();
         loadCardsElements(); 
         inputLabels();
     }
    
     private void inputLabels() { 
        
         for(int i=0; i<(2*NUMBER_OF_WORDS); i++) { 
            Button btn = (Button) findViewById(cardsTxtElementsIds.get(i)); 
            String label; 
            if(cardsObjects.get(i).lang == "pl") { 
                 label = enWords.get(cardsObjects.get(i).wid); 
            } else {
                 label = plWords.get(cardsObjects.get(i).wid); 
            }
            btn.setText(label);  
            ImageView iv = (ImageView) findViewById(cardsElementsIds.get(i));
            iv.setVisibility(View.GONE); 
            if(areImageDataAvailable) { 
            	byte[] image = imageData.get(cardsObjects.get(i).wid); 
            	Bitmap bitmap = null; 
    			if(image != null) {
    				bitmap =  BitmapFactory.decodeByteArray(image, 0, image.length);
    			}
            	iv.setImageBitmap(BitmapUtilities.fitImageView(bitmap, iv));
            
            } else { 
            	UrlImageViewHelper.setUrlDrawable(iv, 
            			getResources().getString(R.string.images_url) + imagePaths.get(cardsObjects.get(i).wid));
            }
         }
     }
     
     private void randomizeWords() { 
         Collections.shuffle(widCollection);
         cardsObjects = new ArrayList<CardObject>(); 
         for(int i=0; i<6; i++) { 
            cardsObjects.add( new CardObject(widCollection.get(i), "pl"));
            cardsObjects.add( new CardObject(widCollection.get(i), "en"));
         }
         Collections.shuffle(cardsObjects); 
          
     }
     
     private void loadCardsElements() { 
         cardsElementsIds = new ArrayList<Integer>(); 
         cardsElementsIds.add(R.id.card11);
         cardsElementsIds.add(R.id.card12);
         cardsElementsIds.add(R.id.card13);
         cardsElementsIds.add(R.id.card21);
         cardsElementsIds.add(R.id.card22);
         cardsElementsIds.add(R.id.card23);
         cardsElementsIds.add(R.id.card31);
         cardsElementsIds.add(R.id.card32);
         cardsElementsIds.add(R.id.card33);
         cardsElementsIds.add(R.id.card41);
         cardsElementsIds.add(R.id.card42);
         cardsElementsIds.add(R.id.card43);
         cardsTxtElementsIds = new ArrayList<Integer>(); 
         cardsTxtElementsIds.add(R.id.cardtxt11);
         cardsTxtElementsIds.add(R.id.cardtxt12);
         cardsTxtElementsIds.add(R.id.cardtxt13);
         cardsTxtElementsIds.add(R.id.cardtxt21);
         cardsTxtElementsIds.add(R.id.cardtxt22);
         cardsTxtElementsIds.add(R.id.cardtxt23);
         cardsTxtElementsIds.add(R.id.cardtxt31);
         cardsTxtElementsIds.add(R.id.cardtxt32);
         cardsTxtElementsIds.add(R.id.cardtxt33);
         cardsTxtElementsIds.add(R.id.cardtxt41);
         cardsTxtElementsIds.add(R.id.cardtxt42);
         cardsTxtElementsIds.add(R.id.cardtxt43); 
     }
     
     public void cardtxtClickEventHandler(View v) {
         Button btn = (Button) v; 
         btn.setVisibility(View.GONE); 
         int idx = cardsTxtElementsIds.indexOf(v.getId()); 
         ImageView iv = (ImageView) findViewById(cardsElementsIds.get(idx));
         iv.setVisibility(View.VISIBLE);
         
         if(uncoverCard) { 
             verifyUncoveredCards(iv); 
         } else { 
             uncoverCard = true; 
             lastUncoveredCard = iv; 
         }
     }
     
     private void verifyUncoveredCards(ImageView newUncoveredCard) { 
         int newIdx = cardsElementsIds.indexOf(newUncoveredCard.getId());
         int lastIdx = cardsElementsIds.indexOf(lastUncoveredCard.getId());
         if(cardsObjects.get(newIdx).wid == cardsObjects.get(lastIdx).wid) { 
             Toast.makeText(this, getString(R.string.good_answer_toast),
                               Toast.LENGTH_SHORT).show();
             uncoverCard = false;
             currWid = cardsObjects.get(newIdx).wid;
             playRecording(); 
             goodAns++;
             personalization.traceForgottenWord(currWid, Personalization.Mood.NEUTRAL);
             
             if((goodAns + badAns) == NUMBER_OF_WORDS) { 
            	 Toast.makeText(this, getString(R.string.game_finished_toast), Toast.LENGTH_SHORT).show();
            	 personalization.synchronize();
             }
             
         } else {  
             newUncoveredCard.setVisibility(View.GONE); 
             lastUncoveredCard.setVisibility(View.GONE); 
             Button btn1 = (Button) findViewById(cardsTxtElementsIds.get(newIdx));
             btn1.setVisibility(View.VISIBLE); 
             Button btn2 = (Button) findViewById(cardsTxtElementsIds.get(lastIdx));
             btn2.setVisibility(View.VISIBLE); 
             uncoverCard = false;
             Toast.makeText(this, getString(R.string.wrong_answer_toast),
                               Toast.LENGTH_SHORT).show();
             badAns++; 
             currWid = cardsObjects.get(newIdx).wid;
             personalization.traceForgottenWord(currWid, Personalization.Mood.BAD);
             addToForgottenDrawerList(currWid); 
         }
     }
     
     public void cardClickEventHandler(View v) { 
         //implement for example switching on details of the uncover word
    	 if(currWid != 0) { 
         	Intent detailsIntent = new Intent(this, WordDetailsActivity.class);
       		detailsIntent.putExtra(WordDetailsActivity.KEY_WORD_ID, currWid);
             startActivity(detailsIntent);
         } else { 
         	Toast.makeText(this, getResources().getString(R.string.no_word_to_show_details), Toast.LENGTH_SHORT).show();  
         }
     }
   
     /**
      * Handler used to populate options menu 
      */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return super.onCreateOptionsMenu(menu);
    }
 
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	return super.onOptionsItemSelected(item);
    }
    
    private class CardObject { 
        public int wid; 
        public String lang;
        
        CardObject(int wid, String lang) { 
            this.wid = wid; 
            this.lang = lang; 
        }
    }

	/*@Override
	protected void hideChallangeElements() {
		return;
	}

	@Override
	protected void displayCurrentWord() {
		return;
	}

	@Override
	protected void verifyAnswer() {
		return;
	}

	@Override
	protected void uncoverChallangeElements() {
		return;
	}*/
    
    /**
     * url = this.getString(R.string.getwordset_url).replaceAll("&amp;", "&"); 
     * url += CartonsActivity.wordsetId;
     * try  { 
     *   InputStream is = CustomHttpClient.retrieveInputStreamFromHttpGet(url);
     *   wordsListObject = new GetWordsListFromXML(is); 
     *   try { 
     *   is.close();
     *   } catch(java.io.IOException e) { } 
     * } catch (Exception e) { }
     *  if(wordsListObject instanceof GetWordsListFromXML) { 
     *    generateCardsOfWords(); 
     *  }
     */
    
    /**
     * DEPRACEATED
     * private void loadWordset() { 
     *  enWords = wordsListObject.getENWords();
     *  plWords = wordsListObject.getPLWords();
     *   images = wordsListObject.getImages(); 
     *  transcriptions = wordsListObject.getTranscriptions();
     *  audios = wordsListObject.getAudios();
     *  widCollection = new ArrayList<Integer>(enWords.keySet());
     * }
     */
    
    /**
     * DEPRECATED:
     * private void impressForgotten() {  
     *      serializedForgotten += currWid + ",1;";    
     * } 
     */
}

