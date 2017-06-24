package pl.elector.service;

import com.google.gson.annotations.SerializedName;

public class LearningHistoryItem {
	
	@SerializedName("profile_id")
	private int profileId; 
	
	@SerializedName("wordset_id")
	private int wordsetId; 
	
	@SerializedName("mode_id")
	private int modeId; 
	
	@SerializedName("wordset_type_id")
	private int wordsetTypeId; 
	
	@SerializedName("bad_answers")
	private int badAnswers; 
	
	@SerializedName("good_answers")
	private int goodAnswers;
	
	@SerializedName("improvement")
	private float improvement;
	
	@SerializedName("hits")
	private int hits; 
	
	@SerializedName("last_access_date")
	private String lastAccessDate; 
	
	public LearningHistoryItem() { }
	
	public LearningHistoryItem(int profileId, int wordsetId, int modeId, int wordsetTypeId, 
			int badAnswers, int goodAnswers, float improvement, int hits, String lastAccessDate) { 
		this(); 
		this.profileId = profileId;
		this.wordsetId = wordsetId; 
		this.modeId = modeId; 
		this.wordsetTypeId = wordsetTypeId; 
		this.badAnswers = badAnswers; 
		this.goodAnswers = goodAnswers; 
		this.improvement = improvement; 
		this.hits = hits; 
		this.lastAccessDate = lastAccessDate; 
	}
	
	public void setProfileId(int profileId) {
		this.profileId = profileId;
	}
	public int getProfileId() {
		return profileId; 
	}
	public void setWordsetId(int wordsetId) {
		this.wordsetId = wordsetId; 
	}
	public int getWordsetId() { 
		return wordsetId; 
	}
	public void setModeId(int modeId) { 
		this.modeId = modeId; 
	}
	public int getModeId() { 
		return modeId; 
	}
	
	public void setWordsetTypeId(int wordsetTypeId) { 
		this.wordsetTypeId = wordsetTypeId; 
	}
	
	public int getWordsetTypeId() { 
		return wordsetTypeId; 
	}
	
	public void setBadAnswers(int badAnswers) { 
		this.badAnswers = badAnswers; 
	}
	
	public int getBadAnswers() { 
		return badAnswers; 
	}
	
	public void setGoodAnswers(int goodAnswers) { 
		this.goodAnswers = goodAnswers; 
	}
	
	public int getGoodAnswers() { 
		return goodAnswers; 
	}
	
	public void setImprovement(float improvement) { 
		this.improvement = improvement; 
	}
	
	public float getImprovement() { 
		return improvement; 
	}
	
	public void setHits(int hits) { 
		this.hits = hits; 
	}
	
	public int getHits() { 
		return hits; 
	}
	
	public void setLastAccessDate(String lastAccessDate) { 
		this.lastAccessDate = lastAccessDate; 
	}
	
	public String getLastAccessDate() { 
		return lastAccessDate; 
	}
	
}
