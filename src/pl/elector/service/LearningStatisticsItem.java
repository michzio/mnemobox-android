package pl.elector.service;

import com.google.gson.annotations.SerializedName;

public class LearningStatisticsItem {

	@SerializedName("profile_id")
	private int profileId; 
	
	@SerializedName("access_date")
	private String accessDate; 
	
	@SerializedName("bad_answers")
	private int badAnswers; 
	
	@SerializedName("good_answers")
	private int goodAnswers;
	
	public LearningStatisticsItem() { }
	
	public LearningStatisticsItem(int profileId, String accessDate, int badAnswers, int goodAnswers) 
	{
		this(); 
		this.profileId = profileId; 
		this.accessDate = accessDate; 
		this.badAnswers = badAnswers; 
		this.goodAnswers = goodAnswers; 
	}
	
	public void setProfileId(int profileId) { 
		this.profileId = profileId; 
	}
	
	public int getProfileId() { 
		return profileId; 
	}
	
	public void setAccessDate(String accessDate) {
		this.accessDate = accessDate; 
	}
	
	public String getAccessDate() { 
		return accessDate; 
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
}
