
/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DatabaseManager {
    private DatabaseHelper dbHelper = null;
    private SQLiteDatabase db = null;
    private static DatabaseManager databaseManager = null;
    private Context ctx;

    private DatabaseManager(Context ctx) {
        dbHelper = DatabaseHelper.getDbInstance(ctx);
        this.ctx = ctx;
    }

    public static DatabaseManager get(Context ctx) {
        if (databaseManager == null) {
            databaseManager = new DatabaseManager(ctx);
        }
        return databaseManager;
    }

    //Drop current week_List and ROOM_DAY TABLE
    public synchronized void updateWeekList() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        Log.d(ConstResource.APP_DEBUG_TAG, "Database is open? " + Boolean.toString(db.isOpen()));
        db.delete("week_list", null, null);
        db.delete("room_day", null, null);
        Iterator iterator = WeekList.get().getIterator();
        while (iterator.hasNext()) {
            WeekList.WeekItem item = (WeekList.WeekItem) iterator.next();
            ContentValues cv = new ContentValues();
            cv.put("week_name", item.getWeekName());
            cv.put("week_code", item.getWeekCode());
            cv.put("week_cache", 0);
            db.insert("week_list", null, cv);
        }
        db.close();
    }

    //Drop current building_list and room_day table
    public synchronized void updateBuildingList() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        Log.d(ConstResource.APP_DEBUG_TAG, "Database is open? " + Boolean.toString(db.isOpen()));
        db.delete("building_list", null, null);
        db.delete("room_day", null, null);
        Iterator iterator = BuildingList.get().getIterator();
        while (iterator.hasNext()) {
            BuildingList.BuildingItem item = (BuildingList.BuildingItem) iterator.next();
            ContentValues cv = new ContentValues();
            cv.put("building_name", item.getBuildingName());
            cv.put("building_code", item.getBuildingCode());
            cv.put("building_id", item.getBuildingId());
            db.insert("building_list", null, cv);
        }
        db.close();
    }

    //Get week list with data status
    public synchronized List<Map<String, Object>> getWeekList() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        Log.d(ConstResource.APP_DEBUG_TAG, "Loading week list");
        Cursor cursor = db.rawQuery("SELECT * FROM `week_list`", null);
        int count = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < count; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("week_name", cursor.getString(1));
            map.put("week_code", cursor.getInt(2));
            int cache = cursor.getInt(3);
            map.put("week_cache", cache);
            if (cache == 0) {
                map.put("week_info", ctx.getString(R.string.have_not_downloaded_yet));
            } else {
                map.put("week_info", ctx.getString(R.string.last_cached)
                        + new SimpleDateFormat("yyyy-MM-dd")
                        .format(new Date((long) cache * 1000 * 60 * 60 * 24)));
            }
            list.add(map);
            cursor.moveToNext();
        }
        db.close();
        return list;
    }

    public synchronized List<Map<String, Object>> getBuildingList() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        Log.d(ConstResource.APP_DEBUG_TAG, "Loading building list");
        Cursor cursor = db.rawQuery("SELECT * FROM `building_list`", null);
        int count = cursor.getCount();
        cursor.moveToFirst();
        for (int i = 0; i < count; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("building_id", cursor.getInt(1));
            map.put("building_name", cursor.getString(2));
            map.put("building_code", cursor.getString(3));
            list.add(map);
            cursor.moveToNext();
        }
        db.close();
        return list;

    }

    public synchronized void insertRoomDay(String room_name, int week_code, int day, int building_id, int[] roomInfo) {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        //Delete the old data if exists
        db.delete(" room_day", "week_code = ? " +
                        "and day = ? " +
                        "and building_id = ? " +
                        "and room_name = ? ",
                new String[]{
                        String.valueOf(week_code),
                        String.valueOf(day),
                        String.valueOf(building_id),
                        room_name
                });
        ContentValues cv = new ContentValues();
        cv.put("week_code", week_code);
        cv.put("day", day);
        cv.put("building_id", building_id);
        cv.put("room_name", room_name);
        cv.put("mor1", roomInfo[0]);
        cv.put("mor2", roomInfo[1]);
        cv.put("aft1", roomInfo[2]);
        cv.put("aft2", roomInfo[3]);
        cv.put("nig1", roomInfo[4]);
        cv.put("nig2", roomInfo[5]);
        db.insert("room_day", null, cv);
        db.close();
    }

    public synchronized void updateCacheHistory(int week_code, int cache) {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        ContentValues cv = new ContentValues();
        cv.put("week_cache", cache);
        db.update("week_list", cv, " week_code = ? ", new String[]{Integer.toString(week_code)});
        Log.d(ConstResource.APP_DEBUG_TAG, "Update cache history");
//        db.rawQuery("UPDATE 'week_list' SET 'week_cache' = "
//                + Integer.toString(cache) + " WHERE week_code = "
//                + Integer.toString(week_code), null);
        db.close();
    }

    public synchronized List<Map<String, Object>> getRoomStatus(int week_code, int day, int class_code) {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        Log.d(ConstResource.APP_DEBUG_TAG, "Loading building list");
        Cursor cursor;
        String sql = "SELECT building_id , room_name , ";

        switch (class_code) {
            case 0:
                //First class in the morning
                sql += "mor1";
                break;
            case 1:
                //Second class in the morning
                sql += "mor2";
                break;
            case 2:
                //First class in the afternoon
                sql += "aft1";
                break;
            case 3:
                //Second class in the afternoon
                sql += "aft2";
                break;
            case 4:
                //First class at night
                sql += "nig1";
                break;
            case 5:
                //Second class at night
                sql += "nig2";
                break;
        }
        sql += " FROM `room_day` where week_code = ? and day = ?";
        Log.d(ConstResource.APP_DEBUG_TAG, sql);
        cursor = db.rawQuery(sql, new String[]{
                Integer.toString(week_code),
                Integer.toString(day)
        });
        int count = cursor.getCount();
        cursor.moveToFirst();
        while (cursor.getPosition() < count) {
            int currentBuilding = cursor.getInt(0);
            Map<String, Object> map = new HashMap<>();
            map.put("building_id", currentBuilding);
            List<String> room_available_list = new ArrayList<>();
            List<String> room_unavailable_list = new ArrayList<>();
            while (cursor.getPosition() < count
                    && currentBuilding == cursor.getInt(0)) {
                // 1 means the room is available, vice versa
                if (cursor.getInt(2) == 1) {
                    room_available_list.add(cursor.getString(1));
                } else {
                    room_unavailable_list.add(cursor.getString(1));
                }
                cursor.moveToNext();
            }
            map.put("room_available_list", room_available_list);
            Log.d(ConstResource.APP_DEBUG_TAG, "room_available_list" + room_available_list.size());
            map.put("room_unavailable_list", room_unavailable_list);
            list.add(map);
        }
        db.close();
        return list;
    }

    public synchronized Boolean isDataCached(int week_code) {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
        }
        Boolean isCached = false;
        Cursor cursor = db.rawQuery(
                "select week_cache from week_list where week_code = ? ",
                new String[]{
                        String.valueOf(week_code)
                });
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            if (cursor.getInt(0) > 0) {
                isCached = true;
            }
        }
        db.close();
        return isCached;
    }
}
