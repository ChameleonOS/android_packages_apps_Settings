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
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.util.chaos.QStatsConstants;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.chameleonos.DraggableGridView;
import com.android.settings.chameleonos.QuickSettingsTiles.OnRearrangeListener;
import com.android.settings.chameleonos.labs.quickstats.QuickStatsUtil.TileInfo;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

@ChaosLab(name="QuickStats", classification=Classification.NEW_CLASS)
public class QuickStatsTiles extends Fragment {

    private static final int MENU_RESET = Menu.FIRST;

    private DraggableGridView mDragView;
    private ViewGroup mContainer;
    private LayoutInflater mInflater;
    private Resources mSystemUiResources;
    private TileAdapter mTileAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity());
        mContainer = container;
        mContainer.setClipChildren(false);
        mContainer.setClipToPadding(false);
        mInflater = inflater;

        PackageManager pm = getActivity().getPackageManager();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        int panelWidth = getItemFromSystemUi("notification_panel_width", "dimen");
        if (panelWidth != 0) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(panelWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            mDragView.setLayoutParams(params);
        }
        int cellHeight = getItemFromSystemUi("quick_stats_cell_height", "dimen");
        if (cellHeight != 0) {
            mDragView.setCellHeight(cellHeight);
        }
        int cellGap = getItemFromSystemUi("quick_stats_cell_gap", "dimen");
        if (cellGap != 0) {
            mDragView.setCellGap(cellGap);
        }
        int columnCount = getItemFromSystemUi("quick_stats_num_columns", "integer");
        if (columnCount != 0) {
            mDragView.setColumnCount(columnCount);
        }
        mTileAdapter = new TileAdapter(getActivity());
        return mDragView;
    }

    private int getItemFromSystemUi(String name, String type) {
        if (mSystemUiResources != null) {
            int resId = (int) mSystemUiResources.getIdentifier(name, type, "com.android.systemui");
            if (resId > 0) {
                try {
                    if (type.equals("dimen")) {
                        return (int) mSystemUiResources.getDimension(resId);
                    } else {
                        return mSystemUiResources.getInteger(resId);
                    }
                } catch (NotFoundException e) {
                }
            }
        }
        return 0;
    }

    void genTiles() {
        mDragView.removeAllViews();
        ArrayList<String> tiles = QuickStatsUtil.getTileListFromString(
                QuickStatsUtil.getCurrentTiles(getActivity()));
        for (String tileindex : tiles) {
            TileInfo tile = QuickStatsUtil.TILES.get(tileindex);
            if (tile != null) {
                addTile(tile.getTitleResId(), tile.getIcon(), 0, false);
            }
        }
        addTile(R.string.profiles_add, null, R.drawable.ic_menu_add, false);
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(int titleId, String iconSysId, int iconRegId, boolean newTile) {
        View tileView = null;
        if (iconRegId != 0) {
            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
            final TextView name = (TextView) tileView.findViewById(R.id.tile_textview);
            name.setText(titleId);
            name.setCompoundDrawablesRelativeWithIntrinsicBounds(0, iconRegId, 0, 0);
        } else {
            if (mSystemUiResources != null && iconSysId != null) {
                int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
                if (resId > 0) {
                    try {
                        Drawable d = mSystemUiResources.getDrawable(resId);
                        tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
                        final TextView name = (TextView) tileView.findViewById(R.id.tile_textview);
                        name.setText(titleId);
                        name.setCompoundDrawablesRelativeWithIntrinsicBounds(null, d, null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        mDragView.addView(tileView, newTile ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        genTiles();
        mDragView.setOnRearrangeListener(new OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                ArrayList<String> tiles = QuickStatsUtil.getTileListFromString(
                        QuickStatsUtil.getCurrentTiles(getActivity()));
                String oldTile = tiles.get(oldIndex);
                tiles.remove(oldIndex);
                tiles.add(newIndex, oldTile);
                QuickStatsUtil.saveCurrentTiles(getActivity(),
                        QuickStatsUtil.getTileStringFromList(tiles));
            }
            @Override
            public void onDelete(int index) {
                ArrayList<String> tiles = QuickStatsUtil.getTileListFromString(
                        QuickStatsUtil.getCurrentTiles(getActivity()));
                tiles.remove(index);
                QuickStatsUtil.saveCurrentTiles(getActivity(),
                        QuickStatsUtil.getTileStringFromList(tiles));
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 != mDragView.getChildCount() - 1) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.tile_choose_title)
                .setAdapter(mTileAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int position) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<String> curr = QuickStatsUtil.getTileListFromString(
                                        QuickStatsUtil.getCurrentTiles(getActivity()));
                                curr.add(mTileAdapter.getTileId(position));
                                QuickStatsUtil.saveCurrentTiles(getActivity(),
                                        QuickStatsUtil.getTileStringFromList(curr));
                            }
                        }).start();
                        TileInfo info = QuickStatsUtil.TILES.get(mTileAdapter.getTileId(position));
                        addTile(info.getTitleResId(), info.getIcon(), 0, true);
                    }
                });
                builder.create().show();
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isPhone(getActivity())) {
            mContainer.setPadding(20, 0, 20, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetTiles();
                return true;
            default:
                return false;
        }
    }

    private void resetTiles() {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.tiles_reset_title);
        alert.setMessage(R.string.tiles_reset_message);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                QuickStatsUtil.resetTiles(getActivity());
                genTiles();
            }
        });
        alert.setNegativeButton(R.string.cancel, null);
        alert.create().show();
    }

    private static class TileAdapter extends ArrayAdapter<String> {
        private static class Entry {
            public final TileInfo tile;
            public final String tileTitle;
            public Entry(TileInfo tile, String tileTitle) {
                this.tile = tile;
                this.tileTitle = tileTitle;
            }
        }

        private Entry[] mTiles;

        public TileAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
            mTiles = new Entry[getCount()];
            loadItems(context.getResources());
            sortItems();
        }

        private void loadItems(Resources resources) {
            int index = 0;
            for (TileInfo t : QuickStatsUtil.TILES.values()) {
                mTiles[index++] = new Entry(t, resources.getString(t.getTitleResId()));
            }
        }

        private void sortItems() {
            final Collator collator = Collator.getInstance();
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            collator.setStrength(Collator.PRIMARY);
            Arrays.sort(mTiles, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return collator.compare(e1.tileTitle, e2.tileTitle);
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setEnabled(isEnabled(position));
            return v;
        }

        @Override
        public int getCount() {
            return QuickStatsUtil.TILES.size();
        }

        @Override
        public String getItem(int position) {
            return mTiles[position].tileTitle;
        }

        public String getTileId(int position) {
            return mTiles[position].tile.getId();
        }

        @Override
        public boolean isEnabled(int position) {
            String usedTiles = QuickStatsUtil.getCurrentTiles(
                    getContext());
            return !(usedTiles.contains(mTiles[position].tile.getId()));
        }
    }
}
