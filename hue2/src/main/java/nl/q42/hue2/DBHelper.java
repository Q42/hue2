package nl.q42.hue2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper{
	private static final int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, "light_presets", null, DATABASE_VERSION);
    }

    public String[] allColumns() {
    	return new String[]{ "id", "bridge_serial", "light_id", "x_color", "y_color", "brightness" };
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE light_presets (" +
        		"id INTEGER PRIMARY KEY AUTOINCREMENT," +
        		"bridge_serial TEXT," +
        		"light_id TEXT," +
        		"x_color REAL," +
        		"y_color REAL," +
        		"brightness INTEGER)");
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w("hue2", "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS light_presets");
		onCreate(db);
	}
}
