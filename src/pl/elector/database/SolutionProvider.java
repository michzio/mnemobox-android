/**
 * @date 17.09.2014
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
public class SolutionProvider extends ContentProvider {
	
	// defining ContentProvider's URI address 
	private static final String AUTHORITY = "pl.elector.provider.SolutionProvider";
	private static final String BASE_PATH = "solutions"; 
	
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH); 

	// defining a UriMatcher to differentiate between different URI requests:
	// for all elements, subset of rows for given task or author 
	// and a single row.
	private static final int ALLROWS = 1; 
	private static final int SINGLE_ROW = 2;
	private static final int ROWS_FOR_TASK = 3; 
	private static final int ROWS_FOR_AUTHOR = 4; 
	
	private static final UriMatcher uriMatcher; 
	
	// populating the UriMatcher object, where an URI ending 
	// in 'solutions' represents a request for all solution items
	// and 'solutions/[rowId]' represents a single row, 
	// and 'solutions/task/[taskId]' represents request for all solutions for given task
	// and 'solutions/author/[authorId]' represents request for all solutions for given author
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, BASE_PATH, ALLROWS);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/#", SINGLE_ROW);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/task/#", ROWS_FOR_TASK);
		uriMatcher.addURI(AUTHORITY, BASE_PATH + "/author/#", ROWS_FOR_AUTHOR); 
	}
	
	// reference to SQLiteOpenHelper class instance
	// used to construct the underlying database. 
	private DatabaseSQLiteOpenHelper databaseHelper; 
	
	// defining the MIME types for all rows (including rows for given task or author)
	// and a single row
	private static final String CONTENT_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.elector.solutions";
	private static final String CONTENT_ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.elector.solutions";
	
	
	/* (non-Javadoc)
	 * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
	 * This method deletes single solution item or set of solutions for given task or author
	 * or all rows depending on URI address. 
	 */
	@Override
	public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
		
		// Open a read/write database to support the transaction. 
		SQLiteDatabase db = databaseHelper.getWritableDatabase(); 
		
		// If this is a row URI limit the deletion to specified row
		// else if this is a task URI limit the deletion to specified task rows
		// else if this is a author URI limit the deletion to specified author rows
		switch( uriMatcher.match(uri) )
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = SolutionTable.COLUMN_SOLUTION_ID + "=" + rowID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			case ROWS_FOR_TASK: 
				String taskID = uri.getPathSegments().get(2); 
				selection = SolutionTable.COLUMN_TASK_ID + "=" + taskID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break; 
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				selection = SolutionTable.COLUMN_AUTHOR_ID + "=" + authorID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			default: 
				break;
		}
		
		// To return the number of deleted items, you must specify 
		// a where clause. To delete all rows and return a value, pass in "1"
		if(selection == null)
			selection = "1"; 
		
		// Execute the deletion
		int deleteCount = db.delete(SolutionTable.TABLE_SOLUTION, selection, selectionArgs);
		
		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);
		
		return deleteCount;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 * This method is used to return correct MIME type, depending on 
	 * the query type: set of rows (including all rows) or a single row. 
	 */
	@Override
	public synchronized String getType(Uri uri) {
		// For given query's Content URI we return suitable MIME type.
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				return CONTENT_ITEM_MIME_TYPE; 
			case ROWS_FOR_TASK: 
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
		// ContentValues object, you must use the null column hack 
		// parameter to specify the name of the column that can be set to null.
		String nullColumnHack = null; 
		
		long id = -1; 
		// checking whether Content URI address is suitable
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: 
				// insert the values into the table 
				id = db.insert(SolutionTable.TABLE_SOLUTION, nullColumnHack, values);
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
		// effectively defer creating and opening database
		// until it's required
		databaseHelper = new DatabaseSQLiteOpenHelper(getContext()); 
		// returns true if the provider was successfully loaded
		return true;
	}

	/* (non-Javadoc)
	 * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
	 * This method enables you to perform queries on the underlying data source 
	 * (SQLite database) using ContentProvider. UriMatcher object is used to differentiate 
	 * queries for all rows, subset of rows for given task or author and a single row. 
	 * SQLite Query Builder is used as a helper object for performing row-based, task-based
	 * and author-based queries.
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
		
		// Replace this with valid SQL statements if necessary.
		String groupBy = null; 
		String having = null; 
		
		// Using SQLiteQueryBuilder instead of query() method 
		// in order to simplify database query construction 
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder(); 
		//Specifying the table on which to perform query 
		queryBuilder.setTables(SolutionTable.TABLE_SOLUTION);
		
		// If this is a single row query add solution id to the base query
		// else if this is a query for subset of rows for given task add task id to the base query
		// else if this is a query for subset of rows for given author add author id to the base query 
		switch( uriMatcher.match(uri))
		{
			case ALLROWS: break; 
			case SINGLE_ROW: 
				String rowID  = uri.getPathSegments().get(1); 
				queryBuilder.appendWhere(SolutionTable.COLUMN_SOLUTION_ID + "=" + rowID); 
				break; 
			case ROWS_FOR_TASK: 
				String taskID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(SolutionTable.COLUMN_TASK_ID + "=" + taskID); 
				break; 
			case ROWS_FOR_AUTHOR: 
				String authorID = uri.getPathSegments().get(2); 
				queryBuilder.appendWhere(SolutionTable.COLUMN_AUTHOR_ID + "=" + authorID); 
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
		
		// If this is an update of single row modify selection argument to indicate 
		// that row, else if this is an update of a set of rows for given task or 
		// author modify selection argument to indicate that rows. 
		switch( uriMatcher.match(uri))
		{
			case SINGLE_ROW: 
				String rowID = uri.getPathSegments().get(1); 
				selection = SolutionTable.COLUMN_SOLUTION_ID + "=" + rowID 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_TASK: 
				String taskID = uri.getPathSegments().get(2); 
				selection = SolutionTable.COLUMN_TASK_ID + "=" + taskID
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : ""); 
				break; 
			case ROWS_FOR_AUTHOR:  
				String authorID = uri.getPathSegments().get(2); 
				selection = SolutionTable.COLUMN_AUTHOR_ID + "=" + authorID 
						+ (!TextUtils.isEmpty(selection) ? " AND ("
						+ selection + ")" : "");
				break;
			default: break; 
		}
		
		// Perform the update. 
		int updateCount = db.update(SolutionTable.TABLE_SOLUTION, values, selection, selectionArgs);
		
		// Notify any observers of the change in the data set. 
		getContext().getContentResolver().notifyChange(uri, null); 
		
		return updateCount;
	}
	
	public static class SolutionTable {
		
		// Database Table 
		public static final String TABLE_SOLUTION = "solutionTable"; 
		public static final String COLUMN_SOLUTION_ID = "_id"; // primary key 
		public static final String COLUMN_TASK_ID = "taskId"; // foreign key 
		public static final String COLUMN_AUTHOR_ID = "authorId"; // foreign key
		public static final String COLUMN_SOLUTION_CONTENT_ID = "solutionContentId"; // foreign key
		public static final String COLUMN_SOLUTION_TEASER = "solutionTeaser"; 
		public static final String COLUMN_CREATION_DATE = "creationDate"; 
		
		// Database Table creation SQL Statement 
		private static final String TABLE_CREATE = "create table if not exists "
				+ TABLE_SOLUTION
				+ " ("
				+ COLUMN_SOLUTION_ID + " integer primary key autoincrement, "
				+ COLUMN_TASK_ID + " integer not null, "
				+ COLUMN_AUTHOR_ID + " integer not null default 0, "
				+ COLUMN_SOLUTION_CONTENT_ID + " integer default null, "
				+ COLUMN_SOLUTION_TEASER + " text not null, "
				+ COLUMN_CREATION_DATE + " numeric not null, "
				+ " foreign key(" + COLUMN_TASK_ID + ") references " 
				+ TaskProvider.TaskTable.TABLE_TASK 
				+ "(" + TaskProvider.TaskTable.COLUMN_TASK_ID + ")"
				+ " on update cascade on delete cascade"
				+ " foreign key(" + COLUMN_AUTHOR_ID + ") references "
				+ ProfileProvider.ProfileTable.TABLE_PROFILE 
				+ "(" + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID + ")"
				+ " on update cascade on delete set default"
				+ " foreign key(" + COLUMN_SOLUTION_CONTENT_ID + ") references "
				+ SolutionContentProvider.SolutionContentTable.TABLE_SOLUTION_CONTENT
				+ "(" + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID + ")"
				+ " on update cascade on delete set default"
				+ " );";
		// TRIGGERS: 
		
		// 1) insert trigger on solution table, checks if corresponding task exists
		private static final String TASK_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_SOLUTION + "_" + COLUMN_TASK_ID + " "
				+ "before insert on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_SOLUTION
									+ " violates foreign key constraint') "
					+ "where (select " + TaskProvider.TaskTable.COLUMN_TASK_ID 
					 		 + " from " + TaskProvider.TaskTable.TABLE_TASK
					 		 + " where " + TaskProvider.TaskTable.COLUMN_TASK_ID
					 		 + " = new." + COLUMN_TASK_ID + ") is null;"
				+ " end;";
				
		// 2) update trigger on solution table, checks if new task exists
		private static final String TASK_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_SOLUTION + "_" + COLUMN_TASK_ID + " "
				+ "before update on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_SOLUTION 
									+ " violates foreign key constraint') "
					+ "where (select " + TaskProvider.TaskTable.COLUMN_TASK_ID
							+ " from " + TaskProvider.TaskTable.TABLE_TASK
							+ " where " + TaskProvider.TaskTable.COLUMN_TASK_ID
							+ " = new." + COLUMN_TASK_ID + ") is null;"
				+ " end;";
				
		// 3) delete trigger on task table, cascade deletes corresponding solutions
		private static final String TASK_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_SOLUTION + "_" + COLUMN_TASK_ID + " "
				+ "before delete on " + TaskProvider.TaskTable.TABLE_TASK + " "
				+ "for each row begin "
					+ "delete from " + TABLE_SOLUTION
						+ " where " + COLUMN_TASK_ID 
						+ " = old." + TaskProvider.TaskTable.COLUMN_TASK_ID + "; "
			    + " end;";
						
		// 4) update trigger on task table, cascade updates corresponding solutions
		private static final String TASK_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_SOLUTION + "_" + COLUMN_TASK_ID + " "
				+ "after update on " + TaskProvider.TaskTable.TABLE_TASK + " "
				+ "for each row begin "
					+ "update " + TABLE_SOLUTION + " set " + COLUMN_TASK_ID 
					+ " = new." + TaskProvider.TaskTable.COLUMN_TASK_ID
					+ " where " + COLUMN_TASK_ID + " = old." + TaskProvider.TaskTable.COLUMN_TASK_ID
					+ "; "
				+ "end;";
				
		// 5) insert trigger on solution table, checks if corresponding author exists if author id != 0
		private static final String AUTHOR_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID + " "
				+ "before insert on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_SOLUTION
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_AUTHOR_ID + " != 0 AND "
							+ "(select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_AUTHOR_ID + ") is null;"
				+ " end;"; 
		
		// 6) update trigger on solution table, checks if new author exists if author id != 0
		private static final String AUTHOR_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID + " "
				+ "before update on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_SOLUTION
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_AUTHOR_ID + " != 0 AND "
							+ "(select " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " from " + ProfileProvider.ProfileTable.TABLE_PROFILE
							+ " where " + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
							+ " = new." + COLUMN_AUTHOR_ID + ") is null;"
				+ " end;"; 
				
		// 7) delete trigger on profile table, set rows in solution table to default value 0 
		private static final String AUTHOR_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID + " "
				+ "before delete on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "update " + TABLE_SOLUTION + " set " + COLUMN_AUTHOR_ID + " = 0 "
					+ "where " + COLUMN_AUTHOR_ID  + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
					+ "; "
				+ "end;";
				
		// 8) update trigger on profile table, cascade updates rows in solution tables
		private static final String AUTHOR_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID + " "
				+ "after update on " + ProfileProvider.ProfileTable.TABLE_PROFILE + " "
				+ "for each row begin "
					+ "update " + TABLE_SOLUTION + " set " + COLUMN_AUTHOR_ID 
						+ " = new." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ " where " + COLUMN_AUTHOR_ID + " = old." + ProfileProvider.ProfileTable.COLUMN_PROFILE_ID
						+ "; "
				+ "end;";
		
		// 9) insert trigger on solution table, checks if corresponding solution content exists if solution content id is not null
		private static final String SOLUTION_CONTENT_INSERT_TRIGGER_CREATE = "create trigger fki_"
				+ TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID + " "
				+ "before insert on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "select raise(rollback, 'insert on table " + TABLE_SOLUTION
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_SOLUTION_CONTENT_ID + " is not null AND "
				    	+ "(select " + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID
				    	+ " from " + SolutionContentProvider.SolutionContentTable.TABLE_SOLUTION_CONTENT
				    	+ " where " + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID
				    	+ " = new." + COLUMN_SOLUTION_CONTENT_ID + ") is null;"
				+ " end;"; 
				
		// 10) update trigger on solution table, checks if new solution content exists if solution content id not set to null
		private static final String SOLUTION_CONTENT_UPDATE_TRIGGER_CREATE = "create trigger fku_"
				+ TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID + " "
				+ "before update on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "select raise(rollback, 'update on table " + TABLE_SOLUTION 
									+ " violates foreign key constraint') "
					+ "where new." + COLUMN_SOLUTION_CONTENT_ID + " is not null AND "
							+ "(select " + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID
							+ " from " + SolutionContentProvider.SolutionContentTable.TABLE_SOLUTION_CONTENT
							+ " where " + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID 
							+ " = new." + COLUMN_SOLUTION_CONTENT_ID + ") is null;"
				+ " end;";
				
		// 11) delete trigger on solution content table, set rows in solution table to default null 
		private static final String SOLUTION_CONTENT_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID + " "
				+ "before delete on " + SolutionContentProvider.SolutionContentTable.TABLE_SOLUTION_CONTENT + " "
				+ "for each row begin "
					+ "update " + TABLE_SOLUTION + " set " + COLUMN_SOLUTION_CONTENT_ID + " = NULL "
					+ "where " + COLUMN_SOLUTION_CONTENT_ID  + " = old." + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID
					+ "; "
				+ "end;";
				
		// 12) update trigger on solution content table, cascade updates rows in solution tables
		private static final String SOLUTION_CONTENT_PARENT_UPDATE_TRIGGER_CREATE = "create trigger fkpu_"
				+ TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID + " "
				+ "after update on " + SolutionContentProvider.SolutionContentTable.TABLE_SOLUTION_CONTENT + " "
				+ "for each row begin "
					+ "update " + TABLE_SOLUTION + " set " + COLUMN_SOLUTION_CONTENT_ID 
						+ " = new." + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID 
						+ " where " + COLUMN_SOLUTION_CONTENT_ID + " = old." + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID
						+ "; "
				+ "end;";
		
		// 13) delete trigger on solution table, cascade deletes corresponding row in solution content table 
		private static final String SOLUTION_DELETE_TRIGGER_CREATE = "create trigger fkd_"
				+ TABLE_SOLUTION + "_" + COLUMN_SOLUTION_ID 
				+ "after delete on " + TABLE_SOLUTION + " "
				+ "for each row begin "
					+ "delete from " + SolutionContentProvider.SolutionContentTable.TABLE_SOLUTION_CONTENT
						+ " where " + SolutionContentProvider.SolutionContentTable.COLUMN_SOLUTION_CONTENT_ID
						+ " = old." + COLUMN_SOLUTION_CONTENT_ID + "; "
				+ " end;";
				
		// called when no database exists in disk and the SQLiteOpenHelper
		// class needs to create a new one. 
		public static void onCreate(SQLiteDatabase database)
		{
			// Solution table creation in database (with additional triggers)
			database.execSQL(TABLE_CREATE); 
			database.execSQL(TASK_INSERT_TRIGGER_CREATE);
			database.execSQL(AUTHOR_INSERT_TRIGGER_CREATE);
			database.execSQL(SOLUTION_CONTENT_INSERT_TRIGGER_CREATE);
			database.execSQL(TASK_UPDATE_TRIGGER_CREATE);
			database.execSQL(AUTHOR_UPDATE_TRIGGER_CREATE);
			database.execSQL(SOLUTION_CONTENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(TASK_DELETE_TRIGGER_CREATE);
			database.execSQL(AUTHOR_DELETE_TRIGGER_CREATE);
			database.execSQL(SOLUTION_CONTENT_DELETE_TRIGGER_CREATE);
			database.execSQL(TASK_PARENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(AUTHOR_PARENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(SOLUTION_CONTENT_PARENT_UPDATE_TRIGGER_CREATE);
			database.execSQL(SOLUTION_DELETE_TRIGGER_CREATE);
			
		}
		
		// called when there is a database version mismatch meaning that the version
		// of the database on disk needs to be upgraded to the current version.
		public static void onUpgrade(SQLiteDatabase database, int oldVersion,
										int newVersion)
		{
			
			// Log the version upgrade 
			Log.w(SolutionTable.class.getName(),
					"Upgrading database Solution table from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data.");
			
			// Upgrade the existing database to conform to the new version.
			// Multiple previous versions can be handled by comparing oldVersion
			// and newVersion values. 
			
			// Upgrade database by adding new version of Solution table?
			database.execSQL("DROP TABLE IF EXISTS " + TABLE_SOLUTION); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_SOLUTION + "_" + COLUMN_TASK_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fki_" + TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_SOLUTION + "_" + COLUMN_TASK_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fku_" + TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_SOLUTION + "_" + COLUMN_TASK_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_SOLUTION + "_" + COLUMN_TASK_ID);
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_SOLUTION + "_" + COLUMN_AUTHOR_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkpu_" + TABLE_SOLUTION + "_" + COLUMN_SOLUTION_CONTENT_ID); 
			database.execSQL("DROP TRIGGER IF EXISTS fkd_" + TABLE_SOLUTION + "_" + COLUMN_SOLUTION_ID); 
			onCreate(database); 
		}
	}

}
