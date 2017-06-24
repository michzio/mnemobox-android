/**
 * @date 13.09.2014
 */
package pl.elector.database;

import java.util.HashMap;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author Michał Ziobro 
 *
 */
public class WordsetWordsProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.WordsetWordsProvider";
	private static final String BASE_PATH_WORDSET_WORDS = "wordset_words"; 
	private static final String BASE_PATH_WORD_WORDSETS = "word_wordsets"; 
	
	public static final Uri CONTENT_URI_WORDSET_WORDS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_WORDSET_WORDS); 
	public static final Uri CONTENT_URI_WORD_WORDSETS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_WORD_WORDSETS); 
	public static final Uri CONTENT_URI = CONTENT_URI_WORDSET_WORDS; // simple content uri for all rows
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// 1) request for wordsets to which given word belongs
	// 2) request for all words contained in given wordset
	private static final int ALLROWS = 1; 
	private static final int WORDS_FOR_WORDSET = 2; 
	private static final int WORDSETS_FOR_WORD = 3; 
	private static final int SINGLE_ROW = 4; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'wordset_words' represents a request for all rows 
	// and 'wordset_words/[wordsetId]' represents a request for 
	// subset of words in given wordset using INNER JOIN, 
	// and 'word_wordsets/[wordId]' represents a request for 
	// subset of wordsets for given word  using INNER JOIN 
	static { 
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH_WORDSET_WORDS, ALLROWS);
		// content://pl.elector.contentprovider/wordset_words/[#wordsetId]/[#wordID]    
		uriMatcher.addURI(AUTHORITY, BASE_PATH_WORDSET_WORDS + "/#/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH_WORDSET_WORDS + "/#", WORDS_FOR_WORDSET);
		uriMatcher.addURI(AUTHORITY, BASE_PATH_WORD_WORDSETS + "/#", WORDSETS_FOR_WORD); 
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper;
	
	// defining the MIME types for all rows (wordset_words) 
	// and for subset of words for given wordset 
	// and for subset of wordsets for given word
	private static final String CONTENT_WORDSET_WORDS_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.wordset_words"; 
	private static final String CONTENT_WORDSET_WORDS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.wordset_words"; 
	private static final String CONTENT_WORDSETS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.wordsets";
	private static final String CONTENT_WORDS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.words"; 
	
	
	// projection map for wordset words query 
	private static final Map<String, String> wordsetWordsColumnMap; 
	
	static { 
		wordsetWordsColumnMap = new HashMap<String, String>();
		// "wordTable._id" => "wordTable._id AS _id" 
		wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_WORD_ID), 
				                   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_WORD_ID) + " AS " + WordProvider.WordTable.COLUMN_WORD_ID);
		// "wordTable.foreignArticle" => "wordTable.foreignArticle AS foreignArticle"
		wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE),
								   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE) + " AS " + WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE);
		// "wordTable.foreignWord" => "wordTable.foreignWord AS foreignWord"
		wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_WORD),
								   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_WORD) + " AS " + WordProvider.WordTable.COLUMN_FOREIGN_WORD);
		// "wordTable.nativeArticle" => "wordTable.nativeArticle AS nativeArticle"
		wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE),
								   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE) + " AS " + WordProvider.WordTable.COLUMN_NATIVE_ARTICLE);
		// "wordTable.nativeWord" => "wordTable.nativeWord AS nativeWord"
		wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_WORD),
								   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_WORD) + " AS " + WordProvider.WordTable.COLUMN_NATIVE_WORD);
		// "wordTable.transcription" => "wordTable.transcription AS transcription"
	    wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_TRANSCRIPTION),
								   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_TRANSCRIPTION) + " AS " + WordProvider.WordTable.COLUMN_TRANSCRIPTION);
	    // "wordTable.recording" => "wordTable.recording AS recording"
	 	wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_RECORDING),
	 							   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_RECORDING) + " AS " + WordProvider.WordTable.COLUMN_RECORDING);
	 	// "wordTable.imagePath" => "wordTable.imagePath AS imagePath"
	 	wordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_IMAGE),
	 							   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_IMAGE) + " AS " + WordProvider.WordTable.COLUMN_IMAGE);		
	 	// "wordsetWordsTable.wordsetId" => "wordsetWordsTable.wordsetId AS wordsetId" 
		wordsetWordsColumnMap.put(WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORDSET_ID), 
								  WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORDSET_ID) + " AS " + WordsetWordsTable.COLUMN_WORDSET_ID);
		// "wordsetWordsTable.wordId" => "wordsetWordsTable.wordId AS wordId"
		wordsetWordsColumnMap.put(WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORD_ID),
								  WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORD_ID) + " AS " + WordsetWordsTable.COLUMN_WORD_ID);
		
	}
	
	// projection map for word wordsets query 
	private static final Map<String, String> wordWordsetsColumnMap;
	
	static {
		wordWordsetsColumnMap = new HashMap<String, String>();
		// "wordsetTable._id" => "wordsetTable._id AS _id"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_ID),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_ID) + " AS " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID);
		// "wordsetTable.categoryId" => "wordsetTable.categoryId AS categoryId"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_CATEGORY_ID),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_CATEGORY_ID) + " AS " + WordsetProvider.WordsetTable.COLUMN_CATEGORY_ID);
		// "wordsetTable.wordsetForeignName" => "wordsetTable.wordsetForeignName AS wordsetForeignName"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME) + " AS " + WordsetProvider.WordsetTable.COLUMN_WORDSET_FOREIGN_NAME);
		// "wordsetTable.wordsetNativeName" => "wordsetTable.wordsetNativeName AS wordsetNativeName"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME) + " AS " +  WordsetProvider.WordsetTable.COLUMN_WORDSET_NATIVE_NAME);
		// "wordsetTable.wordsetLevel" => "wordsetTable.wordsetLevel AS wordsetLevel"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL) + " AS " +  WordsetProvider.WordsetTable.COLUMN_WORDSET_LEVEL);
		// "wordsetTable.wordsetDescription" => "wordsetTable.wordsetDescription AS wordsetDescription"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT) + " AS " +  WordsetProvider.WordsetTable.COLUMN_WORDSET_ABOUT);
		// "wordsetTable.isAudioStoredLocally" => "wordsetTable.isAudioStoredLocally AS isAudioStoredLocally"
		wordWordsetsColumnMap.put( WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY),
								   WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY) + " AS " +  WordsetProvider.WordsetTable.COLUMN_IS_AUDIO_STORED_LOCALLY);
		// "wordsetWordsTable.wordsetId" => "wordsetWordsTable.wordsetId AS wordsetId"
		wordWordsetsColumnMap.put( WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORDSET_ID), 
				  				   WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORDSET_ID) + " AS " + WordsetWordsTable.COLUMN_WORDSET_ID);
		// "wordsetWordsTable.wordId" => "wordsetWordsTable.wordId AS wordId"
		wordWordsetsColumnMap.put(WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORD_ID),
				  				  WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORD_ID) + " AS " + WordsetWordsTable.COLUMN_WORD_ID);

	}  

	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single wordset words item or subset of wordset words items
	 *  for given word ID or given wordset ID depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a single row URI limit the deletion to specified row (wordsetId AND wordID) 
		// else if this is a wordset_words URI then limit the deletion to the subset of word references for given wordsetId 
		// else if this is a word_wordsets URI then limit the deletion to the subset of wordset references for given wordId 
		// else this is all rows deletion then use passed in selection and selectionArgs parameters.
		switch( uriMatcher.match(uri) ) 
		{
			case SINGLE_ROW: {
				String wordsetID = uri.getPathSegments().get(1); 
				String wordID = uri.getPathSegments().get(2); 
				selection  = WordsetWordsTable.COLUMN_WORDSET_ID + "=" + wordsetID
						+ " AND " + WordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+  selection + ")" : "");
				break; 
			}
			case WORDS_FOR_WORDSET: {
				String wordsetID = uri.getPathSegments().get(1); 
				selection = WordsetWordsTable.COLUMN_WORDSET_ID + "=" + wordsetID
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break; 
			}
			
			case WORDSETS_FOR_WORD: {
				String wordID = uri.getPathSegments().get(1); 
				selection = WordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
					    + selection + ")" : ""); 
				break; 
			}
			default: break; 
				
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1". 
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(WordsetWordsTable.TABLE_WORDSET_WORDS, 
									selection, selectionArgs); 
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type depending on the query type: 
	 * single row, all rows, subset of wordsets for given word or subset of words for given wordset.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return suitable MIME type. 
		switch(uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				return CONTENT_WORDSET_WORDS_ITEM_MIME_TYPE; 
			case ALLROWS: 
				return CONTENT_WORDSET_WORDS_MIME_TYPE; 
			case WORDS_FOR_WORDSET: 
				return CONTENT_WORDS_MIME_TYPE; 
			case WORDSETS_FOR_WORD: 
				return CONTENT_WORDSETS_MIME_TYPE; 
			default: 
				throw new IllegalArgumentException("Unsupported URI: " + uri); 
		}
	
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
	 * Transaction method used to insert a new row into database (represented by ContentValues)
	 */
	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// To add empty rows to your database by passing in an empty
		// Content Values object, you must use the null column hack 
		// parameter to specify the name of the column that can be set to null. 
		String nullColumnHack = null; 
		
		long wordID = -1;
		long wordsetID = -1; 
		//checking whether Content URI address is suitable 
		switch(uriMatcher.match(uri))
		{
			case ALLROWS: 
				// insert the values into the table 
				try { 
					db.insertOrThrow(WordsetWordsTable.TABLE_WORDSET_WORDS, 
									 nullColumnHack, values);
					wordsetID = values.getAsInteger(WordsetWordsTable.COLUMN_WORDSET_ID);
					wordID = values.getAsInteger(WordsetWordsTable.COLUMN_WORD_ID); 
				} catch(SQLiteException ex) {
					Log.w(WordsetWordsProvider.class.getName(), "Error while inserting new wordset words row into wordsetWordsTable."); 
					return null; 
				}
				break; 
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		if( wordsetID > -1 && wordID > -1)
		{
			// construct and return the URI of the newly inserted row. 
			Uri insertedItemUri = 
				 ContentUris.withAppendedId(
							ContentUris.withAppendedId(CONTENT_URI, wordsetID), wordID);
			
			// notify any observers of the change in the data set. 
			getContext(). getContentResolver().notifyChange(insertedItemUri, null);
			
			return insertedItemUri; 	
		} else 
			return null;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public synchronized boolean onCreate() {
		// creating instance of SQLiteOpenHelper that effectively 
		// defer creating and opening a database until it's required 
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		//returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to 
	 * differentiate queries for single row, all rows and for set of words for a given wordsetID, 
	 * and for set of wordsets for a given wordID.
	 * SQLite Query Builder is used as a helper object for performing row-based, word-based and wordset-based queries.
	 * @projection - ACCEPTS ONLY FULLY QUALIFIED COLUMN NAMES! ex. table_name.column_name
	 */
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {
		
		// Open the underlying database 
		SQLiteDatabase db; 
		try { 
			db = databaseHelper.getWritableDatabase(); 
		} catch(SQLiteException ex) {
			db = databaseHelper.getReadableDatabase(); 
		}
		
		// Replace this with valid SQL statements if necessary 
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method 
		// in order to simplify database query construction
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
			{
				queryBuilder.setTables(WordsetWordsTable.TABLE_WORDSET_WORDS); 
				//getting wordsetID and wordID for current query and setting Where clause
				String wordsetID = uri.getPathSegments().get(1);
				String wordID = uri.getPathSegments().get(2);
				queryBuilder.appendWhere(WordsetWordsTable.COLUMN_WORDSET_ID + "=" + wordsetID 
										 + " AND " + WordsetWordsTable.COLUMN_WORD_ID + "=" + wordID);
				break; 
			}
			case ALLROWS: 
				queryBuilder.setTables(WordsetWordsTable.TABLE_WORDSET_WORDS); 
				break;
			case WORDS_FOR_WORDSET: 
			{
				// creating inner joined word and wordset_words tables 
				StringBuilder sb = new StringBuilder(); 
				sb.append(WordProvider.WordTable.TABLE_WORD);
				sb.append(" INNER JOIN ");
				sb.append(WordsetWordsTable.TABLE_WORDSET_WORDS); 
				sb.append(" ON (");
				// appending column name prefixed with table name  
				sb.append(WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_WORD_ID));
				sb.append(" = ");
				// appending column name prefixed with table name 
				sb.append(WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORD_ID));
				sb.append(")");
				
				String joinTables = sb.toString(); 
				//setting joined tables and projection map
				queryBuilder.setTables(joinTables);  
				queryBuilder.setProjectionMap(wordsetWordsColumnMap); 
				
				//getting wordsetID for current query and setting Where clause 
				String wordsetID = uri.getPathSegments().get(1); 
				final String COLUMN_WORDSET_ID = WordsetWordsTable.addPrefix( WordsetWordsTable.COLUMN_WORDSET_ID);
				queryBuilder.appendWhere(COLUMN_WORDSET_ID + "=" + wordsetID); 
				
				break;
			}
			case WORDSETS_FOR_WORD: 
			{
				// creating inner joined wordset and wordset_words tables
				StringBuilder sb = new StringBuilder(); 
				sb.append(WordsetProvider.WordsetTable.TABLE_WORDSET);
				sb.append(" INNER JOIN "); 
				sb.append(WordsetWordsTable.TABLE_WORDSET_WORDS);
				sb.append(" ON ("); 
				// appending column name prefixed with table name
				sb.append(WordsetProvider.WordsetTable.addPrefix(WordsetProvider.WordsetTable.COLUMN_WORDSET_ID));
				sb.append(" = ");
				// appending column name prefixed with table name 
				sb.append(WordsetWordsTable.addPrefix(WordsetWordsTable.COLUMN_WORDSET_ID));
				sb.append(")"); 
				
				String joinTables = sb.toString();
				//setting joined tables and projection map
				queryBuilder.setTables(joinTables);
				queryBuilder.setProjectionMap(wordWordsetsColumnMap); 
				
				//getting wordID for current query and setting Where clause 
				String wordID = uri.getPathSegments().get(1); 
				final String COLUMN_WORD_ID = WordsetWordsTable.addPrefix( WordsetWordsTable.COLUMN_WORD_ID); 
				queryBuilder.appendWhere(COLUMN_WORD_ID + "=" + wordID); 
		
				break;
			}
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 	
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		// returning the result set Cursor
		return cursor;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is an update of single row, modify selection argument
		// to indicate that row, else if this is update of a set of rows for given 
		// wordsetID modify selection argument to indicate that rows, else if 
		// this is update of a set of rows for given wordID modify selection argument
		// to indicate that rows.
		switch( uriMatcher.match(uri) )
		{
			case SINGLE_ROW:
			{
				String wordsetID = uri.getPathSegments().get(1); 
				String wordID = uri.getPathSegments().get(2); 
				selection = WordsetWordsTable.COLUMN_WORDSET_ID + "=" + wordsetID 
						    + " AND " + WordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						    + (!TextUtils.isEmpty(selection) ? " AND ("
						    + selection + ")" : "");  
				
				break; 
			}
			case WORDS_FOR_WORDSET: 
			{
			    String wordsetID = uri.getPathSegments().get(1); 
			    selection = WordsetWordsTable.COLUMN_WORDSET_ID + "=" + wordsetID
			    		+ (!TextUtils.isEmpty(selection) ? " AND ("
			    		+ selection + ")" : ""); 
				break; 
			}
			case WORDSETS_FOR_WORD:
			{
				String wordID = uri.getPathSegments().get(1); 
				selection = WordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break;
			}
			default: break;
		}
		
		// Perform the update. 
		int updateCount = db.update(WordsetWordsTable.TABLE_WORDSET_WORDS,
									values, selection, selectionArgs ); 
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount;
	}
	
	public static class WordsetWordsTable {
		
		// Database Table 
		public static final String TABLE_WORDSET_WORDS = "wordsetWordsTable"; 
		public static final String COLUMN_WORDSET_ID = "wordsetId"; 
		public static final String COLUMN_WORD_ID = "wordId"; 
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_WORDSET_WORDS 
				+ " ("
				+ COLUMN_WORDSET_ID + " integer not null, "
				+ COLUMN_WORD_ID + " integer not null, "
				+ " primary key (" + COLUMN_WORDSET_ID + ", " + COLUMN_WORD_ID + ")" 
				+ " foreign key(" + COLUMN_WORDSET_ID + ") references "
				+ WordsetProvider.WordsetTable.TABLE_WORDSET 
				+ "(" + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID + ")"
				+ " on update cascade on delete cascade"
				+ " foreign key(" + COLUMN_WORD_ID + ") references "
				+ WordProvider.WordTable.TABLE_WORD
				+ "(" + WordProvider.WordTable.COLUMN_WORD_ID + ")"
				+ " on update cascade on delete cascade"
				+ " );";
		
		// TRIGGERS: 
		// 1) insert trigger on wordset_words table, checking wordset exists
		private static final String WORDSET_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID + " "
				+ "before insert on " + TABLE_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_WORDSET_WORDS 
									+ " violates foreign key constraint') "
					+ "where (select " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
							 + " from " + WordsetProvider.WordsetTable.TABLE_WORDSET
							 + " where " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID 
							 + " = new." + COLUMN_WORDSET_ID + ") is null;"
				+ " end;"; 
		
		// 2) insert trigger on wordset_words table, checking word exists
		private static final String WORD_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " "
				+ "before insert on " + TABLE_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_WORDSET_WORDS
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID 
							 + " from " + WordProvider.WordTable.TABLE_WORD 
							 + " where " + WordProvider.WordTable.COLUMN_WORD_ID
							 + " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 3) update trigger on wordset_words table, checking new wordset exists
		private static final String WORDSET_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID + " "
				+ "before update on " + TABLE_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_WORDSET_WORDS 
									+ " violates foreign key constraint') "
					+ "where (select " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID 
							  + " from " + WordsetProvider.WordsetTable.TABLE_WORDSET
							  + " where " + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID 
							  + " = new." + COLUMN_WORDSET_ID + ") is null;"
				+ " end;";
	
		// 4) update trigger on wordset_words table, checking new word exists 
		private static final String WORD_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " "
				+ "before update on " + TABLE_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_WORDSET_WORDS
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID 
							  + " from " + WordProvider.WordTable.TABLE_WORD 
							  + " where " + WordProvider.WordTable.COLUMN_WORD_ID
							  + " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 5) delete trigger on wordset table, cascade delete rows in wordset_words
		private static final String WORDSET_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID + " "
				+ "before delete on " + WordsetProvider.WordsetTable.TABLE_WORDSET + " "
				+ "for each row begin " 
					+ "delete from " + TABLE_WORDSET_WORDS + " where "
					+ COLUMN_WORDSET_ID  + " = old." + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID + "; "
				+ "end;"; 
	
		// 6) delete trigger on word table, cascade delete rows in wordset_words
		private static final String WORD_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " " 
				+ "before delete on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin " 
					+ "delete from " + TABLE_WORDSET_WORDS + " where "
					+ COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID + "; "
				+ "end;";
				
		// 7) update trigger on wordset table, cascade update rows in wordset_words
		private static final String WORDSET_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID + " "
				+ "after update on " + WordsetProvider.WordsetTable.TABLE_WORDSET + " "
				+ "for each row begin "
					+ "update " + TABLE_WORDSET_WORDS + " set " + COLUMN_WORDSET_ID  
					  + " = new." + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
					  + " where " + COLUMN_WORDSET_ID + " = old." + WordsetProvider.WordsetTable.COLUMN_WORDSET_ID
					  + "; "
				+ " end;";
				
		// 8) update trigger on word table, cascade update rows in wordset_words
		private static final String WORD_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " "
				+ "after update on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "update " + TABLE_WORDSET_WORDS + " set " + COLUMN_WORD_ID 
					+ " = new." + WordProvider.WordTable.COLUMN_WORD_ID
					+ " where " + COLUMN_WORD_ID + " = old." + WordProvider.WordTable.COLUMN_WORD_ID
					+ "; "
				+ " end;"; 
		
		// called when no database exist in disk and the SQLiteOpenHelper 
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database)
		{
			// WordsetWords table creation in database 
			database.execSQL(TABLE_CREATE);
			database.execSQL(WORDSET_INSERT_TRIGGER_CREATE);
			database.execSQL(WORD_INSERT_TRIGGER_CREATE); 
			database.execSQL(WORDSET_UPDATE_TRIGGER_CREATE);
			database.execSQL(WORD_UPDATE_TRIGGER_CREATE); 
			database.execSQL(WORDSET_DELETE_TRIGGER_CREATE);
			database.execSQL(WORD_DELETE_TRIGGER_CREATE); 
			database.execSQL(WORDSET_PARENT_UPDATE_TRIGGER_CREATE); 
			database.execSQL(WORD_PARENT_UPDATE_TRIGGER_CREATE); 
		}
		
		// called when there is a database version mismatch meaning that the version 
		// of the database on disk needs to be upgraded to the current version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
		{
			// Log the version upgrade 
			Log.w(WordsetWordsTable.class.getName(), 
				  "Upgrading database WordsetWords table from version " 
				   + oldVersion + " to " + newVersion + ", which will destroy all old data.");
			
			// Upgrade the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of WordsetWords table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_WORDSET_WORDS); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORDSET_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			onCreate(database); 
			
		}
		
		public static String addPrefix(String columnName)
		{
			return TABLE_WORDSET_WORDS + "." + columnName; 
		}
	}

}
