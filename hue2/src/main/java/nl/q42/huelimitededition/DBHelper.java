package nl.q42.huelimitededition;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper{
	private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, "local_settings", null, DATABASE_VERSION);
    }

    public String[] allColumns() {
    	return new String[]{ "id", "bridge_serial", "light_id", "group_id", "color_mode", "x_color", "y_color", "ct", "brightness" };
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE presets (" +
        		"id INTEGER PRIMARY KEY AUTOINCREMENT," +
        		"bridge_serial TEXT," +
        		"light_id TEXT," +
        		"group_id TEXT," +
        		"color_mode TEXT," +
        		"x_color REAL," +
        		"y_color REAL," +
        		"ct REAL," +
        		"brightness INTEGER)");
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
		db.execSQL("DROP TABLE IF EXISTS presets");
		
		onCreate(db);
	}
}
