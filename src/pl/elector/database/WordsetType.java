package pl.elector.database;

public enum WordsetType {
	SYSTEM_WORDSET(1),
	FORGOTTEN_WORDSET(2),
	REMEMBER_ME_WORDSET(3), 
	LEARNED_WORDS_WORDSET(4), 
	USER_WORDSET(5),
	SELECTED_WORDS(6)
	;
	public static final String KEY_TYPE = "KEY_WORDSET_TYPE";
	
	// instance variable for storing corresponding typeId 
	private int typeID;
	
	WordsetType(int typeID) throws OutOfRangeException { 
		if(typeID < 0) 
			throw new OutOfRangeException("Wordset type identifier must be higher than zero."); 
		// set for current type item its identifier value
		this.typeID = typeID; 
		
	}
	
	public int id() { 
		return typeID; 
	}
	
	public static WordsetType name(int typeID) { 
		for(WordsetType type : values()) { 
			if(type.compare(typeID)) return type;
		}
		return null; 
	}
	
	public boolean compare(int typeID) { 
		if(this.typeID == typeID) 
			return true; 
		else 
			return false; 
	}
	
	// defining OutOfRangeException class
	static class OutOfRangeException extends ExceptionInInitializerError { 
				
		private String msg; 
				
		OutOfRangeException(String msg) { 
				this.msg = msg; 
		}
				
		@Override
		public String getMessage() { 
				return msg; 
		}
	}
}
