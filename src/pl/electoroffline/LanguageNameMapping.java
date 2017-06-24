package pl.electoroffline;

public class LanguageNameMapping {

	private static final java.util.HashMap<String, Integer> langNameResources; 
	
	static { 
		langNameResources = new java.util.HashMap<String, Integer>(); 
		langNameResources.put("en", R.string.english_lang); 
		langNameResources.put("pl", R.string.polish_lang); 
		langNameResources.put("de", R.string.german_lang); 
		langNameResources.put("es", R.string.spanish_lang);
		langNameResources.put("fr", R.string.french_lang);
		langNameResources.put("it", R.string.italian_lang);
		langNameResources.put("pt", R.string.portuguese_lang);
		langNameResources.put("tr", R.string.turkey_lang); 
		langNameResources.put("ar", R.string.arabic_lang); 
		langNameResources.put("ru",  R.string.russian_lang); 
	}
	
	public static int getResourceId(String languageCode) 
	{
		return langNameResources.get(languageCode); 
	}
}
