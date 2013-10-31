/*
 * Copyright (C) 2012 The CyanogenMod Project
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
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.util.chaos.QStatsUtils;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.android.internal.util.chaos.QStatsConstants.*;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QuickStatsUtil {
    private static final String TAG = "QuickStatsUtil";

    public static final Map<String, TileInfo> TILES;

    private static final Map<String, TileInfo> ENABLED_TILES = new HashMap<String, TileInfo>();
    private static final Map<String, TileInfo> DISABLED_TILES = new HashMap<String, TileInfo>();

    static {
        TILES = Collections.unmodifiableMap(ENABLED_TILES);
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_BATTERY, R.string.qstats_title_tile_battery,
                "com.android.systemui:drawable/ic_qs_battery_neutral"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_WIFI, R.string.qstats_title_tile_wifi,
                "com.android.systemui:drawable/ic_qs_wifi_4"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_PROCESSOR, R.string.qstats_title_tile_processor,
                "com.android.systemui:drawable/ic_qstats_processor"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_TEMPERATURE, R.string.qstats_title_tile_temperature,
                "com.android.systemui:drawable/ic_qstats_temperature"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_CALLS, R.string.qstats_title_tile_calls,
                "com.android.systemui:drawable/ic_qstats_calls"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_MESSAGING, R.string.qstats_title_tile_messaging,
                "com.android.systemui:drawable/ic_qstats_messaging"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_MEMORY, R.string.qstats_title_tile_memory,
                "com.android.systemui:drawable/ic_qstats_memory"));
        registerTile(new QuickStatsUtil.TileInfo(
                TILE_STORAGE, R.string.qstats_title_tile_storage,
                "com.android.systemui:drawable/ic_qstats_storage"));
    }

    private static void registerTile(QuickStatsUtil.TileInfo info) {
        ENABLED_TILES.put(info.getId(), info);
    }

    private static void removeTile(String id) {
        ENABLED_TILES.remove(id);
        DISABLED_TILES.remove(id);
        TILES_DEFAULT.remove(id);
    }

    private static void disableTile(String id) {
        if (ENABLED_TILES.containsKey(id)) {
            DISABLED_TILES.put(id, ENABLED_TILES.remove(id));
        }
    }

    private static void enableTile(String id) {
        if (DISABLED_TILES.containsKey(id)) {
            ENABLED_TILES.put(id, DISABLED_TILES.remove(id));
        }
    }

    private static synchronized void removeUnsupportedTiles(Context context) {
        // Don't show mobile data options if not supported
        if (!QStatsUtils.deviceSupportsMobileData(context)) {
            removeTile(TILE_CALLS);
            removeTile(TILE_MESSAGING);
        }
    }

    public static synchronized void updateAvailableTiles(Context context) {
        removeUnsupportedTiles(context);
    }

    public static boolean isTileAvailable(String id) {
        return ENABLED_TILES.containsKey(id);
    }

    public static String getCurrentTiles(Context context) {
        String tiles = Settings.System.getString(context.getContentResolver(),
                Settings.System.QUICK_STATS_TILES);
        if (tiles == null) {
            tiles = getDefaultTiles(context);
        }
        return tiles;
    }

    public static void saveCurrentTiles(Context context, String tiles) {
        Settings.System.putString(context.getContentResolver(),
                Settings.System.QUICK_STATS_TILES, tiles);
    }

    public static void resetTiles(Context context) {
        String defaultTiles = getDefaultTiles(context);
        Settings.System.putString(context.getContentResolver(),
               Settings.System.QUICK_STATS_TILES, defaultTiles);
    }

    public static String mergeInNewTileString(String oldString, String newString) {
        ArrayList<String> oldList = getTileListFromString(oldString);
        ArrayList<String> newList = getTileListFromString(newString);
        ArrayList<String> mergedList = new ArrayList<String>();

        // add any items from oldlist that are in new list
        for (String tile : oldList) {
            if (newList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // append anything in newlist that isn't already in the merged list to
        // the end of the list
        for (String tile : newList) {
            if (!mergedList.contains(tile)) {
                mergedList.add(tile);
            }
        }

        // return merged list
        return getTileStringFromList(mergedList);
    }

    public static ArrayList<String> getTileListFromString(String tiles) {
        return new ArrayList<String>(Arrays.asList(tiles.split("\\|")));
    }

    public static String getTileStringFromList(ArrayList<String> tiles) {
        if (tiles == null || tiles.size() <= 0) {
            return "";
        } else {
            String s = tiles.get(0);
            for (int i = 1; i < tiles.size(); i++) {
                s += TILE_DELIMITER + tiles.get(i);
            }
            return s;
        }
    }

    public static String getDefaultTiles(Context context) {
        removeUnsupportedTiles(context);
        return TextUtils.join(TILE_DELIMITER, TILES_DEFAULT);
    }

    public static class TileInfo {
        private String mId;
        private int mTitleResId;
        private String mIcon;

        public TileInfo(String id, int titleResId, String icon) {
            mId = id;
            mTitleResId = titleResId;
            mIcon = icon;
        }

        public String getId() {
            return mId;
        }

        public int getTitleResId() {
            return mTitleResId;
        }

        public String getIcon() {
            return mIcon;
        }
    }
}
