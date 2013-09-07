/*
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

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SettingsPreferenceFragment;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "LockscreenInterface";

    private static final int REQUEST_CODE_BG_WALLPAPER = 1024;

    private static final int LOCKSCREEN_WALLPAPER_CUSTOM = 0;
    private static final int LOCKSCREEN_DEFAULT_WALLPAPER = 1;

    private static final String KEY_ALWAYS_BATTERY_PREF = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_LOCKSCREEN_MAXIMIZE_WIDGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_HIDE_INITIAL_PAGE_HINTS = "lockscreen_hide_initial_page_hints";
    private static final String KEY_LOCKSCREEN_WALLPAPER = "default_lock_wallpaper";
    private static final String WALLPAPER_IMAGE_PATH  = "/data/system/theme/wallpaper";
    private static final String WALLPAPER_FILE_NAME = "default_lock_wallpaper.jpg";

    private ListPreference mCustomWallpaper;
    private ListPreference mBatteryStatus;
    private CheckBoxPreference mMaximizeWidgets;
    private CheckBoxPreference mLockscreenHideInitialPageHints;

    private File mWallpaperImage;
    private File mWallpaperTemporary;

    public boolean hasButtons() {
        return !getResources().getBoolean(com.android.internal.R.bool.config_showNavigationBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        mBatteryStatus = (ListPreference) findPreference(KEY_ALWAYS_BATTERY_PREF);
        if (mBatteryStatus != null) {
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }

        mMaximizeWidgets = (CheckBoxPreference)findPreference(KEY_LOCKSCREEN_MAXIMIZE_WIDGETS);
        if (!Utils.isPhone(getActivity())) {
            getPreferenceScreen().removePreference(mMaximizeWidgets);
            mMaximizeWidgets = null;
        } else {
            mMaximizeWidgets.setOnPreferenceChangeListener(this);
        }

        mLockscreenHideInitialPageHints = (CheckBoxPreference)findPreference(KEY_LOCKSCREEN_HIDE_INITIAL_PAGE_HINTS);
        if (!Utils.isPhone(getActivity())) {
            getPreferenceScreen().removePreference(mLockscreenHideInitialPageHints);
            mLockscreenHideInitialPageHints = null;
        } else {
            mLockscreenHideInitialPageHints.setOnPreferenceChangeListener(this);
        }

        PreferenceScreen lockscreenButtons = (PreferenceScreen) findPreference(KEY_LOCKSCREEN_BUTTONS);
        if (!hasButtons()) {
            getPreferenceScreen().removePreference(lockscreenButtons);
        }
        mCustomWallpaper = (ListPreference) findPreference(KEY_LOCKSCREEN_WALLPAPER);
        mCustomWallpaper.setOnPreferenceChangeListener(this);
        updateCustomWallpaperSummary();

        mWallpaperImage = new File(WALLPAPER_IMAGE_PATH + "/default_lock_wallpaper.jpg");
        mWallpaperTemporary = new File(WALLPAPER_IMAGE_PATH + "/default_lock_wallaper.tmp");

    }

    private void updateCustomWallpaperSummary() {
        int resId;
        if (!lockWallpaperExists()) {
            resId = R.string.lockscreen_default_wallpaper;
            mCustomWallpaper.setValueIndex(LOCKSCREEN_DEFAULT_WALLPAPER);
        } else {
            resId = R.string.lockscreen_wallpaper_custom;
            mCustomWallpaper.setValueIndex(LOCKSCREEN_WALLPAPER_CUSTOM);
        }
        mCustomWallpaper.setSummary(getResources().getString(resId));
    }

    @Override
    public void onResume() {
        super.onResume();

        ContentResolver cr = getActivity().getContentResolver();
        if (mBatteryStatus != null) {
            int batteryStatus = Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }

        if (mMaximizeWidgets != null) {
            mMaximizeWidgets.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, 0) == 1);
        }

        if (mLockscreenHideInitialPageHints != null) {
            mLockscreenHideInitialPageHints.setChecked(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_HIDE_INITIAL_PAGE_HINTS, 0) == 1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BG_WALLPAPER) {
            int hintId;

            if (resultCode == Activity.RESULT_OK) {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.renameTo(mWallpaperImage);
                }
                hintId = R.string.lockscreen_wallpaper_result_successful;
                updateCustomWallpaperSummary();
            } else {
                if (mWallpaperTemporary.exists()) {
                    mWallpaperTemporary.delete();
                }
                hintId = R.string.lockscreen_wallpaper_result_not_successful;
            }
            Toast.makeText(getActivity(),
                    getResources().getString(hintId), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();

        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mMaximizeWidgets) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MAXIMIZE_WIDGETS, value ? 1 : 0);
            return true;
        } else if (preference == mLockscreenHideInitialPageHints) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_HIDE_INITIAL_PAGE_HINTS, value ? 1 : 0);
            return true;
        } else if (preference == mCustomWallpaper) {
            int selection = mCustomWallpaper.findIndexOfValue(objValue.toString());
            return handleWallpaperSelection(selection);
        }
        return false;
    }
    private boolean handleWallpaperSelection(int selection) {
         if (selection == LOCKSCREEN_WALLPAPER_CUSTOM) {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
            intent.setType("image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("scale", true);
            intent.putExtra("scaleUpIfNeeded", false);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());

            final Display display = getActivity().getWindowManager().getDefaultDisplay();
            final Rect rect = new Rect();
            final Window window = getActivity().getWindow();

            window.getDecorView().getWindowVisibleDisplayFrame(rect);

            int statusBarHeight = rect.top;
            int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
            int titleBarHeight = contentViewTop - statusBarHeight;
            boolean isPortrait = getResources().getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            int width = display.getWidth();
            int height = display.getHeight() - titleBarHeight;

            intent.putExtra("aspectX", isPortrait ? width : height);
            intent.putExtra("aspectY", isPortrait ? height : width);

            try {
                mWallpaperTemporary.createNewFile();
                mWallpaperTemporary.setWritable(true, false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mWallpaperTemporary));
                intent.putExtra("return-data", false);
                getActivity().startActivityFromFragment(this, intent, REQUEST_CODE_BG_WALLPAPER);
            } catch (IOException e) {
                // Do nothing here
            } catch (ActivityNotFoundException e) {
                // Do nothing here
            }
        } else if (selection == LOCKSCREEN_DEFAULT_WALLPAPER) {
                if (mWallpaperImage.exists()) {
                    mWallpaperImage.delete();
                }
                updateCustomWallpaperSummary();
        }

        return false;
    }

    private boolean lockWallpaperExists() {
        File wallpaper = new File(WALLPAPER_IMAGE_PATH + File.separator + WALLPAPER_FILE_NAME);
        return wallpaper.exists();
    }
}
