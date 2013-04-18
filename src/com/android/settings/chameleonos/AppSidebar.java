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

import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AppSidebar extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {
    private static final String TAG = "PowerMenu";

    private static final String KEY_ENABLED = "sidebar_enable";
    private static final String KEY_SORT_TYPE = "sidebar_sort_type";

    private CheckBoxPreference mEnabledPref;
    ListPreference mSortTypePref;
    private CheckBoxPreference mProfilesPref;
    private CheckBoxPreference mAirplanePref;
    private CheckBoxPreference mUserPref;
    private CheckBoxPreference mSoundPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_sidebar_settings);

        mEnabledPref = (CheckBoxPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.APP_SIDE_BAR_ENABLED, 0) == 1));

        PreferenceScreen prefSet = getPreferenceScreen();
        mSortTypePref = (ListPreference) prefSet.findPreference(KEY_SORT_TYPE);
        mSortTypePref.setOnPreferenceChangeListener(this);
        int sortyTypeValue = Settings.System.getInt(getContentResolver(), Settings.System.APP_SIDEBAR_SORT_TYPE, 0);
        mSortTypePref.setValue(String.valueOf(sortyTypeValue));
        updateSortTypeSummary(sortyTypeValue);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSortTypePref) {
            int sortyTypeValue = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.APP_SIDEBAR_SORT_TYPE, sortyTypeValue);
            updateSortTypeSummary(sortyTypeValue);
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
                    Settings.System.APP_SIDE_BAR_ENABLED,
                    value ? 1 : 0);
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private void updateSortTypeSummary(int value) {
        mSortTypePref.setSummary(mSortTypePref.getEntries()[mSortTypePref.findIndexOfValue("" + value)]);
        Settings.System.putInt(getContentResolver(),
                Settings.System.APP_SIDEBAR_SORT_TYPE, value);
    }
}
