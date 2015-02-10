
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Jack on 2015/1/23.
 */
public class BuildingList {

    public Iterator getIterator() {
        return buildingItems.iterator();
    }

    public class BuildingItem {
        private int buildingId;
        private String buildingCode;
        private String buildingName;

        public BuildingItem(int id, String name, String code) {
            buildingCode = code;
            buildingId = id;
            buildingName = name;
        }

        public int getBuildingId() {
            return buildingId;
        }

        public String getBuildingCode() {
            return buildingCode;
        }

        public String getBuildingName() {
            return buildingName;
        }
    }

    private ArrayList<BuildingItem> buildingItems;

    private BuildingList() {
        buildingItems = new ArrayList<>();
    }

    private void add(int id, String name, String code) {
        buildingItems.add(new BuildingItem(id, name, code));
    }


    private static BuildingList buildingList;

    public static BuildingList get() {
        if (buildingList == null) {
            buildingList = new BuildingList();
        }
        return buildingList;
    }


    public boolean webInitialize() throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Jsoup
                .connect(ConstResource.WEB_BASE_URL()).timeout(10000).execute()
                .bodyAsBytes());
        Document document = Jsoup.parse(
                inputStream, ConstResource.WEB_CHARSET, ConstResource.WEB_BASE_URL());
        //Get all <option> elements in <select> element whose name attribute is building_no
        Elements buildingOptions = document.select("select[name=building_no]").first().children();
        for (Element ele : buildingOptions) {
            buildingList.add(ele.elementSiblingIndex(),ele.text(), ele.attr("value"));
        }
        Log.d(ConstResource.APP_DEBUG_TAG, "Size of buildings" + Integer.toString(buildingList.size()));
        return true;
    }

    public int size() {
        return buildingItems.size();
    }

}
