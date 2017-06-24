/**
 * 
 */
package pl.electoroffline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import pl.electoroffline.WordsetWordsAccessor.ACCESS_TYPE;

import android.app.Activity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.TextView;

/**
 * @author Michał Ziobro
 *
 */
public class WordsetWordsAdapter extends BaseExpandableListAdapter implements WordsetWordsAccessor.Callbacks, OnCheckedChangeListener {
	
	private int wordsetId; 
	private LayoutInflater inflater; 
	private Activity activity; 
	
	// data associated with adapter 
	private LinkedHashMap<Integer, String> foreignWords; 
	private LinkedHashMap<Integer, String> nativeWords;
	private ArrayList<Integer> wordIds;
	
	// word Ids selected to learn,
	// must be used in combination with 
	// flag wordsHasBeenLoaded
	private LinkedHashSet<Integer> selectedWordIds;
	private boolean wordsHasBeenLoaded;
	private boolean wordsHasBeenSelected;
	
	private WordsetWordsAccessor wordsAccessor;
	
	
	/**
	 * WordsetWordsAdapter constructor used to display chackable (expandable) 
	 * list of word items that belongs to wordset with given wordsetId 
	 * @param a
	 * @param wordsetId
	 */
	public WordsetWordsAdapter(Activity a, int wordsetId) {
		activity = a; 
		inflater = a.getLayoutInflater(); 
		this.wordsetId = wordsetId; 
		
		
		foreignWords = new LinkedHashMap<Integer, String>(); 
		nativeWords = new LinkedHashMap<Integer, String>(); 
		wordIds = new ArrayList<Integer>(); 
		wordsHasBeenLoaded = false; 
		wordsHasBeenSelected = false; 
		selectedWordIds = new LinkedHashSet<Integer>(); 
		
		wordsAccessor = new WordsetWordsAccessor(activity, wordsetId, false, ACCESS_TYPE.DEFAULT_NO_PROMPT);
		wordsAccessor.setCallbacksListener(this);
		wordsAccessor.load(); 
	}
	
	/**
     * WordsetWordsAccessor.Callbacks interface method called when WordsetWordsAccessor
     * object finishes WORDSET WORDS details loading process. Now you can notify adapter about word items has been loaded.
     */
	@Override
    public void onWordsLoadFinished(WordsetWordsAccessor wordsAccessor) {
      	
      		Log.w(WordsetWordsAccessor.Callbacks.class.getName(), 
      					"Words loading finished! (" + wordsAccessor.getWordIds().size() + ")");
      		
      		for(String word : wordsAccessor.getForeignWords().values())
      			Log.w(WordsetWordsAccessor.Callbacks.class.getName(), "Loaded word: " + word); 
      		
      		if(wordsAccessor.wordsetId() != this.wordsetId) { 
      			notifyDataSetInvalidated(); return ; 
      		}
      		
      		// setting local attributes with collections of data loaded by words accessory
    	    foreignWords = wordsAccessor.getForeignWords(); 
    	    nativeWords = wordsAccessor.getNativeWords();
    	    wordIds = wordsAccessor.getWordIds();
    	    
    	    wordsHasBeenLoaded = true; 
    	    selectedWordIds = new LinkedHashSet<Integer>(wordIds);
    	        
    	    // reload expandable list view
    	    notifyDataSetChanged();
    }
	
	public void clearDataSet() {
		foreignWords.clear(); 
		nativeWords.clear(); 
		wordIds.clear(); 
		wordsHasBeenLoaded = false; 
		selectedWordIds.clear(); 
		
		wordsAccessor.setCallbacksListener(null); 
		
		// reload expandable list view 
		notifyDataSetChanged(); 
	}
	
	
	public void selectAllWords() {
		selectedWordIds.addAll(wordIds);
		
		// reload expandable list view 
		notifyDataSetChanged(); 
	}
	
	public void unselectAllWords() { 
		selectedWordIds.clear(); 
		
		// reload expandable list view 
		notifyDataSetChanged(); 
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChild(int, int)
	 */
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		
		return wordIds.get(groupPosition);
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChildId(int, int)
	 */
	@Override
	public long getChildId(int groupPosition, int childPosition) {
		
		return wordIds.get(groupPosition);
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChildView(int, int, boolean, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		
		return convertView;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getChildrenCount(int)
	 */
	@Override
	public int getChildrenCount(int groupPosition) {
	
		// no child views, only group views? 
		return 0;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroup(int)
	 */
	@Override
	public Object getGroup(int groupPosition) { 
		
		return wordIds.get(groupPosition);
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroupCount()
	 */
	@Override
	public int getGroupCount() {
		return wordIds.size();
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroupId(int)
	 */
	@Override
	public long getGroupId(int groupPosition) {
		return wordIds.get(groupPosition);
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#getGroupView(int, boolean, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		
		final Integer groupWordId = (Integer) getGroup(groupPosition);
		
		CheckBox wordCheckBox = null; 
		TextView wordCheckBoxLabel = null; 
	
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.wordset_words_item, null);
		}
		
		wordCheckBox= (CheckBox) convertView.findViewById(R.id.word_check_box);
		wordCheckBoxLabel = (TextView) convertView.findViewById(R.id.word_check_box_label); 
		wordCheckBoxLabel.setText(foreignWords.get(groupWordId) + " - " + nativeWords.get(groupWordId));
		wordCheckBox.setTag(groupWordId); 
		if(selectedWordIds.contains(groupWordId)) { 
			wordCheckBox.setChecked(true); 
		} else { 
			wordCheckBox.setChecked(false);
		}
		wordCheckBox.setOnCheckedChangeListener(this); 
		
		convertView.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				//Toast.makeText(activity, "Wordset words drawer list view item clicked with id: " 
				//						+ groupWordId + ".", Toast.LENGTH_LONG).show();
				
			}
			
		});
		
		return convertView;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#hasStableIds()
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}

	/* (non-Javadoc)
	 * @see android.widget.ExpandableListAdapter#isChildSelectable(int, int)
	 */
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
	
	
	@Override
	public void onCheckedChanged(CompoundButton view, boolean isChecked) {
		
		Integer clickedWordId = (Integer) view.getTag(); 
		Log.w(WordsetActivity.class.getName(), "Change in checked state of word with id: " + String.valueOf( clickedWordId ));
		
		if(isChecked) { 
			selectedWordIds.add(clickedWordId);
		} else { 
			selectedWordIds.remove(clickedWordId);
		}
		
		wordsHasBeenSelected = true; 
	}
	
	public ArrayList<String> getSelectedWordIds()
	{
		ArrayList<String> wordIdsList = new ArrayList<String>(); 
		
		for(Integer wordId : selectedWordIds) { 
			wordIdsList.add(String.valueOf(wordId));
		}
		
		return wordIdsList;
	}
	
	public boolean wordsHasBeenLoaded() 
	{
		return wordsHasBeenLoaded; 
	}
	
	public boolean wordsHasBeenSelected() 
	{
		if(wordIds.size() != selectedWordIds.size())
			return true; 
		
		return false; 
	}

}
