

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends ActionBarActivity {
    private Spinner
            weekSpinner = null,
            daySpinner = null;
    private TabManager classTab = null;
    private ExpandableListView expandableListView = null;
    private TextView noCacheTipTextView = null;
    private StatusExpandableListAdapter statusExpandableListAdapter = null;

    private boolean initialized = false;
    private String currentThread = null;
    private static final int FINISH_QUERY = 0;
    private static final int UPDATE_DATA = 1;

    private boolean isCached = false;
    private boolean isInQueryProcess = false;
    private int selectedWeekCode;
    private int selectedDayCode;
    private int selectedClassCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("Main", "Main Loaded");

        weekSpinner = (Spinner) findViewById(R.id.weekSpinner);
        daySpinner = (Spinner) findViewById(R.id.daySpinner);
        classTab = new TabManager((LinearLayout) findViewById(R.id.classTab), R.array.className);
        expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);
        noCacheTipTextView = (TextView) findViewById(R.id.no_cache_tip);

        weekSpinner.setAdapter(new WeekSpinnerAdapter(
                DatabaseManager.get(getApplication()).getWeekList()));
        weekSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWeekCode = (int) view.findViewById(R.id.text).getTag();
                updateStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, R.layout.item_main_spinner);
        dayAdapter.addAll(getResources().getStringArray(R.array.weekday));
        daySpinner.setAdapter(dayAdapter);
        daySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDayCode = position + 1;
                updateStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        classTab.setOnTabSelectListener(new OnSelectionChangeListener() {
            @Override
            public void onSelectionChanged(int index) {
                selectedClassCode = index;
                updateStatus();
            }
        }).select(0);

        statusExpandableListAdapter = new StatusExpandableListAdapter();
        expandableListView.setAdapter(statusExpandableListAdapter);

        initialized = true;
        updateStatus();
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(int index);
    }

    private class TabManager {
        private List<Tab> list;
        private LinearLayout layout;
        private final int layoutRes = R.layout.item_main_tab;
        private int selection = -1;
        private OnSelectionChangeListener listener = null;
        private int defaultBackground = R.drawable.main_class_default_background;
        private int activeBackground = R.drawable.main_class_active_background;

        private class Tab {
            public View view;
            public String text;
            public int position;

            public Tab(String text, int position) {
                this.text = text;
                this.position = position;
            }
        }

        private void changeSelection(int index) {
            if (index == selection) {
                return;
            }

            selection = index;
            draw();

            if (listener != null) {
                listener.onSelectionChanged(index);
            }
        }

        public TabManager(LinearLayout layout, int stringArrayResource) {
            this.layout = layout;
            list = new ArrayList<>();
            for (String str : getResources().getStringArray(stringArrayResource)) {
                list.add(new Tab(str, list.size()));
            }
        }

        public TabManager select(int id) {
            if (id >= 0 && id < list.size()) {
                changeSelection(id);
            }
            return this;
        }

        private TabManager draw() {
            for (Tab t : list) {
                if (t.view == null) {
                    t.view = getLayoutInflater().inflate(layoutRes, null);
                    ((TextView) (t.view.findViewById(R.id.text))).setText(t.text);
                    t.view.setTag(t.position);
                    t.view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            changeSelection((int) v.getTag());
                        }
                    });
                    layout.addView(t.view);
                }
                t.view.findViewById(R.id.text)
                        .setBackgroundResource(defaultBackground);
            }
            list.get(selection).view.findViewById(R.id.text)
                    .setBackgroundResource(activeBackground);
            return this;
        }

        public TabManager setOnTabSelectListener(OnSelectionChangeListener listener) {
            this.listener = listener;
            return this;
        }

    }

    private class WeekSpinnerAdapter extends BaseAdapter {
        List<Map<String, Object>> weekList = null;

        public WeekSpinnerAdapter(List<Map<String, Object>> weekList) {
            this.weekList = weekList;
        }

        @Override
        public int getCount() {
            return weekList.size();
        }

        @Override
        public Object getItem(int position) {
            return weekList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_main_spinner, null);
            }
            TextView textView = (TextView) convertView.findViewById(R.id.text);
            textView.setText((String) weekList.get(position).get("week_name"));
            textView.setTag(weekList.get(position).get("week_code"));
            return convertView;
        }
    }

    private class StatusExpandableListAdapter extends BaseExpandableListAdapter {
        private class Building {
            public String building_name;
            public int building_code;
            public List<String> room_available_list;
            public List<String> room_unavailable_list;

            public Building(String name, int code) {
                this.building_name = name;
                this.building_code = code;
                room_available_list = new ArrayList<>();
                room_unavailable_list = new ArrayList<>();
            }
        }

        private List<Integer> simpleList;

        private List<Building> buildingList;

        public List<Integer> getBuildingList() {
            return simpleList;
        }

        public void updateData(int position) {
            Building bld = buildingList.get(position);
            bld.room_available_list.clear();
            bld.room_available_list.clear();
        }

        public void updateData(int position,
                               List<String> room_available_list,
                               List<String> room_unavailable_list) {
            Building bld = buildingList.get(position);
            bld.room_available_list = room_available_list;
            bld.room_available_list = room_unavailable_list;
        }

        public StatusExpandableListAdapter() {
            buildingList = new ArrayList<>();
            simpleList = new ArrayList<>();
            for (Map<String, Object> bld : DatabaseManager.get(getApplication()).getBuildingList()) {
                buildingList.add(
                        new Building((String) (bld.get("building_name")),
                                (int) (bld.get("building_id"))));
                simpleList.add((int) (bld.get("building_id")));
            }
        }

        @Override
        public int getGroupCount() {
            if (isInQueryProcess) {
                return 0;
            }
            if (!isCached) {
                return 0;
            }
            return buildingList.size();
        }


        @Override
        public int getChildrenCount(int groupPosition) {
            return buildingList.get(groupPosition).room_available_list.size();
        }

        @Override
        public Building getGroup(int groupPosition) {
            return buildingList.get(groupPosition);
        }

        @Override
        public String getChild(int groupPosition, int childPosition) {
            return buildingList.get(groupPosition).room_available_list.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return buildingList.get(groupPosition).building_code;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_main_expandable_group, null);
            }
            ((TextView) convertView.findViewById(R.id.text))
                    .setText(getGroup(groupPosition).building_name);
            TextView countView = ((TextView) convertView.findViewById(R.id.count));
            int count = getGroup(groupPosition).room_available_list.size();
            if (count > 0) {
                countView.setText(Integer.toString(count));
                countView.setVisibility(View.VISIBLE);
            } else {
                countView.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_main_expandable, null);
            }
            ((TextView) convertView.findViewById(R.id.text)).setText(getChild(groupPosition, childPosition));
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }

    private void updateStatus() {
        if (!initialized) {
            return;
        }
        isInQueryProcess = true;
        statusExpandableListAdapter.notifyDataSetChanged();
        new Thread(runnable).start();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.getData().getString("uuid").equals(currentThread)) {
                switch (msg.what) {
                    case UPDATE_DATA:
                        //Currently redundant
                    case FINISH_QUERY:
                        isInQueryProcess = false;
                        if (isCached) {
                            noCacheTipTextView.setVisibility(View.GONE);
                        } else {
                            noCacheTipTextView.setVisibility(View.VISIBLE);
                        }
                        statusExpandableListAdapter.notifyDataSetChanged();
                        break;
                }
            }
        }
    };

    Runnable runnable = new Runnable() {
        private String uuid;

        @Override
        public void run() {

            currentThread = this.uuid = UUID.randomUUID().toString();
            isCached = DatabaseManager.get(getApplication()).isDataCached(selectedWeekCode);
            if (isCached) {
                List<Map<String, Object>> data = DatabaseManager.get(getApplication()).getRoomStatus(
                        selectedWeekCode, selectedDayCode, selectedClassCode);
                List<Integer> buildingList = statusExpandableListAdapter.getBuildingList();
                List<Integer> dataCodeList = new ArrayList<>();
                for (Map<String, Object> bld : data) {
                    dataCodeList.add((Integer) bld.get("building_id"));
                }
                if (!(currentThread.equals(this.uuid))) {
                    return;
                }
                for (int i = 0; i < buildingList.size(); i++) {
                    int code = buildingList.get(i);
                    int position = dataCodeList.indexOf(code);
                    if (position == -1) {
                        statusExpandableListAdapter.updateData(i);
                    } else {
                        statusExpandableListAdapter.updateData(i,
                                (List<String>) (data.get(position).get("room_available_list")),
                                (List<String>) (data.get(position).get("room_unavailable_list")));
                    }
                }
            }
            if (currentThread.equals(this.uuid)) {
                Bundle bundle = new Bundle();
                bundle.putString("uuid", this.uuid);
                Message message = new Message();
                message.what = FINISH_QUERY;
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_help:
                startActivity(new Intent(this, HelpActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
