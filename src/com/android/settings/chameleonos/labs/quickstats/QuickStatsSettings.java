/*
 * Copyright (C) 2013 The ChameleonOS Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.chameleonos.labs.quickstats;

import android.annotation.ChaosLab;
import android.annotation.ChaosLab.Classification;
import android.app.ActionBar;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.chameleonos.QuickSettingsUtil;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QuickStatsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "QuickStatsSettings";
    private static final String KEY_ENABLED = "quick_stats_enabled";

    private SwitchPreference mEnabledPref;
    private CharSequence mPreviousTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_stats_settings);

        mEnabledPref = (SwitchPreference) findPreference(KEY_ENABLED);
        mEnabledPref.setChecked((Settings.System.getInt(getContentResolver(),
                Settings.System.QUICK_STATS_ENABLED, 0) == 1));
        mEnabledPref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        final ActionBar bar = getActivity().getActionBar();
        mPreviousTitle = bar.getTitle();
        bar.setTitle(R.string.qstats_title);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().getActionBar().setTitle(mPreviousTitle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        QuickSettingsUtil.updateAvailableTiles(getActivity());
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnabledPref) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.QUICK_STATS_ENABLED,
                ((Boolean) newValue).booleanValue() ? 1 : 0);
            return true;
        }
        return false;
    }
}
