/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.fragments;

import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.ElementTypeManipulator;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulatorFactory;
import org.opendatakit.tables.utils.PreferenceUtil;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;

public class ColumnPreferenceFragment extends AbsTableLevelPreferenceFragment {

  String mElementKey = null;
  static String COL_ELEM_KEY = "COLUMN_ELEMENT_KEY";

  private static final String TAG =
      ColumnPreferenceFragment.class.getSimpleName();

  public void onAttach(android.app.Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableLevelPreferencesActivity)) {
      throw new IllegalStateException("fragment must be attached to "
          + TableLevelPreferencesActivity.class.getSimpleName());
    }
  }

  /**
   * Get the {@link TableLevelPreferencesActivity} associated with this
   * activity.
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferenceActivity() {
    TableLevelPreferencesActivity result =
        (TableLevelPreferencesActivity) this.getActivity();
    return result;
  }

  /**
   * Retrieve the {@link ColumnDefinition} associated with the column this
   * activity is displaying.
   * 
   * @return
   */
  ColumnDefinition retrieveColumnDefinition() {
    TableLevelPreferencesActivity activity = retrieveTableLevelPreferenceActivity();
    String elementKey = mElementKey;
    if (mElementKey == null) {
      elementKey = activity.getElementKey();
    }
    try {
      OrderedColumns orderedDefns = activity.getColumnDefinitions();
      ColumnDefinition result = orderedDefns.find(elementKey);
      return result;
    } catch ( IllegalArgumentException e ) {
      WebLogger.getLogger(activity.getAppName()).e(
          TAG,
          "[retrieveColumnDefinition] did not find column for element key: " +
              elementKey);
      return null;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null && savedInstanceState.containsKey(COL_ELEM_KEY)) {
      mElementKey = savedInstanceState.getString(COL_ELEM_KEY);
    }
    this.addPreferencesFromResource(R.xml.preference_column);
  }

  @Override
  public void onResume() {
    super.onResume();
    try {
      this.initializeAllPreferences();
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mElementKey = this.retrieveColumnDefinition().getElementKey();
    outState.putString(COL_ELEM_KEY, mElementKey);
  }

  void initializeAllPreferences() throws ServicesAvailabilityException {
    this.initializeColumnType();
    this.initializeColumnWidth();
    this.initializeDisplayName();
    this.initializeElementKey();
    this.initializeElementName();
    this.initializeColorRule();
  }

  private void initializeDisplayName() throws ServicesAvailabilityException {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.DISPLAY_NAME);

    String rawDisplayName;
    DbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName());
      rawDisplayName = ColumnUtil.get().getRawDisplayName(Tables.getInstance(), getAppName(), 
          db, getTableId(), 
          this.retrieveColumnDefinition().getElementKey());
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }

    pref.setSummary(rawDisplayName);

  }

  private void initializeElementKey() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_KEY);
    pref.setSummary(this.retrieveColumnDefinition().getElementKey());
  }

  private void initializeColumnType() {
    EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.TYPE);
    ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance(this.getAppName());
    ITypeManipulatorFragment r = m.getDefaultRenderer(this.retrieveColumnDefinition().getType());
    pref.setSummary(r.getElementTypeDisplayLabel());
  }

  private void initializeElementName() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_NAME);
    pref.setSummary(this.retrieveColumnDefinition().getElementName());
  }

  private void initializeColumnWidth() throws ServicesAvailabilityException {
    TableLevelPreferencesActivity activity = retrieveTableLevelPreferenceActivity();
    final String appName = activity.getAppName();
    final EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.WIDTH);
    int columnWidth = PreferenceUtil.getColumnWidth(getActivity(),
        getAppName(), getTableId(),
        retrieveColumnDefinition().getElementKey());
    pref.setSummary(Integer.toString(columnWidth));

    pref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(
          Preference preference,
          Object newValue) {
        String newValueStr = (String) newValue;
        Integer newWidth = Integer.parseInt(newValueStr);
        if (newWidth > LocalKeyValueStoreConstants.Spreadsheet.MAX_COL_WIDTH) {
          WebLogger.getLogger(appName).e(TAG, "column width bigger than allowed, doing nothing");
          return false;
        }
        PreferenceUtil.setColumnWidth(
            getActivity(), getAppName(), getTableId(),
            retrieveColumnDefinition().getElementKey(),
            newWidth);
        pref.setSummary(Integer.toString(newWidth));
        return true;
      }
    });

  }
  
  private void initializeColorRule() {
    Preference pref =
        this.findPreference(Constants.PreferenceKeys.Column.COLOR_RULES);
    pref.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
      
      @Override
      public boolean onPreferenceClick(Preference preference) {
        TableLevelPreferencesActivity activity =
            (TableLevelPreferencesActivity) getActivity();
        activity.showColorRuleListFragment(
            retrieveColumnDefinition().getElementKey(),
            ColorRuleGroup.Type.COLUMN);
        return true;
      }
      
    });
  }

}
