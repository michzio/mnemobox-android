/**
 * @date 08.10.2014
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
public class UserWordsetWordsProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	private static final String AUTHORITY = "pl.elector.provider.UserWordsetWordsProvider"; 
	private static final String BASE_PATH = "user_wordset_words"; 
	private static final String BASE_PATH_WORD_USER_WORDSETS = "word_user_wordsets";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	public static final Uri CONTENT_URI_WORD_USER_WORDSETS  = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_WORD_USER_WORDSETS);
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// 1) request for all words contained in given user_wordset
	// 2) request for user_wordsets to which given word belongs 
	// 3) request for all rows 
	// 4) request for single row (checking whether given word belongs to given user_wordset)
	private static final int ALLROWS = 1; 
	private static final int WORDS_FOR_USER_WORDSET = 2; 
	private static final int USER_WORDSETS_FOR_WORD = 3; 
	private static final int SINGLE_ROW = 4; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'user_wordset_words' represents a request for all rows 
	// and 'user_wordset_words/[userWordsetId]'represents a request for 
	// subset of words in given user_wordset using INNER JOIN, 
	// and 'word_user_wordsets/[wordId]' represents a request for 
	// subset of user_wordsets for given word using INNER JOIN 
	// and 'user_wordset_words/[userWordsetId]/[wordId]' query a single row 
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		// content://pl.elector.contentprovider/user_wordset_words/[#userWordsetId]/[#wordId]
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", WORDS_FOR_USER_WORDSET);
		uriMatcher.addURI(AUTHORITY, BASE_PATH_WORD_USER_WORDSETS + "/#", USER_WORDSETS_FOR_WORD);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (user_wordset_words)
	// and for subset of words for given user_wordset
	// and for subset of user_wordsets for given word 
	// and for single row item in user_wordset_words table
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.user_wordset_words";
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.user_wordset_words"; 
	private static final String CONTENT_USER_WORDSETS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.user_wordsets"; 
	private static final String CONTENT_WORDS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.words"; 
	
	// projection map for user wordset words query 
	private static final Map<String, String> userWordsetWordsColumnMap; 
	
	static { 
		userWordsetWordsColumnMap = new HashMap<String, String>(); 
		// "wordTable._id" => "wordTable._id AS _id" 
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(  WordProvider.WordTable.COLUMN_WORD_ID),
									   WordProvider.WordTable.addPrefix( WordProvider.WordTable.COLUMN_WORD_ID) + " AS " + WordProvider.WordTable.COLUMN_WORD_ID);
		// "wordTable.foreignArticle" => "wordTable.foreignArticle AS foreignArticle"
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE),
				   					   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE) + " AS " + WordProvider.WordTable.COLUMN_FOREIGN_ARTICLE);
		// "wordTable.foreignWord" => "wordTable.foreignWord AS foreignWord" 
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_WORD),
				   					   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_FOREIGN_WORD) + " AS " + WordProvider.WordTable.COLUMN_FOREIGN_WORD);
		// "wordTable.nativeArticle" => "wordTable.nativeArticle AS nativeArticle"
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE),
				   					   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_ARTICLE) + " AS " + WordProvider.WordTable.COLUMN_NATIVE_ARTICLE);
		// "wordTable.nativeWord" => "wordTable.nativeWord AS nativeWord" 
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_WORD),
				   					   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_NATIVE_WORD) + " AS " + WordProvider.WordTable.COLUMN_NATIVE_WORD);
		// "wordTable.transcription" => "wordTable.transcription AS transcription" 
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_TRANSCRIPTION),
				   					   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_TRANSCRIPTION) + " AS " + WordProvider.WordTable.COLUMN_TRANSCRIPTION);
		// "wordTable.recording" => "wordTable.recording AS recording"
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_RECORDING),
				   					   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_RECORDING) + " AS " + WordProvider.WordTable.COLUMN_RECORDING);
		// "wordTable.imagePath" => "wordTable.imagePath AS imagePath" 
		userWordsetWordsColumnMap.put( WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_IMAGE),
									   WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_IMAGE) + " AS " + WordProvider.WordTable.COLUMN_IMAGE);		
		// "userWordsetWordsTable.userWordsetId" => "userWordsetWordsTable.userWordsetId AS userWordsetId" 
		userWordsetWordsColumnMap.put(UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID), 
				  					  UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID) + " AS " + UserWordsetWordsTable.COLUMN_USER_WORDSET_ID);
		// "userWordsetWordsTable.wordId" => "userWordsetWordsTable.wordId AS wordId"
		userWordsetWordsColumnMap.put(UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_WORD_ID),
									  UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_WORD_ID) + " AS " + UserWordsetWordsTable.COLUMN_WORD_ID);
	}
	
	// projection map for word user_wordsets query
	private static final Map<String, String> wordUserWordsetsColumnMap;
	
	static {
		wordUserWordsetsColumnMap = new HashMap<String, String>(); 
		// "userWordsetTable._id" => "userWordsetTable._id AS _id"
		wordUserWordsetsColumnMap.put( UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID),
				   					   UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID) + " AS " + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID);
		// "userWordsetTable.userId" => "userWordsetTable.userId AS userId"
		wordUserWordsetsColumnMap.put( UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_USER_ID),
									   UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_USER_ID) + " AS " + UserWordsetProvider.UserWordsetTable.COLUMN_USER_ID);
		// "userWordsetTable.wordsetForeignName" => "userWordsetTable.wordsetForeignName AS wordsetForeignName"
		wordUserWordsetsColumnMap.put( UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_FOREIGN_NAME),
				   					   UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_FOREIGN_NAME) + " AS " + UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_FOREIGN_NAME);
		// "userWordsetTable.wordsetNativeName" => "userWordsetTable.wordsetNativeName AS wordsetNativeName"
		wordUserWordsetsColumnMap.put( UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_NATIVE_NAME),
									   UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_NATIVE_NAME) + " AS " + UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_NATIVE_NAME);
		// "userWordsetTable.wordsetDescription" => "userWordsetTable.wordsetDescription AS wordsetDescription"
		wordUserWordsetsColumnMap.put( UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_ABOUT),
				   					   UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_ABOUT) + " AS " + UserWordsetProvider.UserWordsetTable.COLUMN_WORDSET_ABOUT); 
		// "userWordsetWordsTable.userWordsetId" => "userWordsetWordsTable.userWordsetId AS userWordsetId" 
		wordUserWordsetsColumnMap.put(UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID), 
						  			  UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID) + " AS " + UserWordsetWordsTable.COLUMN_USER_WORDSET_ID);
		// "userWordsetWordsTable.wordId" => "userWordsetWordsTable.wordId AS wordId"
		wordUserWordsetsColumnMap.put(UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_WORD_ID),
									  UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_WORD_ID) + " AS " + UserWordsetWordsTable.COLUMN_WORD_ID);
	}
	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single user_wordset words item 
	 * or subset of user_wordset words items for given word ID 
	 * or given wordset ID depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a single row URI limit the deletion to specified row (userWordsetId AND wordId) 
		// else if this is a user_wordset_words URI then limit the deletion to the subset of word references for given userWordsetId
		// else if this is a word_user_wordsets URI then limit the deletion to the subset of user_wordset references for given wordId
		// else this is all rows deletion then use passed in selection and selectionArgs parameters. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
			{
				String userWordsetID = uri.getPathSegments().get(1); 
				String wordID = uri.getPathSegments().get(2); 
				selection = UserWordsetWordsTable.COLUMN_USER_WORDSET_ID + "=" + userWordsetID
						+ " AND " + UserWordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			}
			case WORDS_FOR_USER_WORDSET: 
			{
				String userWordsetID = uri.getPathSegments().get(1); 
				selection = UserWordsetWordsTable.COLUMN_USER_WORDSET_ID + "=" + userWordsetID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			}
			case USER_WORDSETS_FOR_WORD: 
			{
				String wordID = uri.getPathSegments().get(1); 
				selection = UserWordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			default: break; 
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, 
		// pass in "1".
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS, 
									selection, selectionArgs); 
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type depending on the query type:
	 * single row, all rows, subset of user_wordsets for given word or subset of words for 
	 * give user_wordset
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return suitable MIME type.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				return CONTENT_ITEM_MIME_TYPE; 
			case ALLROWS:
				return CONTENT_MIME_TYPE; 
			case WORDS_FOR_USER_WORDSET: 
				return CONTENT_WORDS_MIME_TYPE; 
			case USER_WORDSETS_FOR_WORD: 
				return CONTENT_USER_WORDSETS_MIME_TYPE; 
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
		long userWordsetID = -1; 
		// checking whether Content URI address is suitable
		switch(uriMatcher.match(uri))
		{
			case ALLROWS:
				// insert the values into the table 
				try {
					db.insertOrThrow(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS,
									  nullColumnHack, values);
					userWordsetID = values.getAsInteger(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID);
					wordID = values.getAsInteger(UserWordsetWordsTable.COLUMN_WORD_ID);
				} catch(SQLiteException ex) {
					Log.w(UserWordsetWordsProvider.class.getName(), 
							"Error while inserting new user wordset words row into userWordsetWordsTable.");
					return null; 
				}
				break;
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		if( userWordsetID > -1 && wordID > -1)
		{
			// construct and return the URI of the newly inserted row.
			Uri insertedItemUri = 
					ContentUris.withAppendedId(
							ContentUris.withAppendedId(CONTENT_URI, userWordsetID), wordID); 
			
			// notify any observers of the change in the data set. 
			getContext().getContentResolver().notifyChange(insertedItemUri, null);
			
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
		// returns true if the provider was successfully loaded 
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to 
	 * differentiate queries for single row, all rows and for set of words for 
	 * given userWordsetID, and for set of user_wordsets for given wordID. 
	 * SQLite Query Builder is used as a helper object for performing row-based, 
	 * word-based, user_wordset-based queries.
	 * @projection - ACCEPTS ONLY FULLY QUALIFIED COLUMN NAMES ex. table_name.column_name 
	 * 				for WORDS_FOR_USER_WORDSET and USER_WORDSETS_FOR_WORD queries
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
		
		switch( uriMatcher.match(uri) )
		{
			case SINGLE_ROW: 
			{
				queryBuilder.setTables(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS);
				// getting userWordsetID and wordID for current query and setting where clause
				String userWordsetID = uri.getPathSegments().get(1); 
				String wordID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID + "=" + userWordsetID
										+ " AND " + UserWordsetWordsTable.COLUMN_WORD_ID + "=" + wordID);
				break; 
			}
			case ALLROWS: 
				queryBuilder.setTables(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS);
				break;
			case WORDS_FOR_USER_WORDSET:
			{
				// creating inner joined word and user_wordset_words tables
				StringBuilder sb = new StringBuilder(); 
				sb.append(WordProvider.WordTable.TABLE_WORD);
				sb.append(" INNER JOIN "); 
				sb.append(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS);
				sb.append(" ON ("); 
				// appending column name prefixed with table name 
				sb.append(WordProvider.WordTable.addPrefix(WordProvider.WordTable.COLUMN_WORD_ID));
				sb.append(" = "); 
				// appending column name prefixed with table name 
				sb.append(UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_WORD_ID));
				sb.append(")"); 
				
				String joinTables = sb.toString(); 
				// setting joined tables and projection map 
				queryBuilder.setTables(joinTables);
				queryBuilder.setProjectionMap(userWordsetWordsColumnMap);
				
				// getting userWordsetID for current query and setting Where clause 
				String userWordsetID = uri.getPathSegments().get(1);
				final String COLUMN_USER_WORDSET_ID = UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID);
				queryBuilder.appendWhere(COLUMN_USER_WORDSET_ID + "=" + userWordsetID); 
				
				break; 
			}
			case USER_WORDSETS_FOR_WORD: 
			{
				// creating inner joined user_wordset and user_wordset_words tables
				StringBuilder sb = new StringBuilder(); 
				sb.append(UserWordsetProvider.UserWordsetTable.TABLE_USER_WORDSET);
				sb.append(" INNER JOIN "); 
				sb.append(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS);
				sb.append(" ON ("); 
				// appending column name prefixed with table name 
				sb.append(UserWordsetProvider.UserWordsetTable.addPrefix(UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID));
				sb.append(" = "); 
				// appending column name prefixed with table name 
				sb.append(UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_USER_WORDSET_ID));
				sb.append(")"); 
				
				String joinTables = sb.toString(); 
				// setting joined tables and projection map 
				queryBuilder.setTables(joinTables);
				queryBuilder.setProjectionMap(wordUserWordsetsColumnMap);
				
				// getting wordID for current query and setting where clause 
				String wordID = uri.getPathSegments().get(1); 
				final String COLUMN_WORD_ID = UserWordsetWordsTable.addPrefix(UserWordsetWordsTable.COLUMN_WORD_ID);
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
		
		// If this is an update of a single row, modify selection argument 
		// to indicate that row, else if this is update of a set of rows 
		// for given userWordsetID modify selection argument to indicate that rows,
		// else if this is update of a set of rows for given wordID modify selection 
		// argument to indicate that rows. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
			{
				String userWordsetID = uri.getPathSegments().get(1); 
				String wordID = uri.getPathSegments().get(2); 
				selection = UserWordsetWordsTable.COLUMN_USER_WORDSET_ID + "=" + userWordsetID
						+ " AND " + UserWordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			}
			case WORDS_FOR_USER_WORDSET: 
			{
				String userWordsetID = uri.getPathSegments().get(1); 
				selection = UserWordsetWordsTable.COLUMN_USER_WORDSET_ID + "=" + userWordsetID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			}
			case USER_WORDSETS_FOR_WORD: 
			{
				String wordID = uri.getPathSegments().get(1); 
				selection = UserWordsetWordsTable.COLUMN_WORD_ID + "=" + wordID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(UserWordsetWordsTable.TABLE_USER_WORDSET_WORDS,
									values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount;
	}
	
	public static class UserWordsetWordsTable {
		
		// Database Table 
		public static final String TABLE_USER_WORDSET_WORDS = "userWordsetWordsTable"; 
		public static final String COLUMN_USER_WORDSET_ID = "userWordsetId"; 
		public static final String COLUMN_WORD_ID = "wordId"; 
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_USER_WORDSET_WORDS
				+ " ("
				+ COLUMN_USER_WORDSET_ID + " integer not null, "
				+ COLUMN_WORD_ID + " integer not null, "
				+ " primary key (" 
				+ COLUMN_USER_WORDSET_ID + ", " + COLUMN_WORD_ID
				+ ")"
				+ " foreign key(" + COLUMN_USER_WORDSET_ID + ") references "
				+ UserWordsetProvider.UserWordsetTable.TABLE_USER_WORDSET
				+ "(" + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID + ")"
				+ " on update cascade on delete cascade"
				+ " foreign key(" + COLUMN_WORD_ID + ") references "
				+ WordProvider.WordTable.TABLE_WORD
				+ "(" + WordProvider.WordTable.COLUMN_WORD_ID + ")"
				+ " on update cascade on delete cascade"
				+ " );"; 
		
		// TRIGGERS: 
		// 1) insert trigger on user_wordset_words table, checking if corresponding user_wordset exists
		private static final String USER_WORDSET_INSERT_TRIGGER_CREATE = "create trigger fki_" 
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID + " "
				+ "before insert on " + TABLE_USER_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_USER_WORDSET_WORDS
									+ " violates foreign key constraint') "
					+ "where (select " + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID
							+ " from " + UserWordsetProvider.UserWordsetTable.TABLE_USER_WORDSET
							+ " where " + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID
							+ " = new." + COLUMN_USER_WORDSET_ID + ") is null;"
				+ " end;";
		
		// 2) insert trigger on user_wordset_words table, checking if corresponding word exists
		private static final String WORD_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " " 
				+ "before insert on " + TABLE_USER_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_USER_WORDSET_WORDS
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " from " + WordProvider.WordTable.TABLE_WORD
							+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 3) update trigger on user_wordset_words table, checking if new user_wordset exists 
		private static final String USER_WORDSET_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID + " "
				+ "before update on " + TABLE_USER_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_USER_WORDSET_WORDS
									+ " violates foreign key constraint') "
					+ "where (select " + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID
							+ " from " + UserWordsetProvider.UserWordsetTable.TABLE_USER_WORDSET
							+ " where " + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID
							+ " = new." + COLUMN_USER_WORDSET_ID + ") is null;"
				+ " end;";
		
		// 4) update trigger on user_wordset_words table, checking if new word exists 
		private static final String WORD_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " "
				+ "before update on " + TABLE_USER_WORDSET_WORDS + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_USER_WORDSET_WORDS
									+ " violates foreign key constraint') "
					+ "where (select " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " from " + WordProvider.WordTable.TABLE_WORD
							+ " where " + WordProvider.WordTable.COLUMN_WORD_ID
							+ " = new." + COLUMN_WORD_ID + ") is null;"
				+ " end;";
		
		// 5) delete trigger on user_wordset table, cascade deletes rows in user_wordset_words
		private static final String USER_WORDSET_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID + " "
				+ "before delete on " + UserWordsetProvider.UserWordsetTable.TABLE_USER_WORDSET + " "
				+ "for each row begin "
					+ "delete from " + TABLE_USER_WORDSET_WORDS 
						  + " where " + COLUMN_USER_WORDSET_ID
						  + " = old." + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID 
						  + "; "
				+ "end;";
		
		// 6) delete trigger on word table, cascade deletes rows in user_wordset_words
		private static final String WORD_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " "
				+ "before delete on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "delete from " + TABLE_USER_WORDSET_WORDS
						  + " where " + COLUMN_WORD_ID 
						  + " = old." + WordProvider.WordTable.COLUMN_WORD_ID
						  + "; "
				+ "end;";
		
		// 7) update trigger on user_wordset table, cascade updates rows in user_wordset_words
		private static final String USER_WORDSET_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID + " "
				+ "after update on " + UserWordsetProvider.UserWordsetTable.TABLE_USER_WORDSET + " "
				+ "for each row begin "
					+ "update " + TABLE_USER_WORDSET_WORDS + " set " + COLUMN_USER_WORDSET_ID 
						+ " = new." + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID
						+ " where " + COLUMN_USER_WORDSET_ID 
						+ " = old." + UserWordsetProvider.UserWordsetTable.COLUMN_USER_WORDSET_ID
						+ "; "
				+ " end;";
		
		// 8) update trigger on word table, cascade updates rows in user_wordset_words
		private static final String WORD_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID + " "
				+ "after update on " + WordProvider.WordTable.TABLE_WORD + " "
				+ "for each row begin "
					+ "update " + TABLE_USER_WORDSET_WORDS + " set " + COLUMN_WORD_ID
						+ " = new." + WordProvider.WordTable.COLUMN_WORD_ID
						+ " where " + COLUMN_WORD_ID
						+ " = old." + WordProvider.WordTable.COLUMN_WORD_ID
						+ "; "
				+ " end;";
		
		// called when no database exist in disk and the SQLiteOpenHelper 
		// class needs to create a new one.
		public static void onCreate(SQLiteDatabase database)
		{
			// UserWordsetWords table creation in database (with additional triggers)
			database.execSQL(TABLE_CREATE);
			database.execSQL(USER_WORDSET_INSERT_TRIGGER_CREATE);
			database.execSQL(WORD_INSERT_TRIGGER_CREATE);
			database.execSQL(USER_WORDSET_UPDATE_TRIGGER_CREATE);
			database.execSQL(WORD_UPDATE_TRIGGER_CREATE);
			database.execSQL(USER_WORDSET_DELETE_TRIGGER_CREATE);
			database.execSQL(WORD_DELETE_TRIGGER_CREATE); 
			database.execSQL(USER_WORDSET_PARENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(WORD_PARENT_UPDATE_TRIGGER_CREATE);
		}
		
		// called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version.
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
		{
			// Log the version upgrade 
			Log.w(UserWordsetWordsTable.class.getName(),
					"Upgrading database UserWordsetWords table from version "
					+ oldVersion + " to " + newVersion + ", which will destroy all old data.");
			
			// Upgrade the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values.
			
			// Upgrade database by adding new version of UserWordsetWords table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_WORDSET_WORDS); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_USER_WORDSET_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_USER_WORDSET_WORDS + "_" + COLUMN_WORD_ID); 
			onCreate(database); 
		}
		
		public static String addPrefix(String columnName)
		{
			return TABLE_USER_WORDSET_WORDS + "." + columnName; 
		}
				
	}

}
