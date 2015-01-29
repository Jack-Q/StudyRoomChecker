

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright (c) 2015 Jack Q (QiaoBo#outlook.com)
 ~ >> Created at 2015 - 1 - 27 .
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package com.jackq.studyroomchecker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;




public class SettingsFragment extends PreferenceFragment {
    public SettingsFragment() {
        super();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        findPreference("manage_data").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        getActivity().startActivity(
                                new Intent(getActivity(), DataManagementActivity.class));
                        return false;
                    }
                });
        findPreference("update_list").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(android.R.string.dialog_alert_title);
                        builder.setIconAttribute(android.R.attr.alertDialogIcon);
                        builder.setMessage(R.string.preference_reset_data_tip);

                        builder.setPositiveButton(R.string.string_continue, new DialogInterface.OnClickListener() {
                            @Override
                            public synchronized void onClick(DialogInterface dialog, int which) {

                                SharedPreferences.Editor spe = getActivity().getApplication().getSharedPreferences(
                                        ConstResource.APP_BASIC_CONFIG_FILE, Context.MODE_PRIVATE + Context.MODE_APPEND).edit();
                                spe.putBoolean(ConstResource.CONFIG_INITIALIZED, false);
                                spe.commit();

//                                new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        try {
//                                            WeekList.get().webInitialize();
//                                            BuildingList.get().webInitialize();
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                });

                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                        return false;
                    }
                });

    }
}
