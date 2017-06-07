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

import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.JsonReader;
import org.apache.commons.lang3.StringUtils;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.PreferenceUtil;

import java.io.StringReader;
import java.net.ProtocolException;

/**
 * This class represents the preference pane you get from going to a table's preferences,
 * clicking columns and then selecting a column. It shows the column's Display Name, ELement Key,
 * Element Name and Column Type, and allows the user to edit the Column Width or edit the color
 * rules
 */
public class ColumnPreferenceFragment extends AbsTableLevelPreferenceFragment {

  // The element key
  private String mElementKey = null;

  // Tag used for putting things in the saved instance state bundle
  private static String COL_ELEM_KEY = "COLUMN_ELEMENT_KEY";

  // Used for logging
  private static final String TAG = ColumnPreferenceFragment.class.getSimpleName();

  /**
   * Called when the fragment is attached to a view, asserts that we were attached to a
   * TableLevelPreferencesActivity so that we can get the table id and app name out of it
   * @param context the context to execute in
   */
  public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof TableLevelPreferencesActivity)) {
      throw new IllegalStateException(
          "fragment must be attached to " + TableLevelPreferencesActivity.class.getSimpleName());
    }
  }

  /**
   * Get the {@link TableLevelPreferencesActivity} associated with this
   * activity.
   *
   * @return the activity that we're attached to
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferenceActivity() {
    return (TableLevelPreferencesActivity) this.getActivity();
  }

  /**
   * Retrieve the {@link ColumnDefinition} associated with the column this
   * activity is displaying.
   *
   * @return the column definitions for this table, taken from the activity we're attached to
   */
  ColumnDefinition retrieveColumnDefinition() {
    TableLevelPreferencesActivity activity = retrieveTableLevelPreferenceActivity();
    String elementKey = mElementKey;
    if (mElementKey == null) {
      elementKey = activity.getElementKey();
    }
    try {
      OrderedColumns orderedDefns = activity.getColumnDefinitions();
      return orderedDefns.find(elementKey);
    } catch (IllegalArgumentException e) {
      WebLogger.getLogger(activity.getAppName())
          .e(TAG, "[retrieveColumnDefinition] did not find column for element key: " + elementKey);
      return null;
    }
  }

  /**
   * Called when we resume, restores mElementKey (which column we represent) from the saved bundle
   * @param savedInstanceState a bundle that contains our saved element key
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null && savedInstanceState.containsKey(COL_ELEM_KEY)) {
      mElementKey = savedInstanceState.getString(COL_ELEM_KEY);
    }
    this.addPreferencesFromResource(R.xml.preference_column);
  }

  /**
   * Also called when we resume, it initializes all properties if possible
   */
  @Override
  public void onResume() {
    super.onResume();
    try {
      this.initializeAllPreferences();
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
  }

  /**
   * Called when the activity is about to go away, saves the element key in the saved state so it
   * can be restored when we're recreated
   * @param outState a bundle that will be saved
   */
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mElementKey = this.retrieveColumnDefinition().getElementKey();
    outState.putString(COL_ELEM_KEY, mElementKey);
  }

  /**
   * Internal helper method that sets up all of the preferences that get displayed to the user
   * @throws ServicesAvailabilityException if the database is down
   */
  void initializeAllPreferences() throws ServicesAvailabilityException {
    this.initializeColumnType();
    this.initializeColumnWidth();
    this.initializeDisplayName();
    this.initializeElementKey();
    this.initializeElementName();
    this.initializeColorRule();
  }

  /**
   * Handles initializing the "Display Name" preference to show to the user
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeDisplayName() throws ServicesAvailabilityException {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.DISPLAY_NAME);

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    String rawDisplayName;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(getAppName());
      rawDisplayName = ColumnUtil.get()
          .getRawDisplayName(dbInterface, getAppName(), db, getTableId(),
              this.retrieveColumnDefinition().getElementKey());
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(getAppName(), db);
      }
    }

    // For some reason rawDisplayName comes in as {"text":"real display name here"} so we need to
    // decode it
    JsonReader parser = new JsonReader(new StringReader(rawDisplayName));
    try {
      parser.beginObject();
      if (!parser.nextName().equals("text")) {
        throw new ProtocolException();
      }
      rawDisplayName = parser.nextString();
      parser.close();
    } catch (Throwable e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }

    pref.setSummary(rawDisplayName);

  }

  /**
   * Handles initializing the "Element Key" preference to show to the user
   */
  private void initializeElementKey() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_KEY);
    pref.setSummary(this.retrieveColumnDefinition().getElementKey());
  }

  /**
   * Handles initializing the "Column Type" (really data type) preference to show to the user
   */
  private void initializeColumnType() {
    EditTextPreference pref = this.findEditTextPreference(Constants.PreferenceKeys.Column.TYPE);
    pref.setSummary(StringUtils.capitalize(retrieveColumnDefinition().getType().getElementType()));
  }

  /**
   * Handles initializing the "Element Name" preference to show to the user
   */
  private void initializeElementName() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_NAME);
    pref.setSummary(this.retrieveColumnDefinition().getElementName());
  }

  /**
   * Handles initializing the (editable!) "Column Width" preference to show to the user
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeColumnWidth() throws ServicesAvailabilityException {
    TableLevelPreferencesActivity activity = retrieveTableLevelPreferenceActivity();
    final String appName = activity.getAppName();
    final EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.WIDTH);
    int columnWidth = PreferenceUtil.getColumnWidth(getActivity(), getAppName(), getTableId(),
        retrieveColumnDefinition().getElementKey());
    pref.setSummary(Integer.toString(columnWidth));

    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newValueStr = (String) newValue;
        Integer newWidth = Integer.parseInt(newValueStr);
        if (newWidth > LocalKeyValueStoreConstants.Spreadsheet.MAX_COL_WIDTH) {
          WebLogger.getLogger(appName).e(TAG, "column width bigger than allowed, doing nothing");
          return false;
        }
        PreferenceUtil.setColumnWidth(getActivity(), getAppName(), getTableId(),
            retrieveColumnDefinition().getElementKey(), newWidth);
        pref.setSummary(Integer.toString(newWidth));
        return true;
      }
    });

  }

  /**
   * Initializes the color rule menu, which takes you to a different preferences fragment
   */
  private void initializeColorRule() {
    Preference pref = this.findPreference(Constants.PreferenceKeys.Column.COLOR_RULES);
    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        TableLevelPreferencesActivity activity = (TableLevelPreferencesActivity) getActivity();
        activity.showColorRuleListFragment(retrieveColumnDefinition().getElementKey(),
            ColorRuleGroup.Type.COLUMN);
        return true;
      }

    });
  }

}
