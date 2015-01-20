package com.wawoo.database;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.wawoo.data.Reminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	// Logcat tag
	private static final String LOG = "DatabaseHelper";

	// Database Version
	public static final int DATABASE_VERSION = 3;

	// Database Name
	public static final String DATABASE_NAME = "VueDatabase";

	// Table Names
	public static final String TABLE_SERVICES = "services";
	public static final String TABLE_SERVICE_CATEGORIES = "service_categories";
	public static final String TABLE_SERVICE_SUB_CATEGORIES = "service_sub_categories";
	private static final String TABLE_PROGRAM_REMINDER = "program_reminder";

	// Common column names
	public static final String KEY_ID = "_id";
	public static final String SERVICE_ID = "service_id";
	public static final String CHANNEL_NAME = "channel_name";
	public static final String CATEGORY = "category";
	public static final String SUB_CATEGORY = "sub_category";
	public static final String URL = "url";

	// Service column names
	public static final String CLIENT_ID = "client_id";
	public static final String CHANNEL_DESC = "channel_desc";
	public static final String IMAGE = "image";
	public static final String IS_FAVOURITE = "is_favourite";

	// program_reminder column names
	private static final String PROG_NAME = "program_name";
	private static final String TIME = "time";

	// Table Services Create Statements
	private static final String CREATE_TABLE_SERVICES = "CREATE TABLE "
			+ TABLE_SERVICES + "(" + KEY_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + SERVICE_ID + " INTEGER,"
			+ CLIENT_ID + " INTEGER," + CHANNEL_NAME + " TEXT," + CHANNEL_DESC
			+ " TEXT," + CATEGORY + " TEXT," + SUB_CATEGORY + " TEXT," + IMAGE
			+ " TEXT," + URL + " TEXT," + IS_FAVOURITE + " NUMERIC DEFAULT 0,"
			+ "UNIQUE(" + SERVICE_ID + ") ON CONFLICT REPLACE" + ")";

	// Table Categories Create Statements
	private static final String CREATE_TABLE_SERVICE_CATEGORIES = "CREATE TABLE "
			+ TABLE_SERVICE_CATEGORIES
			+ "("
			+ KEY_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ CATEGORY
			+ " TEXT,"
			+ "UNIQUE(" + CATEGORY + ") ON CONFLICT REPLACE" + ")";
	// Table Services Create Statements
	private static final String CREATE_TABLE_SERVICE_SUB_CATEGORIES = "CREATE TABLE "
			+ TABLE_SERVICE_SUB_CATEGORIES
			+ "("
			+ KEY_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
			+ SUB_CATEGORY
			+ " TEXT,"
			+ "UNIQUE(" + SUB_CATEGORY + ") ON CONFLICT REPLACE" + ")";

	private static final String CREATE_REMINDER_TABLE = "CREATE TABLE "
			+ TABLE_PROGRAM_REMINDER + "(" + KEY_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT," + PROG_NAME + " TEXT,"
			+ TIME + " INTEGER," + SERVICE_ID + " INTEGER," + CHANNEL_DESC
			+ " TEXT," + URL + " TEXT" + ")";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// creating required tables
		db.execSQL(CREATE_TABLE_SERVICES);
		db.execSQL(CREATE_TABLE_SERVICE_CATEGORIES);
		db.execSQL(CREATE_TABLE_SERVICE_SUB_CATEGORIES);
		db.execSQL(CREATE_REMINDER_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// on upgrade drop older tables
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE_CATEGORIES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE_SUB_CATEGORIES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROGRAM_REMINDER);
		// create new tables
		onCreate(db);
	}

	public void addReminder(Reminder reminder) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(PROG_NAME, reminder.get_prog_name());
		values.put(TIME, reminder.get_time());
		values.put(SERVICE_ID, reminder.get_channel_id());
		values.put(CHANNEL_DESC, reminder.get_channel_desc());
		values.put(URL, reminder.get_url());
		db.insert(TABLE_PROGRAM_REMINDER, null, values);
		db.close();
	}

	public List<Reminder> getAllReminder() {
		List<Reminder> reminderList = new ArrayList<Reminder>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_PROGRAM_REMINDER;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				Reminder reminder = new Reminder();
				reminder.set_id((Integer.parseInt(cursor.getString(0))));
				reminder.set_prog_name(cursor.getString(1));
				reminder.set_time(cursor.getLong(2));
				reminder.set_channel_id(cursor.getInt(3));
				reminder.set_channel_desc(cursor.getString(4));
				reminder.set_url(cursor.getString(5));
				reminderList.add(reminder);
			} while (cursor.moveToNext());
		}

		return reminderList;
	}

	public void deleteOldReminders() {
		SQLiteDatabase db = this.getWritableDatabase();
		Calendar c = Calendar.getInstance();
		long currentMillies = c.getTimeInMillis();

		db.delete(TABLE_PROGRAM_REMINDER, TIME + " < " + currentMillies, null);
		db.close();
	}

	public void deleteAllReminders() {
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("delete from " + TABLE_PROGRAM_REMINDER);
		db.close();
	}
}