
/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.IOException;


public class LoadingActivity extends Activity implements GestureDetector.OnGestureListener {
    enum ButtonMode{
        FirstRunTip,

    }
    private ViewSwitcher vs = null;
    private LoadingActivity act = null;
    private GestureDetector gd = null;
    private boolean buttonShowed = false;
    private ButtonMode buttonMode = ButtonMode.FirstRunTip;
    private SharedPreferences.Editor spe = null;
    private DatabaseManager databaseManager = null;
    private TextView statusText = null;
    private boolean startUpThreadRunning = false;

    public enum MessageLevel {
        Verbose,
        Information,
        Error
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        Log.d(ConstResource.APP_DEBUG_TAG, "App Started");
        vs = (ViewSwitcher) findViewById(R.id.loading_view_flipper);
        statusText = (TextView) findViewById(R.id.loading_status_information);

        act = this;
        gd = new GestureDetector(this, this);

        SharedPreferences sp = getApplication().getSharedPreferences(
                ConstResource.APP_BASIC_CONFIG_FILE, MODE_PRIVATE + MODE_APPEND);
        spe = sp.edit();

        //Exit event
        findViewById(R.id.loading_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.finish();
            }
        });



        //Hide the buttons on start up.
        hideButtons();

        //Get database
        databaseManager = DatabaseManager.get(getApplication());

        //Check for first run
        if (!sp.getBoolean(ConstResource.CONFIG_INITIALIZED, false)) {
            buttonMode = ButtonMode.FirstRunTip;
            showButtons();
            return;
        }

        //Check for Start up run
        if(!startUpThreadRunning){
            startUpThreadRunning = true;
            startupRun();
        }


    }

    private void hideButtons() {
        if (buttonShowed) {
            vs.showPrevious();
            buttonShowed = false;
        }
    }

    private void showButtons() {
        switch (buttonMode) {
            case FirstRunTip:
                findViewById(R.id.loading_continue).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideButtons();
                        firstRunHelper.run(spe, databaseManager, act);
                    }
                });
                break;
        }
        if (!buttonShowed) {
            vs.showNext();
            buttonShowed = true;
        }
    }

    public void showMessage(int msg_id, MessageLevel level) {
        switch (level) {
            case Verbose:
                break;
            case Information:
                statusText.setTextColor(
                        getResources().getColor(R.color.loading_text_information));
                statusText.setText(msg_id);
                break;
            case Error:
                statusText.setTextColor(
                        getResources().getColor(R.color.loading_text_error));
                statusText.setText(msg_id);
                break;
        }
    }


    public void startupRun(){
        startActivity(new Intent(this,MainActivity.class));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("buttonShowed", buttonShowed);
        outState.putBoolean("startUpThreadRunning",startUpThreadRunning);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        buttonShowed = savedInstanceState.getBoolean("buttonShowed");
        startUpThreadRunning =savedInstanceState.getBoolean("startUpThreadRunning");
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //TODO: Change the flip mode to "Other" use.
        if ((e2.getY() - e1.getY()) < -80.0) {
            showButtons();
        } else if ((e2.getY() - e1.getY()) > 80.0) {
            hideButtons();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gd.onTouchEvent(event);
    }
}


class firstRunHelper {
    static final int FIRST_RUN_NOTICE = 1;
    static final int FIRST_RUN_FINISH = 2;
    static final int FIRST_RUN_CHOOSE = 3;
    static final int FIRST_RUN_ERROR = 4;

    static final String FIRST_RUN_ERROR_KEY = "err_key";
    static final String FIRST_RUN_NOTICE_KEY = "notice_key";

    static int runProcess = 0;

    static SharedPreferences.Editor spe;
    static DatabaseManager db;
    static LoadingActivity act;

    private static Handler handler = new Handler() {

        private void finishFirstRun() {
            Log.d(ConstResource.APP_DEBUG_TAG, "First Run Procedure Finish");
            spe.putBoolean(ConstResource.CONFIG_INITIALIZED, true);
            spe.commit();
            act.startupRun();
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FIRST_RUN_NOTICE:
                    act.showMessage(msg.getData().getInt(FIRST_RUN_NOTICE_KEY),
                            LoadingActivity.MessageLevel.Information);
                    break;
                case FIRST_RUN_FINISH:
                    finishFirstRun();
                    break;
                case FIRST_RUN_CHOOSE:
                    break;
                case FIRST_RUN_ERROR:
                    act.showMessage(msg.getData().getInt(FIRST_RUN_ERROR_KEY),
                            LoadingActivity.MessageLevel.Error);
                    break;
                default:
                    Log.d(ConstResource.APP_DEBUG_TAG, "Unknown message from thread.");
            }

        }
    };


    private static Thread thread = new Thread(new Runnable() {


        private void sendMsg(int type, Bundle bundle) {
            Message message = new Message();
            message.what = type;
            message.setData(bundle);
            handler.sendMessage(message);
        }

        private void sendErrorMsg(int err_id) {
            Bundle bundle = new Bundle();
            bundle.putInt(FIRST_RUN_ERROR_KEY, err_id);
            sendMsg(FIRST_RUN_ERROR, bundle);
        }

        private void sendNoticeMsg(int notice_id) {
            Bundle bundle = new Bundle();
            bundle.putInt(FIRST_RUN_NOTICE_KEY, notice_id);
            sendMsg(FIRST_RUN_NOTICE, bundle);
        }

        private void sendFinishMsg() {
            sendMsg(FIRST_RUN_FINISH, null);
        }


        @Override
        synchronized public void run() {
            try {
                Log.d(ConstResource.APP_DEBUG_TAG, "First Run Procedure Start");
                if (!WeekList.get().webInitialize()) {
                    //Internet Error, retry or exit.
                    sendErrorMsg(R.string.loading_err_network_error);
                    return;
                }
                db.updateWeekList();
                if (!BuildingList.get().webInitialize()) {
                    //Internet Error, retry or exit.
                    sendErrorMsg(R.string.loading_err_network_error);
                    return;
                }
                db.updateBuildingList();
                sendFinishMsg();
            } catch (IOException e) {
                sendErrorMsg(R.string.loading_err_network_error);
                Log.d(ConstResource.APP_DEBUG_TAG, "Internet error", e);
            }
        }
    });

    public static void run(SharedPreferences.Editor editor, DatabaseManager databaseManager, LoadingActivity loadingActivity) {

        spe = editor;
        db = databaseManager;
        act = loadingActivity;

        if (thread.getState() == Thread.State.NEW) {
            thread.start();
        }

    }
}
