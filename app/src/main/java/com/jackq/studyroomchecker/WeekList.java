

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Jack on 2015/1/23.
 */
public class WeekList {

    public Iterator getIterator() {
        return weekItems.iterator();
    }

    public class WeekItem {
        private int weekCode;
        private String weekName;

        public WeekItem(String name, int code) {
            this.weekCode = code;
            this.weekName = name;
        }

        public String getWeekName() {
            return weekName;
        }

        public int getWeekCode() {
            return weekCode;
        }
    }

    private ArrayList<WeekItem> weekItems;

    private WeekList() {
        weekItems = new ArrayList<>();
    }

    private void add(String name, int code) {
        weekItems.add(new WeekItem(name, code));
    }


    private static WeekList weekList;

    public static WeekList get() {
        if (weekList == null) {
            weekList = new WeekList();
        }
        return weekList;
    }


    public boolean webInitialize() throws IOException {
        Document document = Jsoup.parse(
                new java.io.ByteArrayInputStream(Jsoup
                .connect(ConstResource.WEB_BASE_URL).timeout(10000).execute()
                .bodyAsBytes()), ConstResource.WEB_CHARSET, ConstResource.WEB_BASE_URL);
        //Get all <option> elements in <select> element whose name attribute is week
        Elements weekOptions = document.select("select[name=week]").first().children();
        for(Element ele :weekOptions){
            weekList.add(ele.text(), Integer.decode(ele.attr("value")));
        }
        Log.d(ConstResource.APP_DEBUG_TAG, "Size of weeks" + Integer.toString(weekList.size()));
        return true;
    }

    public int size() {
        return weekItems.size();
    }

}
