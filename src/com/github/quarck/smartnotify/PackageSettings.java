package com.github.quarck.smartnotify;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PackageSettings extends SQLiteOpenHelper
{
	private static final String TAG = "DB";

	public class Package
	{
		private String packageName;

		// Java boilerplate
		public String getPackageName()
		{
			return packageName;
		}

		private boolean handleThisPackage;

		// Java boilerplate
		public boolean isHandlingThis()
		{
			return handleThisPackage;
		}

		public void setHandlingThis(boolean val)
		{
			handleThisPackage = val;
		}

		private int remindIntervalSeconds;

		// Java boilerplate
		public int getRemindIntervalSeconds()
		{
			return remindIntervalSeconds;
		}

		public void setRemindIntervalSeconds(int val)
		{
			remindIntervalSeconds = val;
		}

		public Package()
		{
		}

		public Package(String _packageName, boolean _handleThis, int _interval)
		{
			super();
			packageName = _packageName;
			handleThisPackage = _handleThis;
			remindIntervalSeconds = _interval;
		}

		@Override
		public String toString()
		{
			return "Package [package=" + packageName + ", handle="
					+ handleThisPackage + ", interval=" + remindIntervalSeconds
					+ "]";
		}
	}

	private static final int DATABASE_VERSION = 2;

	private static final String DATABASE_NAME = "Packages";

	private static final String TABLE_NAME = "packages";
	private static final String INDEX_NAME = "pkgidx";

	// private static final String KEY_ID = "id";
	private static final String KEY_PACKAGE = "package";
	private static final String KEY_HANDLE = "handle";
	private static final String KEY_INTERVAL = "interval";

	private final String[] COLUMNS =
	{
		KEY_PACKAGE, KEY_HANDLE, KEY_INTERVAL
	};

	public PackageSettings(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String CREATE_PKG_TABLE = "CREATE TABLE " + TABLE_NAME + " ( "
				+ KEY_PACKAGE + " TEXT PRIMARY KEY, " + KEY_HANDLE + " INTEGER, "
				+ KEY_INTERVAL + " INTEGER )";

		Lw.d(TAG, "Creating DB TABLE using query: " + CREATE_PKG_TABLE);

		db.execSQL(CREATE_PKG_TABLE);

		String CREATE_INDEX = "CREATE UNIQUE INDEX " + INDEX_NAME + " ON "
				+ TABLE_NAME + " (" + KEY_PACKAGE + ")";

		Lw.d(TAG, "Creating DB INDEX using query: " + CREATE_INDEX);
		
		db.execSQL(CREATE_INDEX);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Lw.d(TAG, "DROPPING table and index");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		db.execSQL("DROP INDEX IF EXISTS " + INDEX_NAME);
		this.onCreate(db);
	}

	public void addPackage(Package pkg)
	{
		Lw.d(TAG, "addPackage " + pkg.toString());

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_PACKAGE, pkg.getPackageName());
		values.put(KEY_HANDLE, pkg.isHandlingThis());
		values.put(KEY_INTERVAL, pkg.getRemindIntervalSeconds());

		db.insert(TABLE_NAME, // table
				null, // nullColumnHack
				values); // key/value -> keys = column names/ values = column
							// values

		db.close();
	}

	public boolean isPackageHandled(String packageId)
	{
		Package pkg = getPackage(packageId);
		
		return pkg != null && pkg.isHandlingThis();
	}
	
	public Package getPackage(String packageId)
	{
		SQLiteDatabase db = this.getReadableDatabase();

		Lw.d(TAG, "getPackage" + packageId);

		Cursor cursor = db.query(TABLE_NAME, // a. table
				COLUMNS, // b. column names
				" " + KEY_PACKAGE + " = ?", // c. selections
				new String[]
				{
					packageId
				}, // d. selections args
				null, // e. group by
				null, // f. h aving
				null, // g. order by
				null); // h. limit

		Package pkg = null;

		if (cursor != null && cursor.getCount() >= 1)
		{
			cursor.moveToFirst();

			pkg = new Package(cursor.getString(0), Integer.parseInt(cursor
					.getString(1)) != 0, Integer.parseInt(cursor.getString(2)));
		}

		return pkg;
	}

	public List<Package> getAllPackages()
	{
		List<Package> packages = new LinkedList<Package>();

		String query = "SELECT  * FROM " + TABLE_NAME;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		Package pkg = null;
		if (cursor.moveToFirst())
		{
			do
			{
				pkg = new Package(cursor.getString(0), Integer.parseInt(cursor
						.getString(1)) != 0, Integer.parseInt(cursor
						.getString(2)));

				packages.add(pkg);
			}
			while (cursor.moveToNext());
		}

		return packages;
	}

	public int updatePackage(Package pkg)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		// values.put(KEY_PACKAGE, pkg.getPackageName());
		values.put(KEY_HANDLE, pkg.isHandlingThis());
		values.put(KEY_INTERVAL, pkg.getRemindIntervalSeconds());

		int i = db.update(TABLE_NAME, // table
				values, // column/value
				KEY_PACKAGE + " = ?", // selections
				new String[]
				{
					pkg.getPackageName()
				}); // selection args

		db.close();

		return i;

	}

	public void deletePackage(Package pkg)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		db.delete(TABLE_NAME, // table name
				KEY_PACKAGE + " = ?", // selections
				new String[]
				{
					pkg.getPackageName()
				}); // selections args

		db.close();

		Lw.d(TAG, "deletePackage "  + pkg.toString());
	}
}
