/*
 * Copyright (C) 2012 The CyanogenMod project
 * Copyright (C) 2013 The CyanogenMod project
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

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_NAVIGATION_BAR = "navigation_bar";
    private static final String KEY_NAVIGATION_RING = "navigation_ring";
    private static final String KEY_NAVIGATION_BAR_CATEGORY = "navigation_bar_category";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_STATUS_BAR = "status_bar";
    private static final String KEY_QUICK_SETTINGS = "quick_settings_panel";
    private static final String KEY_NOTIFICATION_DRAWER = "notification_drawer";
    private static final String KEY_UI_DISPLAY = "ui_display";

    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;
    private boolean mIsPrimary;
    private ListPreference mUIDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mUIDisplay = (ListPreference) findPreference(KEY_UI_DISPLAY);
        int uiDisplay = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.UI_DISPLAY_STATE, 2);
        mUIDisplay.setValue(String.valueOf(uiDisplay));
        mUIDisplay.setSummary(mUIDisplay.getEntry());
        mUIDisplay.setOnPreferenceChangeListener(this);

        // Determine which user is logged in
        mIsPrimary = UserHandle.myUserId() == UserHandle.USER_OWNER;
        if (mIsPrimary) {
            // Primary user only preferences
            // Battery lights
            mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
            if (mBatteryPulse != null) {
                if (getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveBatteryLed) == false) {
                    prefScreen.removePreference(mBatteryPulse);
                } else {
                    updateBatteryPulseDescription();
                }
            }

            // Only show the hardware keys config on a device that does not have a navbar
            // and the navigation bar config on phones that has a navigation bar
            boolean removeNavbar = false;
            boolean hasSystemBar = false;
            IWindowManager windowManager = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
            try {
                hasSystemBar = windowManager.hasSystemNavBar();
                if (!windowManager.hasNavigationBar() && !hasSystemBar) {
                    removeNavbar = true;
                }
            } catch (RemoteException e) {
                // Do nothing
            }

            // Act on the above
            if (removeNavbar && (uiDisplay != 2)) {
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
                prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_CATEGORY));
            }
        } else {
            // Secondary user is logged in, remove all primary user specific preferences
            prefScreen.removePreference(findPreference(KEY_BATTERY_LIGHT));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_RING));
            prefScreen.removePreference(findPreference(KEY_NAVIGATION_BAR_CATEGORY));
            prefScreen.removePreference(findPreference(KEY_STATUS_BAR));
            prefScreen.removePreference(findPreference(KEY_QUICK_SETTINGS));
            prefScreen.removePreference(findPreference(KEY_NOTIFICATION_DRAWER));
        }

        // Preferences that applies to all users
        // Notification lights
        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                prefScreen.removePreference(mNotificationPulse);
            } else {
                updateLightPulseDescription();
            }
        }

        // Don't display the lock clock preference if its not installed
        removePreferenceIfPackageNotInstalled(findPreference(KEY_LOCK_CLOCK));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mUIDisplay) {
            int uiDisplay = Integer.valueOf((String) newValue);
            int index = mUIDisplay.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.UI_DISPLAY_STATE, uiDisplay);
            mUIDisplay.setSummary(mUIDisplay.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    private void updateBatteryPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.notification_light_disabled));
        }
     }

    @Override
    public void onResume() {
        super.onResume();

        // All users
        updateLightPulseDescription();

        // Primary user only
        if (mIsPrimary) {
            updateBatteryPulseDescription();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri = ((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName = matcher.find() ? matcher.group(1) : null;
        if (packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "package " + packageName + " not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }
}
