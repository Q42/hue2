package nl.q42.hue2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper{
	private static final int DATABASE_VERSION = 1;

    DBHelper(Context context) {
        super(context, "bridges", null, DATABASE_VERSION);
    }

    public String[] allColumns() {
    	return new String[]{ "_id", "name", "fullConfig", "user", "lastUsed" };
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE bridges (" +
        		"_id INTEGER PRIMARY KEY AUTOINCREMENT," +
        		"name TEXT," +
        		"fullConfig TEXT," +
        		"user TEXT," +
        		"lastUsed INTEGER)");
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("hue2", "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS bridges");
		onCreate(db);
	}
}
