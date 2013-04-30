/*
 * Copyright (C) 2012 CyanogenMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.chameleonos;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AppSidebar extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = "PowerMenu";

    private static final String KEY_ENABLED = "sidebar_enable";
    private static final String KEY_TRANSPARENCY = "sidebar_transparency";
    private static final String KEY_SETUP_ITEMS = "sidebar_setup_items";
    private static final String KEY_POSITION = "sidebar_position";
    private static final String KEY_HIDE_LABELS = "sidebar_hide_labels";
    private static final String KEY_USE_TAB = "use_tab";
    private static final String KEY_TAB_POSITION = "tab_position";
    private static final String KEY_TAB_SIZE = "tab_size";

    private SwitchPreference mEnabledPref;
    private SeekBarDialogPreference mTransparencyPref;
    private ListPreference mPositionPref;
    private CheckBoxPreference mHideLabelsPref;
    private CheckBoxPreference mUseTabPref;
    private ListPreference mTabPositionPref;
    private ListPreference mTabSizePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_sidebar_settings);

        mEnabledPref = (SwitchPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_ENABLED, 0) == 1));

        mHideLabelsPref = (CheckBoxPreference) findPreference(KEY_HIDE_LABELS);
        mHideLabelsPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_DISABLE_LABELS, 0) == 1));

        mUseTabPref = (CheckBoxPreference) findPreference(KEY_USE_TAB);
        mUseTabPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_USE_TAB, 0) == 1));

        PreferenceScreen prefSet = getPreferenceScreen();
        mPositionPref = (ListPreference) prefSet.findPreference(KEY_POSITION);
        mPositionPref.setOnPreferenceChangeListener(this);
        int position = Settings.System.getInt(getContentResolver(), Settings.System.APP_SIDEBAR_POSITION, 0);
        mPositionPref.setValue(String.valueOf(position));
        updatePositionSummary(position);

        mTabPositionPref = (ListPreference) prefSet.findPreference(KEY_TAB_POSITION);
        mTabPositionPref.setOnPreferenceChangeListener(this);
        position = Settings.System.getInt(getContentResolver(), Settings.System.APP_SIDEBAR_TAB_POSITION, 0);
        mTabPositionPref.setValue(String.valueOf(position));
        updateTabPositionSummary(position);

        mTabSizePref = (ListPreference) prefSet.findPreference(KEY_TAB_SIZE);
        mTabSizePref.setOnPreferenceChangeListener(this);
        float size = Settings.System.getFloat(getContentResolver(), Settings.System.APP_SIDEBAR_TAB_SCALE, 1.5f);
        mTabSizePref.setValue(String.valueOf(size));
        updateTabSizeSummary(size);

        mTransparencyPref = (SeekBarDialogPreference) findPreference(KEY_TRANSPARENCY);
        mTransparencyPref.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_TRANSPARENCY, 0));
        mTransparencyPref.setOnPreferenceChangeListener(this);

        findPreference(KEY_SETUP_ITEMS).setOnPreferenceClickListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTransparencyPref) {
            int transparency = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_TRANSPARENCY, transparency);
            return true;
        } else if (preference == mPositionPref) {
            int position = Integer.valueOf((String) newValue);
            updatePositionSummary(position);
            return true;
        } else if (preference == mTabPositionPref) {
            int position = Integer.valueOf((String) newValue);
            updateTabPositionSummary(position);
            return true;
        } else if (preference == mTabSizePref) {
            float size = Float.valueOf((String) newValue);
            updateTabSizeSummary(size);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mEnabledPref) {
            value = mEnabledPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_ENABLED,
                    value ? 1 : 0);
        } else if (preference == mHideLabelsPref) {
            value = mHideLabelsPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_DISABLE_LABELS,
                    value ? 1 : 0);
        } else if (preference == mUseTabPref) {
            value = mUseTabPref.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_USE_TAB,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals(KEY_SETUP_ITEMS)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(new ComponentName("com.android.systemui",
                    "com.android.systemui.statusbar.sidebar.SidebarConfigurationActivity"));
            getActivity().startActivity(intent);
            return true;
        }
        return false;
    }

    private void updatePositionSummary(int value) {
        mPositionPref.setSummary(mPositionPref.getEntries()[mPositionPref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_POSITION, value);
    }

    private void updateTabPositionSummary(int value) {
        mTabPositionPref.setSummary(mTabPositionPref.getEntries()[mTabPositionPref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_TAB_POSITION, value);
    }

    private void updateTabSizeSummary(float value) {
        mTabSizePref.setSummary(mTabSizePref.getEntries()[mTabSizePref.findIndexOfValue("" + value)]);
        Settings.System.putFloat(getContentResolver(),
                Settings.System.APP_SIDEBAR_TAB_SCALE, value);
    }
}
