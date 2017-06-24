package pl.elector.database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class LearningStatsProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.LearningStatsProvider"; 
	private static final String BASE_PATH = "learning_stats";
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	// defining a UriMatcher to differentiate between different URI requests: 
	// 1) for all elements 
	// 2) subset of rows for given profile id 
	// 3) subset of rows from given date period
	// 4) subset of rows from and to given date 
	// 5) subset of rows in given day 
	// 6) subset of rows in given week
	// 7) subset of rows in given month
	// 8) single row for specific learning_stats id
	// 9) subset of rows for given profile id that have set not_synced to value 1 
	// 11) single row for unique combination of profile id and access date
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_PROFILE = 3; 
	private static final int ROWS_FOR_PROFILE_FROM_DATE = 4;  // from: yyyy-MM-dd  (sqlite: YYYY-MM-DD HH:MM:SS)
	private static final int ROWS_FOR_PROFILE_FROM_TO_DATE = 5; // from: yyyy-MM-dd  and to: yyyy-MM-dd (sqlite: YYYY-MM-DD HH:MM:SS)
	private static final int ROWS_FOR_PROFILE_IN_DAY = 6; // day:  yyyy-MM-dd (sqlite: YYYY-MM-DD HH:MM:SS)
	private static final int ROWS_FOR_PROFILE_IN_WEEK = 7; // week:  yyyy-MM-dd -> start day for week and end day for week (sqlite: YYYY-MM-DD HH:MM:SS)
	private static final int ROWS_FOR_PROFILE_IN_MONTH = 8; // month:  yyyy-MM-dd -> start day for month and end day for month (sqlite: YYYY-MM-DD HH:MM:SS)
	private static final int ROWS_FOR_PROFILE_NOT_SYNCED = 9; 
	private static final int ROW_INSERT_OR_UPDATE = 10; // used only to insert/update row, in other cases throws Exception
	private static final int ROW_FOR_PROFILE_AND_ACCESS_DATE = 11; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'learning_stats' represents a request for all items
	// and 'learning_stats/[rowId]' represents a single row,
	// and 'learning_stats/profile/[profileId]' represents a request for subset of rows for given profile 
	// and 'learning_stats/profile/[profileId]/from_date/[yyyy-MM-dd]' represents subset of rows for given profile newer then from_date
	// and 'learning_stats/profile/[profileId]/from_date/[yyyy-MM-dd]/to_date/[yyyy-MM-dd]' represents subset of rows for given profile between to dates from and to
	// and 'learning_stats/profile/[profileId]/in_day/[yyyy-MM-dd] represents rows for given profile in given day 
	// and 'learning_stats/profile/[profileId]/in_week/[yyyy-MM-dd] represents rows for given profile in given week
	// and 'learning_stats/profile/[profileId]/in_month/[yyyy-MM-dd] represents rows for given profile in given month
	// and 'learning_stats/profile/[profileId]/not_synced represents rows for given profile that haven't been synced yet (not_synced column set to 1)
	// and 'learning_stats/profile/[profileId]/access_date/[yyyy-MM-dd hh:mm:ss] row for given profile id and access date 
	static { 
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#", ROWS_FOR_PROFILE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/from_date/#", ROWS_FOR_PROFILE_FROM_DATE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/from_date/#/to_date/#", ROWS_FOR_PROFILE_FROM_TO_DATE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/in_day/#", ROWS_FOR_PROFILE_IN_DAY);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/in_week/#", ROWS_FOR_PROFILE_IN_WEEK);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/in_month/#", ROWS_FOR_PROFILE_IN_MONTH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/not_synced", ROWS_FOR_PROFILE_NOT_SYNCED);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/insert_or_update", ROW_INSERT_OR_UPDATE);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/profile/#/access_date/*", ROW_FOR_PROFILE_AND_ACCESS_DATE);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database.
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given profile) 
	// and a single row. 
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.learning_stats"; 
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.learning_stats"; 

	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArg) {
		
		// Open a read/write database to support the transaction.
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// Limit the deletion to specified row or subset of rows based on URI. 
		switch(uriMatcher.match(uri))
		{
			case SINGLE_ROW: { 
				String rowID = uri.getPathSegments().get(1); 
				selection = LearningStatsTable.COLUMN_LEARNING_STATS_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE: { 
				String profileID = uri.getPathSegments().get(2); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_FROM_DATE: { 
				String profileID = uri.getPathSegments().get(2); 
				String fromDate = uri.getPathSegments().get(4); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " >= datetime('" + fromDate + "')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_FROM_TO_DATE:  { 
				String profileID = uri.getPathSegments().get(2); 
				String fromDate = uri.getPathSegments().get(4); 
				String toDate = uri.getPathSegments().get(6); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " >= datetime('" + fromDate + "')"
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " <= datetime('" + toDate + "')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
								+ selection + ")" : "");
				break;
			}
			case ROWS_FOR_PROFILE_IN_DAY:  { 
				String profileID = uri.getPathSegments().get(2); 
				String dayDate = uri.getPathSegments().get(4); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") = date('" + dayDate + "')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_IN_WEEK: { 
				String profileID = uri.getPathSegments().get(2); 
				String inWeekDate = uri.getPathSegments().get(4); 
				try {
				   selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
							+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") >= date('" + firstDayOfWeek(inWeekDate) + "')"
							+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") <= date('" + lastDayOfWeek(inWeekDate) + "')"
							+ (!TextUtils.isEmpty(selection) ? " AND ("
							+ selection + ")" : "");
				} catch (ParseException e) {
					e.printStackTrace();
					return 0; 
				}
				break; 
			}
			case ROWS_FOR_PROFILE_IN_MONTH: { 
				String profileID = uri.getPathSegments().get(2); 
				String inMonthDate = uri.getPathSegments().get(4); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") >="
						+ 		" date('" + inMonthDate  + "','start of month')"
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") <="
						+ 		" date('" + inMonthDate  + "','start of month','+1 month','-1 day')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
								+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_NOT_SYNCED: { 
				String profileID = uri.getPathSegments().get(2); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearningStatsTable.COLUMN_NOT_SYNCED + "= 1" 
						+ (!TextUtils.isEmpty(selection) ? " AND (" 
						+ selection + ")" : ""); 
				break;
			}
			case ROW_FOR_PROFILE_AND_ACCESS_DATE: { 
				String profileID = uri.getPathSegments().get(2); 
				String accessDate = uri.getPathSegments().get(4);
				Log.d(LearningStatsProvider.class.getName(), "Uri access date is: " + accessDate); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND datetime(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") =" 
						+ 		" datetime('" + accessDate + "')" 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			}
			case ROW_INSERT_OR_UPDATE:  
				throw new IllegalArgumentException("Unsupported URI for deletion: " + uri); 
			default: break; 
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1".
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion. 
		int deleteCount = db.delete(LearningStatsTable.TABLE_LEARNING_STATS,
									selection, selectionArg);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return deleteCount;
	}
	
	private String firstDayOfWeek(String day) throws ParseException { 
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
		Date dayDate = sdf.parse(day);
		
		// set the date
	    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
	    cal.setTime(dayDate); 
	    
	    // "calculate" the start date of the week
	    cal.add(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK));
	    
		return sdf.format(cal.getTime());
	}
	
	private String lastDayOfWeek(String day) throws ParseException { 
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH); 
		Date dayDate = sdf.parse(day); 
		
		// set the date 
		Calendar cal = Calendar.getInstance(Locale.ENGLISH); 
		cal.setTime(dayDate); 
		
		// "calculate" the end date of the week
		 cal.add(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek() - cal.get(Calendar.DAY_OF_WEEK));
		 cal.add(Calendar.DAY_OF_YEAR, 6); 
		 
		 return sdf.format(cal.getTime()); 
	}
	
	/**
	 * This method is used to return correct MIME type depending on the query type:
	 * single row, all rows, subset of rows for given profile/date range
	 */
	@Override
	public synchronized String getType(Uri uri) {
		
		// For a given query's Content URI we return suitable MIME type. 
		switch( uriMatcher.match(uri) ) { 
		
			case SINGLE_ROW: 
			case ROW_FOR_PROFILE_AND_ACCESS_DATE:
			case ROW_INSERT_OR_UPDATE:
				return CONTENT_ITEM_MIME_TYPE; 
			case ALLROWS: 
			case ROWS_FOR_PROFILE:
			case ROWS_FOR_PROFILE_FROM_DATE:
			case ROWS_FOR_PROFILE_FROM_TO_DATE:
			case ROWS_FOR_PROFILE_IN_DAY: 
			case ROWS_FOR_PROFILE_IN_WEEK: 
			case ROWS_FOR_PROFILE_IN_MONTH: 
			case ROWS_FOR_PROFILE_NOT_SYNCED: 
				return CONTENT_MIME_TYPE; 
			default: 
				throw new IllegalArgumentException("Unsupported URI: " + uri); 
		}
	}
	
	/**
	 * Transaction method used to insert a new row into database (represented by Content Values)
	 */
	@Override
	public synchronized Uri insert(Uri uri, ContentValues values) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// To add empty rows to your database by passing in an empty 
		// ContentValues object, you must use the null column hack
		// parameter to specify the name of the column that can be set to null.
		String nullColumnHack = null; 
		
		long id = -1; 
		// checking whether Content URI address is suitable 
		switch( uriMatcher.match(uri)) {
		
			case ALLROWS: {
				// insert the values into the table 
				id = db.insert(LearningStatsTable.TABLE_LEARNING_STATS,
							   nullColumnHack, values); 
				break; 
			}
			case ROW_INSERT_OR_UPDATE: { 
				id = insertOrUpdate(db, nullColumnHack, values); 
				break;
			}
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		if( id > -1 )
		{
			// construct and return the URI of the newly inserted row. 
			Uri insertedItemUri = ContentUris.withAppendedId(CONTENT_URI, id);
			
			// notify any observers of the change in the data set. 
			getContext().getContentResolver().notifyChange(insertedItemUri, null); 
			
			return insertedItemUri; 
		} else 
			return null;
	}
	
	/**
	 * Helper method used to do insert or update operation on learning statistics table 
	 * depending on whether it already contains or not corresponding row.
	 */
	private long insertOrUpdate(SQLiteDatabase db, String nullColumnHack, ContentValues values) throws SQLException { 
		
		long id = -1; 
		// insert new row or if already exists update it 
		// there is unique key on (profile_id, access_date) columns
		try { 
			id = db.insertOrThrow(LearningStatsTable.TABLE_LEARNING_STATS, nullColumnHack, values);
		} catch(SQLiteConstraintException e) { 
			Log.d(LearningStatsProvider.class.getName(), "Updating learning stats row...");
			// row with such unique index already exists in the table, update it 
			Integer profileId = values.getAsInteger(LearningStatsTable.COLUMN_PROFILE_ID);
			String accessDate = values.getAsString(LearningStatsTable.COLUMN_ACCESS_DATE);
			Uri updateUri = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(BASE_PATH)
					.appendPath("profile").appendPath(String.valueOf(profileId))
					.appendPath("access_date").appendPath(accessDate).build(); 
					
			Log.d(LearningStatsProvider.class.getName(), "Update URI: " + updateUri); 
			int updateCount = update(updateUri, values, null, null); // update ROW_FOR_PROFILE_AND_ACCESS_DATE
			if(updateCount == 0) { 
				Log.d(LearningStatsProvider.class.getName(), "Error while updating learning stats row.");
				throw e; 
			} else { // if row successfully updated than query for learning_stats_id of updated row
				Log.d(LearningStatsProvider.class.getName(), "Querying learning statistics id..."); 
				Cursor cursor = query(updateUri, new String[] { LearningStatsTable.COLUMN_LEARNING_STATS_ID }, null, null, null); 
				
				if(cursor != null && cursor.getCount() == 1) { 
					cursor.moveToFirst(); 
					id = cursor.getInt(cursor.getColumnIndexOrThrow(LearningStatsTable.COLUMN_LEARNING_STATS_ID));
				} else { 
					Log.d(LearningStatsProvider.class.getName(), "Error while selecting learning statistics id."); 
					throw e; 
				}
			}
		}
		return id; 
	}
	
	@Override
	public synchronized boolean onCreate() {
		// creating instance of SQLiteOpenHelper that 
		// effectively defer creating and opening database 
		// until it's required. 
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		// returns true if the provider was successfully loaded 
		return true;
	}
	
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection,
									String[] selectionArgs, String sortOrder) {
		
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
			case SINGLE_ROW: {
				
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS);
				// getting rowID for current query and setting where clause 
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_LEARNING_STATS_ID + "=" + rowID); 
				break; 
			}
			case ROWS_FOR_PROFILE:{
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID); 
				break; 
			}
			case ROWS_FOR_PROFILE_FROM_DATE: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID and fromDate for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				String fromDate = uri.getPathSegments().get(4);
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " >= datetime('" + fromDate + "')" );
				break; 
			}
			case ROWS_FOR_PROFILE_FROM_TO_DATE: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID and fromDate/toDate for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String fromDate = uri.getPathSegments().get(4); 
				String toDate = uri.getPathSegments().get(6);
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " >= datetime('" + fromDate + "')"
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " <= datetime('" + toDate + "')" );
				break;
			}
			case ROWS_FOR_PROFILE_IN_DAY: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID and dayDate for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				String dayDate = uri.getPathSegments().get(4); 
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") = date('" + dayDate + "')" ); 
				break; 
			}
			case ROWS_FOR_PROFILE_IN_WEEK: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID and inWeekDate for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String inWeekDate = uri.getPathSegments().get(4); 
				try {
					queryBuilder.appendWhere( LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
							+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") >= date('" + firstDayOfWeek(inWeekDate) + "')"
							+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") <= date('" + lastDayOfWeek(inWeekDate) + "')" );
				} catch (ParseException e) {
					e.printStackTrace();
					return null; 
				}
				break;
			}
			case ROWS_FOR_PROFILE_IN_MONTH: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID and inMonthDate for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String inMonthDate = uri.getPathSegments().get(4); 
				queryBuilder.appendWhere( LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") >="
						+ 		" date('" + inMonthDate  + "','start of month')"
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") <="
						+ 		" date('" + inMonthDate  + "','start of month','+1 month','-1 day')" ); 
				break; 
			}
			case ROWS_FOR_PROFILE_NOT_SYNCED: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS);
				// getting profileID for current query and setting where clause 
				String profileID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_PROFILE_ID + "="+ profileID
						+ " AND " + LearningStatsTable.COLUMN_NOT_SYNCED + "= 1"); 
				break;
			}
			case ALLROWS: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				break; 
			}
			case ROW_FOR_PROFILE_AND_ACCESS_DATE: { 
				queryBuilder.setTables(LearningStatsTable.TABLE_LEARNING_STATS); 
				// getting profileID and accessDate for current query and setting where clause
				String profileID = uri.getPathSegments().get(2); 
				String accessDate = uri.getPathSegments().get(4);
				Log.d(LearningStatsProvider.class.getName(), "Uri access date is: " + accessDate);
				queryBuilder.appendWhere(LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
										+ " AND datetime(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") ="
										+ 		" datetime('" + accessDate + "')");
				break; 
			}
			case ROW_INSERT_OR_UPDATE:
			default: 
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder);
		
		// return the result set Cursor 
		return cursor;
	}
	
	
	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// Modify selection argument to indicate updated row or rows
		switch( uriMatcher.match(uri) ) 
		{ 
			case SINGLE_ROW: { 
				String rowID = uri.getPathSegments().get(1); 
				selection = LearningStatsTable.COLUMN_LEARNING_STATS_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE: { 
				String profileID = uri.getPathSegments().get(2); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_FROM_DATE: { 
				String profileID = uri.getPathSegments().get(2); 
				String fromDate = uri.getPathSegments().get(4); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " >= datetime('" + fromDate + "')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_FROM_TO_DATE:  { 
				String profileID = uri.getPathSegments().get(2); 
				String fromDate = uri.getPathSegments().get(4); 
				String toDate = uri.getPathSegments().get(6); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " >= datetime('" + fromDate + "')"
						+ " AND " + LearningStatsTable.COLUMN_ACCESS_DATE + " <= datetime('" + toDate + "')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			}
			case ROWS_FOR_PROFILE_IN_DAY:  { 
				String profileID = uri.getPathSegments().get(2); 
				String dayDate = uri.getPathSegments().get(4); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") = date('" + dayDate + "')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_IN_WEEK: { 
				String profileID = uri.getPathSegments().get(2); 
				String inWeekDate = uri.getPathSegments().get(4); 
				try {
				   selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
							+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") >= date('" + firstDayOfWeek(inWeekDate) + "')"
							+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") <= date('" + lastDayOfWeek(inWeekDate) + "')"
							+ (!TextUtils.isEmpty(selection) ? " AND ("
							+ selection + ")" : "");
				} catch (ParseException e) {
					e.printStackTrace();
					return 0; 
				}
				break; 
			}
			case ROWS_FOR_PROFILE_IN_MONTH: { 
				String profileID = uri.getPathSegments().get(2); 
				String inMonthDate = uri.getPathSegments().get(4); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID 
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") >="
						+ 		" date('" + inMonthDate  + "','start of month')"
						+ " AND date(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") <="
						+ 		" date('" + inMonthDate  + "','start of month','+1 month','-1 day')"
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			}
			case ROWS_FOR_PROFILE_NOT_SYNCED: { 
				String profileID = uri.getPathSegments().get(2); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND " + LearningStatsTable.COLUMN_NOT_SYNCED + "= 1" 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break;
			}
			case ROW_FOR_PROFILE_AND_ACCESS_DATE: { 
				String profileID = uri.getPathSegments().get(2); 
				String accessDate = uri.getPathSegments().get(4);
				Log.d(LearningStatsProvider.class.getName(), "Uri access date is: " + accessDate); 
				selection = LearningStatsTable.COLUMN_PROFILE_ID + "=" + profileID
						+ " AND datetime(" + LearningStatsTable.COLUMN_ACCESS_DATE + ") =" 
						+ 		" datetime('" + accessDate + "')" 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			}
			case ROW_INSERT_OR_UPDATE: { 
				throw new IllegalArgumentException(); 
			}
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(LearningStatsTable.TABLE_LEARNING_STATS,
									values, selection, selectionArgs);
		
		Log.d(LearningStatsProvider.class.getName(), "Update count: " + updateCount + ", selection: " + selection); 
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount;
	} 

	public static class LearningStatsTable { 
		
		// Database Table 
		public static final String TABLE_LEARNING_STATS = "learningStatsTable"; 
		public static final String COLUMN_LEARNING_STATS_ID = "_id"; // INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
		public static final String COLUMN_PROFILE_ID = "profileId"; // INTEGER NOT NULL
		public static final String COLUMN_ACCESS_DATE = "accessDate"; // TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
		public static final String COLUMN_GOOD_ANSWERS = "goodAnswers"; // INTEGER NOT NULL
		public static final String COLUMN_BAD_ANSWERS = "badAnswers"; // INTEGER NO NULL
		public static final String COLUMN_NOT_SYNCED = "notSynced"; // BOOLEAN NOT NULL (NUMERIC, 0 - synced, 1 - not_synced)
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_LEARNING_STATS 
				+ " (" 
				+ COLUMN_LEARNING_STATS_ID + " integer primary key autoincrement not null, "
				+ COLUMN_PROFILE_ID + " integer not null default 0, " // 0 - Anonymous user 
				+ COLUMN_ACCESS_DATE + " datetime not null default current_timestamp, "
				+ COLUMN_GOOD_ANSWERS + " integer not null, "
				+ COLUMN_BAD_ANSWERS + " integer not null, "
				+ COLUMN_NOT_SYNCED + " boolean not null, "
				+ " unique(" + COLUMN_PROFILE_ID + ", " + COLUMN_ACCESS_DATE 
				+ " ) on conflict abort, "
				+ " foreign key (" + COLUMN_PROFILE_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete cascade "
				+ ")";
		
		// TRIGGERS: 
		// 1) insert trigger on learning_stats table, checks if corresponding profile exists
		private static final String PROFILE_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID + " "
				+ "before insert on " + TABLE_LEARNING_STATS + " "
				+ "for each row begin "
						+ "select raise(rollback, 'insert on table " + TABLE_LEARNING_STATS
										+ " violates foreign key constraint') "
						+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " // 0 - Anonymous user 
								+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
								+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " = new." + COLUMN_PROFILE_ID + ") is null;"
					+ " end;";
		
		// 2) update trigger on learning_stats table, checks if new profile exists
		private static final String PROFILE_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID + " "
				+ "before update on " + TABLE_LEARNING_STATS + " "
				+ "for each row begin "
						+ "select raise(rollback, 'update on table " + TABLE_LEARNING_STATS
										+ " violates foreign key constraint') "
						+ "where new." + COLUMN_PROFILE_ID + "!= 0 AND (select " 
								+ ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
								+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
								+ " = new." + COLUMN_PROFILE_ID + ") is null;"
					+ " end;";
		
		// 3) delete trigger on profile table, cascade deletes corresponding learning_stats
		private static final String PROFILE_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
						+ "delete from " + TABLE_LEARNING_STATS
							+ " where " + COLUMN_PROFILE_ID
							+ " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ";"
				+ " end;"; 
		
		// 4) update trigger on profile table, cascade updates corresponding learning_stats
		private static final String PROFILE_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
						+ "update " + TABLE_LEARNING_STATS + " set " + COLUMN_PROFILE_ID 
						+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ " where " + COLUMN_PROFILE_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database) 
		{
			// LearningStats table creation in database (with additional triggers) 
			database.execSQL(TABLE_CREATE); 
			database.execSQL(PROFILE_INSERT_TRIGGER_CREATE); 
			database.execSQL(PROFILE_UPDATE_TRIGGER_CREATE); 
			database.execSQL(PROFILE_DELETE_TRIGGER_CREATE); 
			database.execSQL(PROFILE_PARENT_UPDATE_TRIGGER_CREATE); 
		}
		
		// called when there is a database version mismatch meaning that the version 
		// of the database on disk needs to be upgraded to the current version. 
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) 
		{ 
			// Log the version upgrade 
			Log.w(LearningStatsProvider.class.getName(), 
					"Upgrading database LearningStats table from version " + oldVersion 
					+ " to " + newVersion + ", which will destroy all old data.");
			
			// Upgrading the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion 
			// and newVersion values. 
			
			// Upgrade database by adding new version of LearningStats table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_LEARNING_STATS);
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_LEARNING_STATS + "_" + COLUMN_PROFILE_ID); 
			
			onCreate(database); 
		}
	}
}
