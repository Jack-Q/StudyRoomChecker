
/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Jack on 2015/1/20.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private final static String dbName = "jack_q_study_room_checker_data.sqlite_db";
    private final static int dbVersion = 1;
    private static DatabaseHelper dbInstance = null;
    private Context ctx = null;

    private DatabaseHelper(Context context) {
        super(context, dbName, null, dbVersion);
        ctx = context;
    }

    public static DatabaseHelper getDbInstance(Context ctx){
        if(dbInstance==null){
            dbInstance = new DatabaseHelper(ctx);
        }
        return dbInstance;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS week_list " +
                "( _id INTEGER PRIMARY KEY AUTOINCREMENT, week_name VARCHAR(30), " +
                " week_code INTEGER, week_cache INTEGER )");
        db.execSQL("CREATE TABLE IF NOT EXISTS building_list " +
                "( _id INTEGER PRIMARY KEY AUTOINCREMENT, building_id INTEGER, " +
                " building_name VARCHAR(30), building_code VARCHAR(15) )");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_day " +
                "( _id INTEGER PRIMARY KEY AUTOINCREMENT, week_code INTEGER, " +
                " day INTEGER, building_id INTEGER, room_name VARCHAR, " +
                " mor1 SMALLINT , mor2 SMALLINT ," + // Two class in morning
                " aft1 SMALLINT , aft2 SMALLINT ," + // Two class in afternoon
                " nig1 SMALLINT , nig2 SMALLINT )"); // Twp class in evening
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
