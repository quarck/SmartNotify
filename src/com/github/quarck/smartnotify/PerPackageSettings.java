package com.github.quarck.smartnotify;

import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PerPackageSettings extends SQLiteOpenHelper 
{
	public class Package 
	{		 
	    private int id;
	    // Java boilerplate  
	    public int getId() { return id; }
	    public void setId(int newId) { id = newId; }
	    
	    private String packageName;
	    // Java boilerplate  
	    public String getPackageName() { return packageName; }
	    
	    private boolean handleThisPackage;
	    // Java boilerplate  
	    public boolean isHandlingThis() { return handleThisPackage; }
	    public void setHandlingThis(boolean val) { handleThisPackage = val; }
	    
	    private int remindIntervalSeconds;
	    // Java boilerplate  
	    public int getRemindIntervalSeconds() { return remindIntervalSeconds; }
	    public void setRemindIntervalSeconds(int val) { remindIntervalSeconds = val; }

	    
	    public Package(){}
	 
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
	        return "Package [id=" + id + ", package=" + packageName + ", handle=" +
	        		handleThisPackage + ", interval=" + remindIntervalSeconds + "]";
	    }
	}
	private static final int DATABASE_VERSION = 1;
    
	private static final String DATABASE_NAME = "PackageSettingsDB";
 
    private static final String TABLE_NAME = "packages";

    private static final String KEY_ID = "id";
    private static final String KEY_PACKAGE = "package";
    private static final String KEY_HANDLE = "handle";
    private static final String KEY_INTERVAL = "interval";

    private final String[] COLUMNS = {KEY_ID,KEY_PACKAGE,KEY_HANDLE,KEY_INTERVAL};
    
    public PerPackageSettings(Context context) 
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);  
    }
 
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PKG_TABLE = "CREATE TABLE " + TABLE_NAME + " ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                KEY_PACKAGE + " TEXT, "+
                KEY_HANDLE + " INTEGER, "+
                KEY_INTERVAL + " INTEGER )";

        db.execSQL(CREATE_PKG_TABLE);
    }
 
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        this.onCreate(db);
    }	 
    
    public void addPackage(Package pkg)
    {
	    Log.d("addPackage", pkg.toString()); 
	
	    SQLiteDatabase db = this.getWritableDatabase();

	    ContentValues values = new ContentValues();
	    values.put(KEY_PACKAGE, pkg.getPackageName());  
	    values.put(KEY_HANDLE, pkg.isHandlingThis()); 
	    values.put(KEY_INTERVAL, pkg.getRemindIntervalSeconds());

	    db.insert(TABLE_NAME, // table
	            null, //nullColumnHack
	            values); // key/value -> keys = column names/ values = column values

	    db.close(); 
	}
    
    
    public Package getPackage(int id)
    {

        SQLiteDatabase db = this.getReadableDatabase();
     
        Cursor cursor = 
                db.query(TABLE_NAME, // a. table
                COLUMNS, // b. column names
                " id = ?", // c. selections 
                new String[] { String.valueOf(id) }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit
     
        if (cursor != null)
            cursor.moveToFirst();
     
        Package pkg = 
        		new Package(
        				cursor.getString(1),
        				Integer.parseInt(cursor.getString(2))!=0,
						Integer.parseInt(cursor.getString(3))
        			);
        pkg.setId(Integer.parseInt(cursor.getString(0)));

        Log.d("getPackage("+id+")", pkg.toString());
     
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
            do {
    	        pkg = 
	        		new Package(
	        				cursor.getString(1),
	        				Integer.parseInt(cursor.getString(2))!=0,
    						Integer.parseInt(cursor.getString(3))
	        			);
    	        pkg.setId(Integer.parseInt(cursor.getString(0)));
  
                packages.add(pkg);
            } while (cursor.moveToNext());
        }
  
        Log.d("getAllPackages()", packages.toString());
  
        return packages;
    }
    
    public int updatePackage(Package pkg) 
    {
        SQLiteDatabase db = this.getWritableDatabase();
     
	    ContentValues values = new ContentValues();
	    values.put(KEY_PACKAGE, pkg.getPackageName());  
	    values.put(KEY_HANDLE, pkg.isHandlingThis()); 
	    values.put(KEY_INTERVAL, pkg.getRemindIntervalSeconds());
     
        int i = db.update(TABLE_NAME, //table
                values, // column/value
                KEY_ID+" = ?", // selections
                new String[] { String.valueOf(pkg.getId()) }); //selection args
     
        db.close();
     
        return i;
     
    }
    
    public void deletePackage(Package pkg) 
    {
        SQLiteDatabase db = this.getWritableDatabase();
 
        db.delete(TABLE_NAME, //table name
                KEY_ID+" = ?",  // selections
                new String[] { String.valueOf(pkg.getId()) }); //selections args
 
        db.close();
 
        Log.d("deletePackage", pkg.toString());	 
    }
}
