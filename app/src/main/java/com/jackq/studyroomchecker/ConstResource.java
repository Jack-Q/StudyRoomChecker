

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import java.util.UUID;

/**
 * Created by Jack on 2015/1/20.
 */
public class ConstResource {
    public static final String APP_DEBUG_TAG = "JACK_Q_DBG";
    public static final String APP_BASIC_CONFIG_FILE = "com.jackQ.studyRoomChecker.basicConfig";
    public static final String CONFIG_INITIALIZED = "initialized";


    // URL Sample:http://e.tju.edu.cn/Education/schedule.do?to
    //     do=displayWeekBuilding&schekind=6&week=5&building_no=1048
    public static final String WEB_CHARSET = "GBK";
    private static final String WEB_STATIC_BASE_URL = "http://e.tju.edu.cn/Education/schedule.do?schekind=6";
    public static String WEB_BASE_URL(){

        return WEB_STATIC_BASE_URL + "&rnd="+ UUID.randomUUID().toString().replace("-","");
    }
    public static final String WEB_DATA_QUERY_PARA = "todo=displayWeekBuilding";
    public static final String WEB_WEEK_PARA = "week";
    public static final String WEB_BUILDING_PARA = "building_no";

}
