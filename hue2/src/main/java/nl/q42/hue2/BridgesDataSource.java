package nl.q42.hue2;

import java.util.ArrayList;
import java.util.List;

import nl.q42.hue2.models.Bridge;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class BridgesDataSource {
	
	private SQLiteDatabase db;
	private DBHelper dbHelper;

	public BridgesDataSource(Context context) {
		dbHelper = new DBHelper(context);
	}

	public void open() throws SQLException {
		db = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public void insertBridge(String name, String fullConfig, String user) {
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("fullConfig", fullConfig);
		values.put("user", user);
		values.put("lastUsed", System.currentTimeMillis());
		// long insertId = database.insert("bridges", null, values);
		db.insert("bridges", null, values);
	}

	public List<Bridge> getAllBridges() {
		List<Bridge> bridges = new ArrayList<Bridge>();

		Cursor cursor = db.query("bridges", dbHelper.allColumns(), null, null, null, null, "lastUsed desc");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Bridge bridge = cursorToBridge(cursor);
			bridges.add(bridge);
			cursor.moveToNext();
		}
		cursor.close();
		return bridges;
	}

	private Bridge cursorToBridge(Cursor cursor) {
		  Bridge bridge = new Bridge();
		  bridge.setId(cursor.getLong(0));
		  bridge.setName(cursor.getString(1));
		  bridge.setFullConfig(cursor.getString(2));
		  bridge.setUser(cursor.getString(3));
		  bridge.setLastUsed(cursor.getLong(4));
		  return bridge;
	  }
}
