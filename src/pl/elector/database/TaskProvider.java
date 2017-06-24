/**
 * @date 16.09.2014
 */
package pl.elector.database;

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
public class TaskProvider extends ContentProvider {
	
	// defining ContentProvider's URI address
	private static final String AUTHORITY = "pl.elector.provider.TaskProvider";
	private static final String BASE_PATH = "tasks"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
	
	// defining a UriMatcher to differentiate between different URI requests:
	// for all elements, subset of rows for given task category or given author 
	// and a single row.
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2; 
	private static final int ROWS_FOR_CATEGORY = 3; 
	private static final int ROWS_FOR_AUTHOR = 4; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'tasks' represents a request for all task items 
	// and 'tasks/[rowId]' represents a single row, 
	// and 'tasks/category/[catId]' represents request for all tasks in given category,
	// and 'tasks/author/[authorId]' represents request for all tasks for given author
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH); 
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/category/#", ROWS_FOR_CATEGORY);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/author/#", ROWS_FOR_AUTHOR);
	}
	
	// reference to SQLiteOpenHelper class instance 
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given category or author)
	// and a single row 
	public static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.tasks";
	public static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.tasks"; 

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single task item or set of tasks for given author or category 
	 * or all rows depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI, limit the deletion to specified row
		// else if this is a category URI, limit the deletion to specified category rows, 
		// else if this is an author URI, limit the deletion to specified author rows
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = TaskTable.COLUMN_TASK_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break;
			case ROWS_FOR_CATEGORY: 
				String categoryID = uri.getPathSegments().get(2); 
				selection = TaskTable.COLUMN_TASK_CATEGORY_ID + "=" + categoryID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				selection = TaskTable.COLUMN_AUTHOR_ID + "=" + authorID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			default: 
				break;
		}
		
		// To return the number of deleted items, you must specify
		// a where clause. To delete all rows and return a value, pass in "1".
		if(selection == null)
			selection = "1";
		
		// Execute the deletion. 
		int deleteCount = db.delete(TaskTable.TABLE_TASK,
									selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type, depending 
	 * on the query type: set of rows (including all rows) or a single row.
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For given query's Content URI we return suitable MIME type.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW:
				return CONTENT_ITEM_MIME_TYPE; 
			case ROWS_FOR_CATEGORY:
			case ROWS_FOR_AUTHOR:
			case ALLROWS: 
				return CONTENT_MIME_TYPE; 
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
		
		long id = -1; 
		//checking whether Content URI address is suitable
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: 
				// insert the values into the table
				id = db.insert(TaskTable.TABLE_TASK, nullColumnHack, values);
				break; 
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri); 
		}
		
		if(id > -1)
		{
			// construct and return the URI of the newly inserted row. 
			Uri insertedItemUri = ContentUris.withAppendedId(CONTENT_URI, id); 
			
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
		// creating instance of SQLiteOpenHelper that 
		// effectively defer creating and opening a database 
		// until it's required
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		//returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate
	 * queries for all rows, subset of rows for given category or author and a single row. 
	 * SQLite Query Builder is used as a helper object for performing row-based, category-based
	 * and author-based queries.
	 */
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
		
		// Replace this with valid SQL statements if necessary.
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method 
		// in order to simplify database query construction 
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		// Specifying the table on which to perform query 
		queryBuilder.setTables(TaskTable.TABLE_TASK); 
		
		// If this is a single row query add task id to the base query 
		// else if this is a query for subset of rows for given category add category id to the base query 
		// else if this is a query for subset of rows for given author add author id to the base query 
		switch(uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(TaskTable.COLUMN_TASK_ID + "=" + rowID); 
				break; 
			case ROWS_FOR_CATEGORY: 
				String categoryID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(TaskTable.COLUMN_TASK_CATEGORY_ID + "=" + categoryID); 
				break; 
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(TaskTable.COLUMN_AUTHOR_ID + "=" + authorID); 
				break; 
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
	public synchronized int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is an update of single row, modify selection argument
		// to indicate that row, else if this is an update of a set of rows
		// for given category or author modify selection argument to indicate that rows.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = TaskTable.COLUMN_TASK_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			case ROWS_FOR_CATEGORY: 
				String categoryID = uri.getPathSegments().get(2); 
				selection = TaskTable.COLUMN_TASK_CATEGORY_ID + "=" + categoryID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				selection = TaskTable.COLUMN_AUTHOR_ID + "=" + authorID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(TaskTable.TABLE_TASK, values, selection, selectionArgs); 
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount; 
		
	}
	
	public static class TaskTable {
		
		// Database Table 
		public static final String TABLE_TASK = "taskTable"; 
		public static final String COLUMN_TASK_ID = "_id"; //primary key
		public static final String COLUMN_TASK_CATEGORY_ID = "taskCategoryId"; //foreign key
		public static final String COLUMN_AUTHOR_ID = "authorId"; //foreign key
		public static final String COLUMN_TASK_TEXT = "taskText"; 
		public static final String COLUMN_CREATION_DATE = "creationDate"; 
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_TASK
				+ " ("
				+ COLUMN_TASK_ID + " integer primary key autoincrement, "
				+ COLUMN_TASK_CATEGORY_ID + " integer not null, "
				+ COLUMN_AUTHOR_ID + " integer not null default 0, "
				+ COLUMN_TASK_TEXT + " text not null, "
				+ COLUMN_CREATION_DATE + "numeric not null, "
				+ " foreign key(" + COLUMN_TASK_CATEGORY_ID + ") references "
				+ TaskCategoryProvider.TaskCategoryTable.TABLE_TASK_CATEGORY
				+ "(" + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID + ")"
				+ " on update cascade on delete cascade "
				+ " foreign key(" + COLUMN_AUTHOR_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE 
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete set default  "
				+ ");"; 
		
		// TRIGGERS: 
		// 1) insert trigger on task table, checking author profile exists 
		private static final String AUTHOR_INSERT_TRIGGER_CREATE = "create trigger fki_"
				 + TABLE_TASK + "_" + COLUMN_AUTHOR_ID + " "
				 + "before insert on " + TABLE_TASK + " "
				 + "for each row begin "
				 	+ "select raise(rollback, 'insert on table " + TABLE_TASK
				 					+ " violates foreign key constraint') "
				 	+ "where new." + COLUMN_AUTHOR_ID + " != 0 AND "
				 			+ "(select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
				 			+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
				 			+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID 
				 			+ " = new." + COLUMN_AUTHOR_ID + ") is null;"
			     + " end;"; 
				
		// 2) insert trigger on task table, checking task category exists
		private static final String TASK_CATEGORY_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID + " "
				+ "before insert on " + TABLE_TASK + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_TASK 
									+ " violates foreign key constraint') "
					+ "where (select " + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID 
								+ " from " + TaskCategoryProvider.TaskCategoryTable.TABLE_TASK_CATEGORY 
								+ " where " + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID 
								+ " = new." + COLUMN_TASK_CATEGORY_ID + ") is null;"
				+ " end;"; 
				
		// 3) update trigger on task table, checking new author profile exists
		private static final String AUTHOR_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_TASK + "_" + COLUMN_AUTHOR_ID + " "
				+ "before update on " + TABLE_TASK + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_TASK 
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_AUTHOR_ID + " != 0 AND "
							+ "(select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE  
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_AUTHOR_ID + ") is null;"
				+ " end;";
				
		// 4) update trigger on task table, checking new task category exists
		private static final String TASK_CATEGORY_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID + " "
				+ "before update on " + TABLE_TASK + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_TASK 
									+ " violates foreign key constraint') "
					+ "where (select " + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID 
							+ " from " + TaskCategoryProvider.TaskCategoryTable.TABLE_TASK_CATEGORY
							+ " where " + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID
							+ " = new." + COLUMN_TASK_CATEGORY_ID + ") is null;"
				+ " end;";
				
		// 5) delete trigger on profile table, set rows in task table to default value 0
		private static final String AUTHOR_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_TASK + "_" + COLUMN_AUTHOR_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "update " + TABLE_TASK + " set " + COLUMN_AUTHOR_ID + " = 0 "
					+ "where " + COLUMN_AUTHOR_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
					+ "; "
				+ "end;";
		
		// 6) delete trigger on task category table, cascade delete rows in task table
		private static final String TASK_CATEGORY_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID + " "
				+ "before delete on " + TaskCategoryProvider.TaskCategoryTable.TABLE_TASK_CATEGORY + " "
				+ "for each row begin "
					+ "delete from " + TABLE_TASK 
						  + " where " + COLUMN_TASK_CATEGORY_ID 
					      + " = old." + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID + "; "
				+ " end;";
				
		// 7) update trigger on profile table, cascade update rows in task table
		private static final String AUTHOR_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_TASK + "_" + COLUMN_AUTHOR_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " " 
				+ "for each row begin "
					+ "update " + TABLE_TASK + " set " + COLUMN_AUTHOR_ID 
						+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ " where " + COLUMN_AUTHOR_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ "; "
				+ "end;";
				
		// 8) update trigger on task category table, cascade update rows in task table
		private static final String TASK_CATEGORY_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID + " "
				+ "after update on " + TaskCategoryProvider.TaskCategoryTable.TABLE_TASK_CATEGORY + " "
				+ "for each row begin "
					+ "update " + TABLE_TASK + " set " + COLUMN_TASK_CATEGORY_ID 
						+ " = new." + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID
						+ " where " + COLUMN_TASK_CATEGORY_ID + " = old." + TaskCategoryProvider.TaskCategoryTable.COLUMN_ID
						+ "; "
				+ "end;";
		
		// called when no database exists in disk and the SQLiteOpenHelper 
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database)
		{
			Log.w(TaskTable.class.getName(), TABLE_CREATE); 
			
			// Task table creation in database (with additional triggers) 
			database.execSQL(TABLE_CREATE); 
			database.execSQL(AUTHOR_INSERT_TRIGGER_CREATE);
			database.execSQL(TASK_CATEGORY_INSERT_TRIGGER_CREATE);
			database.execSQL(AUTHOR_UPDATE_TRIGGER_CREATE);
			database.execSQL(TASK_CATEGORY_UPDATE_TRIGGER_CREATE);
			database.execSQL(AUTHOR_DELETE_TRIGGER_CREATE);
			database.execSQL(TASK_CATEGORY_DELETE_TRIGGER_CREATE);
			database.execSQL(AUTHOR_PARENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(TASK_CATEGORY_PARENT_UPDATE_TRIGGER_CREATE);
		}
		
		// called when there is a database version mismatch meaning that the version 
		// of the database on disk needs to be upgraded to the current version.
		public static void onUpgrade(SQLiteDatabase database, int oldVersion, 
										int newVersion)
		{
			// Log the version upgrade 
			Log.w(TaskTable.class.getName(), 
					"Upgrading database Task table from version " + oldVersion + " to "
					 + newVersion + ", which wil destroy all old data."); 
			
			// Upgrade the existing database to conform to the new version. 
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of Task table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_TASK + "_" + COLUMN_AUTHOR_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_TASK + "_" + COLUMN_AUTHOR_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_TASK + "_" + COLUMN_AUTHOR_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_TASK + "_" + COLUMN_AUTHOR_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_TASK + "_" + COLUMN_TASK_CATEGORY_ID); 
			onCreate(database); 
			
		}
	}

}
