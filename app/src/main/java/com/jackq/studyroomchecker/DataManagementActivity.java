

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class DataManagementActivity extends ActionBarActivity {
    private boolean isDownloadingProgressShowed = false;
    private TextView downloadDetail = null;
    private ProgressBar progressBar = null;
    private ViewSwitcher viewSwitcher = null;
    private ListView listView = null;
    private List<Map<String, Object>> weekList = null;
    private Context context;

    private boolean downloader_isRunning = false;

    private DownloadHelper downloadHelper = null;
    private DatabaseManager databaseManager = null;

    private enum downloadStatus {
        WAIT_TO_DOWNLOAD,   //Haven't downloaded yet
        WAIT_TO_UPDATE,     //Downloaded, but may need update
        WAIT_IN_QUEUE,      //Add to download queue, haven't started to download
        DOWNLOADING         //Downloading
    }

    private boolean onReturn() {
        if (isDownloadingProgressShowed) {
            isDownloadingProgressShowed = false;
            viewSwitcher.showPrevious();
            return true;
        }
        if (downloader_isRunning) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.tip));
            builder.setIconAttribute(android.R.attr.dialogMessage);
            builder.setMessage(getString(R.string.downloading_exit_tip));

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_management);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        progressBar = (ProgressBar) findViewById(R.id.download_progress);
        listView = (ListView) findViewById(R.id.list_view);
        downloadDetail = (TextView) findViewById(R.id.download_detail);
        downloadDetail.setText(R.string.download_detail_null);
        context = this;

        Log.d(ConstResource.APP_DEBUG_TAG, "Main thread");
        progressBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownLoadingProgress();
            }
        });
        progressBar.setMax(10000);//Max of progress bar
        findViewById(R.id.return_tip).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideDownLoadingProgress();
            }
        });

        databaseManager = DatabaseManager.get(getApplication());
        weekList = databaseManager.getWeekList();
        listView.setAdapter(new DownloadAdapter(context, weekList, getDownloadHelper()));


    }

    @Override
    public void onBackPressed() {
        if(!onReturn()){
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return onReturn() || super.onSupportNavigateUp();
    }

    private final class DownloadHelper {
        private List<Task> taskList = null;
        private List<Map<String, Object>> buildingList = null;
        private boolean isRunning = false;
        private int currentTask = 0;
        private int totalTask = 0;
        private int lastProgress = 0;

        private class Task {
            public UUID uuid;
            public String weekName;
            public int weekCode;
            public String weekInfo;
            public int status;//0:pending, 1:downloading, -1:cancel
            public Map<String, Object> weekMap;
        }

        public DownloadHelper(List<Map<String, Object>> buildingList) {
            this.buildingList = buildingList;
            this.taskList = new ArrayList<>();
        }

        public UUID addTask(Map<String, Object> weekMap) {
            Task task = new Task();
            task.uuid = UUID.randomUUID();
            task.status = 0;
            task.weekName = (String) weekMap.get("week_name");
            task.weekCode = (Integer) weekMap.get("week_code");
            task.weekInfo = (String) weekMap.get("week_info");
            task.weekMap = weekMap;
            if(!isRunning){
                taskList.clear();
                currentTask = 0;
            }

            taskList.add(task);
            Log.d(ConstResource.APP_DEBUG_TAG, "Add task " + task.weekName
                    + " with id " + task.uuid.toString());
            Log.d(ConstResource.APP_DEBUG_TAG, "Total task " + Integer.toString(taskList.size()));


            totalTask += buildingList.size();

            updateProgress(0, -1);
            return task.uuid;
        }

        private void updateProgress(int currentTaskId, int currentProgress) {
            double progress = progressBar.getMax();
            if (currentProgress != -1) {
                lastProgress = buildingList.size() * currentTaskId + currentProgress;
            }
            progress = progress * lastProgress / totalTask;
            progressBar.setProgress((int) progress);

            downloadDetail.setText(String.format(
                            getString(R.string.download_detail_format),
                            lastProgress,
                            totalTask,
                            totalTask,
                            lastProgress,
                            totalTask - lastProgress,
                            taskList.get(currentTask).weekName,
                            taskList.size() - currentTaskId - 1,
                            100 * lastProgress / totalTask)
            );
        }

        private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Task curTask;
                switch (msg.what) {
                    case 1: //Start download
                        curTask = taskList.get(msg.getData().getInt("task_position"));
                        curTask.status = 1;
                        curTask.weekMap.put("status", downloadStatus.DOWNLOADING);
                        ((DownloadAdapter) listView.getAdapter()).notifyDataSetChanged();
                        break;
                    case 2: //Finish download
                        curTask = taskList.get(msg.getData().getInt("task_position"));
                        curTask.status = 2;
                        curTask.weekMap.put("status", downloadStatus.WAIT_TO_UPDATE);
                        ((DownloadAdapter) listView.getAdapter()).notifyDataSetChanged();
                        break;
                    case 3: //Update progress
                        updateProgress(msg.getData().getInt("task_position")
                                , msg.getData().getInt("task_finished"));
                }
            }
        };

        Runnable runnable = new Runnable() {
            private void sendMsg(int type, Bundle bundle) {
                Message message = new Message();
                message.what = type;
                message.setData(bundle);
                handler.sendMessage(message);
            }

            private void sendStartDownload(int position) {
                Bundle bundle = new Bundle();
                bundle.putInt("task_position", position);
                sendMsg(1, bundle);
            }

            private void sendFinishDownload(int position) {
                Bundle bundle = new Bundle();
                bundle.putInt("task_position", position);
                sendMsg(2, bundle);
            }

            private void sendProgress(int position, int buildingFinished) {
                Bundle bundle = new Bundle();
                bundle.putInt("task_position", position);
                bundle.putInt("task_finished", buildingFinished);
                sendMsg(3, bundle);
            }

            @Override
            public void run() {

                Log.d(ConstResource.APP_DEBUG_TAG, "Newer thread");
                //Loop to handle tasks
                while (taskList.size() > currentTask) {
                    if (taskList.get(currentTask).status == 0) {
                        sendStartDownload(currentTask);
                        Task task = taskList.get(currentTask);
                        String url = ConstResource.WEB_BASE_URL();
                        url += "&" + ConstResource.WEB_DATA_QUERY_PARA;
                        url += "&" + ConstResource.WEB_WEEK_PARA + "=" + task.weekCode;
                        for (Map<String, Object> building : buildingList) {
                            try {
                                String curUrl = url + "&" + ConstResource.WEB_BUILDING_PARA
                                        + "=" + building.get("building_code");
                                Log.d(ConstResource.APP_DEBUG_TAG, curUrl);
                                Elements rows = Jsoup.parse(new ByteArrayInputStream(Jsoup
                                                .connect(curUrl).timeout(10000).execute()
                                                .bodyAsBytes()), ConstResource.WEB_CHARSET,
                                        ConstResource.WEB_BASE_URL())
                                        .select("table[width=90%]")
                                        .select("table[cellpadding=2]") //Get table elements
                                        .first()                        //Get table
                                        .children()                     //Get tBody elements
                                        .first()                        //Get tBody
                                        .children();                    //Get row elements
                                if (rows.size() == 2) {
                                    //No Elements
                                } else {
                                    int count = rows.size() - 2;
                                    for (int i = 0; i < count; i++) {
                                        //Get a room a week's data
                                        Elements cells = rows.get(i + 2).children();
                                        int cellCount = cells.size();
                                        if (cellCount == (1 + 7 * 6)) {
                                            String studyRoomName = cells.first().text();
                                            int[] status = new int[6];
                                            for (int j = 1; j <= 7; j++) {
                                                // One Day's Info
                                                for (int k = 1; k <= 6; k++) {
                                                    Element cell = cells.get(j * k);
                                                    String typeOfType = cell.children()
                                                            .first().attr("color");

                                                    if (typeOfType.contains("00dd00")) {
                                                        // Free for study
                                                        status[k - 1] = 1;
                                                    } else {
                                                        // Busy for study
                                                        status[k - 1] = 0;
                                                    }
                                                }
                                                DatabaseManager.get(getApplication()).insertRoomDay(
                                                        studyRoomName, task.weekCode,
                                                        j, (int) building.get("building_id"),
                                                        status
                                                );
                                            }
                                        } else {
                                            // Error cell count.
                                        }
                                    }
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            sendProgress(currentTask, (int) building.get("building_id") + 1);
                        }
                        int cache = (int) (Calendar.getInstance().getTimeInMillis()
                                / 1000 / 60 / 60 / 24);
                        task.weekMap.put("week_cache", cache);
                        task.weekMap.put("week_info", getString(R.string.last_cached)
                                + new SimpleDateFormat("yyyy-MM-dd")
                                .format(new Date((long) cache * 1000 * 60 * 60 * 24)));
                        DatabaseManager.get(getApplication()).updateCacheHistory(task.weekCode, cache);
                        sendFinishDownload(currentTask);
                        currentTask++; //Set to next task
                    }
                }
                totalTask = 0;
                lastProgress = 0;
                isRunning = false;
                downloader_isRunning = false;
            }
        };

        public void start() {
            if (isRunning) {
                return;
            }
            new Thread(runnable).start();
            isRunning = true;
            downloader_isRunning = true;
        }
    }

    private DownloadHelper getDownloadHelper() {
        if (downloadHelper == null) {
            downloadHelper = new DownloadHelper(databaseManager.getBuildingList());
        }
        return downloadHelper;
    }

    private final class DownloadAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private List<Map<String, Object>> data;
        private DownloadHelper downloadHelper;
        private DownloadAdapter adapter;

        private final class ViewHolder {
            public TextView weekName;
            public TextView weekInfo;
            public Button weekButton;
        }

        public DownloadAdapter(Context ctx, List<Map<String, Object>> data, DownloadHelper downloadHelper) {
            inflater = LayoutInflater.from(ctx);
            this.adapter = this;
            this.data = data;
            this.downloadHelper = downloadHelper;
            for (Map<String, Object> week : data) {
                int cache = (int) week.get("week_cache");
                if (cache == 0) {
                    week.put("status", downloadStatus.WAIT_TO_DOWNLOAD);
                } else {
                    week.put("status", downloadStatus.WAIT_TO_UPDATE);
                }

            }

        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_download_list, null);
                holder.weekName = (TextView) convertView.findViewById(R.id.week_name);
                holder.weekInfo = (TextView) convertView.findViewById(R.id.week_info);
                holder.weekButton = (Button) convertView.findViewById(R.id.button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.weekName.setText((String) data.get(position).get("week_name"));
            holder.weekInfo.setText((String) data.get(position).get("week_info"));
            switch ((downloadStatus) data.get(position).get("status")) {
                case WAIT_TO_DOWNLOAD:
                    holder.weekButton.setEnabled(true);
                    holder.weekButton.setText(R.string.download);
                    holder.weekButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadHelper.addTask(data.get(position));
                            data.get(position).put("status", downloadStatus.WAIT_IN_QUEUE);
                            adapter.notifyDataSetChanged();
                            downloadHelper.start();
                        }
                    });
                    break;
                case WAIT_TO_UPDATE:
                    holder.weekButton.setEnabled(true);
                    holder.weekButton.setText(R.string.update);
                    holder.weekButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadHelper.addTask(data.get(position));
                            data.get(position).put("status", downloadStatus.WAIT_IN_QUEUE);
                            adapter.notifyDataSetChanged();
                            downloadHelper.start();
                        }
                    });
                    break;
                case WAIT_IN_QUEUE:
                    holder.weekButton.setEnabled(false);
                    holder.weekButton.setText(R.string.pending);
                    holder.weekButton.setOnClickListener(null);
                    break;
                case DOWNLOADING:
                    holder.weekButton.setEnabled(false);
                    holder.weekButton.setText(R.string.downloading);
                    holder.weekButton.setOnClickListener(null);
                    break;
            }

            return convertView;
        }
    }


    private void showDownLoadingProgress() {
        if (!isDownloadingProgressShowed) {
            isDownloadingProgressShowed = true;
            viewSwitcher.showNext();
        }
    }

    private void hideDownLoadingProgress() {
        if (isDownloadingProgressShowed) {
            isDownloadingProgressShowed = false;
            viewSwitcher.showPrevious();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_data_management, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }
}
