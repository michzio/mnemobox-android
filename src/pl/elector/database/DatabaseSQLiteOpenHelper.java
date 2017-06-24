package pl.elector.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseSQLiteOpenHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "database.db";
	private static final int DATABASE_VERSION = 2; 

	public DatabaseSQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	//called when no database exists in disk and the helper class
	//needs to create a new one
	@Override
	public void onCreate(SQLiteDatabase db) {
		//creating tables in database:
		WordsetCategoryProvider.WordsetCategoryTable.onCreate(db);
		WordsetProvider.WordsetTable.onCreate(db);
		WordProvider.WordTable.onCreate(db);
		SentenceProvider.SentenceTable.onCreate(db); 
		WordsetWordsProvider.WordsetWordsTable.onCreate(db);
		ProfileProvider.ProfileTable.onCreate(db); 
		PostItProvider.PostItTable.onCreate(db);
		ForgottenProvider.ForgottenTable.onCreate(db);
		ForgottenNotSyncedProvider.ForgottenNotSyncedTable.onCreate(db); 
		RememberMeProvider.RememberMeTable.onCreate(db);
		RememberMeNotSyncedProvider.RememberMeNotSyncedTable.onCreate(db); 
		LearnedWordsProvider.LearnedTable.onCreate(db);
		LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.onCreate(db); 
		UserWordsetProvider.UserWordsetTable.onCreate(db);
		UserWordsetWordsProvider.UserWordsetWordsTable.onCreate(db);
		TaskCategoryProvider.TaskCategoryTable.onCreate(db);
		TaskProvider.TaskTable.onCreate(db);
		SolutionContentProvider.SolutionContentTable.onCreate(db);
		SolutionProvider.SolutionTable.onCreate(db);
		LearningHistoryProvider.LearningHistoryTable.onCreate(db);
		LearningStatsProvider.LearningStatsTable.onCreate(db);
	
	}

	//method is called during creation of the database
	// ex. database version has been increased 
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// upgrading tables in database by adding new versions of tables: 
		if(oldVersion < 1) {
			// changes added in DBv2
			WordsetCategoryProvider.WordsetCategoryTable.onUpgrade(db, oldVersion, newVersion);
			WordsetProvider.WordsetTable.onUpgrade(db, oldVersion, newVersion);
			WordProvider.WordTable.onUpgrade(db, oldVersion, newVersion);
			SentenceProvider.SentenceTable.onUpgrade(db, oldVersion, newVersion);
			WordsetWordsProvider.WordsetWordsTable.onUpgrade(db, oldVersion, newVersion);
			ProfileProvider.ProfileTable.onUpgrade(db, oldVersion, newVersion);
			PostItProvider.PostItTable.onUpgrade(db, oldVersion, newVersion);
			ForgottenProvider.ForgottenTable.onUpgrade(db, oldVersion, newVersion);
			ForgottenNotSyncedProvider.ForgottenNotSyncedTable.onUpgrade(db, oldVersion, newVersion);
			RememberMeProvider.RememberMeTable.onUpgrade(db, oldVersion, newVersion);
			RememberMeNotSyncedProvider.RememberMeNotSyncedTable.onUpgrade(db, oldVersion, newVersion);
			LearnedWordsProvider.LearnedTable.onUpgrade(db, oldVersion, newVersion);
			LearnedWordsNotSyncedProvider.LearnedWordsNotSyncedTable.onUpgrade(db, oldVersion, newVersion);
			UserWordsetProvider.UserWordsetTable.onUpgrade(db, oldVersion, newVersion);
			UserWordsetWordsProvider.UserWordsetWordsTable.onUpgrade(db, oldVersion, newVersion);
			TaskCategoryProvider.TaskCategoryTable.onUpgrade(db, oldVersion, newVersion);
			TaskProvider.TaskTable.onUpgrade(db, oldVersion, newVersion);
			SolutionContentProvider.SolutionContentTable.onUpgrade(db, oldVersion, newVersion);
			SolutionProvider.SolutionTable.onUpgrade(db, oldVersion, newVersion);
		}
		if( oldVersion < 2) { 
			// changes added in DBv3
			LearningHistoryProvider.LearningHistoryTable.onUpgrade(db, oldVersion, newVersion); 
			LearningStatsProvider.LearningStatsTable.onUpgrade(db, oldVersion, newVersion); 
		}
	}

}
