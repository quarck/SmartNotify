/*
 * Copyright (c) 2014, Sergey Parshin, quarck@gmail.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of developer (Sergey Parshin) nor the
 *       names of other project contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


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
	
	public static final int DEFAULT_REMIND_INTERVAL = 5 * 60;

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
			if (_interval == 0)
				_interval = DEFAULT_REMIND_INTERVAL;
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

	private static final int DATABASE_VERSION = 5;

	private static final String DATABASE_NAME = "Packages";

	private static final String TABLE_NAME = "packages";
	private static final String INDEX_NAME = "pkgidx";
	private static final String TABLE_DISABLED_NAME = "packages_disabled";

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

		CREATE_PKG_TABLE = "CREATE TABLE " + TABLE_DISABLED_NAME + " ( "
				+ KEY_PACKAGE + " TEXT PRIMARY KEY, " + KEY_HANDLE + " INTEGER, "
				+ KEY_INTERVAL + " INTEGER )";

		Lw.d(TAG, "Creating DB TABLE_DISABLED using query: " + CREATE_PKG_TABLE);

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
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_DISABLED_NAME);
		db.execSQL("DROP INDEX IF EXISTS " + INDEX_NAME);
		this.onCreate(db);
	}

	public void addPackage(String tableName, Package pkg)
	{
		Lw.d(TAG, "addPackage " + pkg.toString());

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_PACKAGE, pkg.getPackageName());
		values.put(KEY_HANDLE, pkg.isHandlingThis());
		values.put(KEY_INTERVAL, pkg.getRemindIntervalSeconds());

		db.insert(tableName, // table
				null, // nullColumnHack
				values); // key/value -> keys = column names/ values = column
							// values

		db.close();
	}

	public boolean isPackageHandled(String packageId)
	{
		Package pkg = getPackage(TABLE_NAME, packageId);
		
		return pkg != null && pkg.isHandlingThis();
	}

	public Package getPackage(String packageId)
	{
		return getPackage(TABLE_NAME, packageId);
	}

	public Package getPackage(String tableName, String packageId)
	{
		SQLiteDatabase db = this.getReadableDatabase();

		Lw.d(TAG, "getPackage" + packageId);

		Cursor cursor = db.query(tableName, // a. table
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
		
		if (cursor != null)
			cursor.close();

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

			cursor.close();
		}

		return packages;
	}

	public boolean isEmpty()
	{
		boolean ret = true;

		String query = "SELECT COUNT(" + KEY_PACKAGE + ") FROM " + TABLE_NAME;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		if (cursor.moveToFirst())
		{
			int count = Integer.parseInt(cursor.getString(0));
			ret = (count == 0);

			cursor.close();
		}

		return ret;
	}
	
	public int updatePackage(Package pkg)
	{
		return updatePackage(TABLE_NAME, pkg);
	}

	public int updatePackage(String tableName, Package pkg)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		// values.put(KEY_PACKAGE, pkg.getPackageName());
		values.put(KEY_HANDLE, pkg.isHandlingThis());
		values.put(KEY_INTERVAL, pkg.getRemindIntervalSeconds());

		int i = db.update(tableName, // table
				values, // column/value
				KEY_PACKAGE + " = ?", // selections
				new String[]
				{
					pkg.getPackageName()
				}); // selection args

		db.close();

		return i;
	}

	public void deletePackage(String tableName, Package pkg)
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
	
	
	public void set(String packageName, int delay, boolean enabled)
	{
		Package pkg = getPackage(TABLE_NAME, packageName);

		if (pkg == null)
		{
			Lw.d(TAG, "Added reminde for " + packageName + " enabled: " + enabled + " delay: " + delay);
			addPackage(TABLE_NAME, new Package(packageName, enabled, delay));
		}
		else
		{
			Lw.d(TAG, "Updating reminder for " + packageName + " enabled: " + enabled + " delay: " + delay);
			pkg.setRemindIntervalSeconds(delay);
			pkg.setHandlingThis(enabled);
			updatePackage(TABLE_NAME, pkg);
		}
	}

	public boolean getIsListed(String packageName)
	{
		Package pkg = getPackage(TABLE_NAME, packageName);
		return pkg != null;
	}

	public boolean getIsHandled(String packageName)
	{
		Package pkg = getPackage(TABLE_NAME, packageName);
		return pkg != null ? pkg.isHandlingThis() : false;
	}

	public int getInterval(String packageName)
	{
		Package pkg = getPackage(TABLE_NAME, packageName);
		return pkg != null ? pkg.getRemindIntervalSeconds() : DEFAULT_REMIND_INTERVAL;
	}
	
	public Package lookupEverywhereAndMoveOrInsertNew(String packageName, boolean _handleThis, int _interval)
	{
		Package pkg = getPackage(TABLE_NAME, packageName);
		
		if (pkg != null)
		{
			// nothing to do, it is already inside the main table
		}
		else
		{
			pkg = getPackage(TABLE_DISABLED_NAME, packageName);
	
			if (pkg != null)
			{
				deletePackage(TABLE_DISABLED_NAME, pkg); // delete it from the "disabled" table
			}
			else
			{
				pkg = new Package(packageName, _handleThis, _interval);
			}

			addPackage(TABLE_NAME, pkg);
		}
		
		return pkg;
	}
	
	public void hidePackage(Package pkg)
	{
		deletePackage(TABLE_NAME, pkg);
		addPackage(TABLE_DISABLED_NAME, pkg);
	}
}
