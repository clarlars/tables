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
package org.opendatakit.tables.views.components;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.data.ColorRule;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.provider.DataTableColumns;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class ColorRuleAdapter extends ArrayAdapter<ColorRule> {

  private static final String TAG = ColorRuleAdapter.class.getSimpleName();

  private final TableLevelPreferencesActivity mTableLevelActivity;
  private final String mAppName;
  private final String mTableId;
  private String[] mAdminColumns;
  private Map<String,String> mLocalizedDisplayNames;
  private List<ColorRule> mColorRules;
  private int mResourceId;
  private ColorRuleGroup.Type mType;

  public ColorRuleAdapter(TableLevelPreferencesActivity activity, String appName, String tableId, int resource,
      String[] adminColumns, Map<String,String> localizedDisplayNames, List<ColorRule> colorRules, ColorRuleGroup.Type colorRuleType) {
    super(activity, resource, colorRules);
    this.mTableLevelActivity = activity;
    this.mAppName = appName;
    this.mTableId = tableId;
    this.mResourceId = resource;
    this.mAdminColumns = adminColumns;
    this.mLocalizedDisplayNames = localizedDisplayNames;
    this.mColorRules = colorRules;
    this.mType = colorRuleType;
  }

  private View createView(ViewGroup parent) {
    LayoutInflater layoutInflater = (LayoutInflater) parent.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    return layoutInflater.inflate(this.mResourceId, parent, false);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = convertView;
    if (row == null) {
      row = this.createView(parent);
    }
    final int currentPosition = position;
    // We'll need to display the display name if this is an editable field.
    // (ie if a status column or table rule)
    String description = "";
    boolean isMetadataSyncRule = false;
    if (mType == ColorRuleGroup.Type.STATUS_COLUMN || mType == ColorRuleGroup.Type.TABLE) {
      ColorRule colorRule = mColorRules.get(currentPosition);
      String elementKey = colorRule.getColumnElementKey();
      
      String appName = mTableLevelActivity.getAppName();
        
      if (Arrays.asList(mAdminColumns).contains(elementKey)) {
        if (elementKey.equals(DataTableColumns.SYNC_STATE)) {
          isMetadataSyncRule = true;
          // We know it must be a String rep of an int.
          SyncState targetState = null;
          try {
            targetState = SyncState.valueOf(colorRule.getVal());

            // For now we need to handle the special cases of the sync state.
            if (targetState == SyncState.new_row) {
              description = this.mTableLevelActivity.getString(R.string.sync_state_equals_new_row_message);
            } else if (targetState == SyncState.changed) {
              description = this.mTableLevelActivity.getString(R.string.sync_state_equals_changed_message);
            } else if (targetState == SyncState.synced) {
              description = this.mTableLevelActivity.getString(R.string.sync_state_equals_synced_message);
            } else if (targetState == SyncState.synced_pending_files) {
              description = this.mTableLevelActivity
                      .getString(R.string.sync_state_equals_synced_pending_files_message);
            } else if (targetState == SyncState.deleted) {
              description = this.mTableLevelActivity.getString(R.string.sync_state_equals_deleted_message);
            } else if (targetState == SyncState.in_conflict) {
              description = this.mTableLevelActivity.getString(R.string.sync_state_equals_in_conflict_message);
            } else {
              WebLogger.getLogger(mAppName).e(TAG, "unrecognized sync state: " + targetState);
              description = "unknown";
            }
          } catch (IllegalArgumentException | NullPointerException ex) {
            WebLogger.getLogger(mAppName).e(TAG, "unrecognized sync state: " + targetState);
            description = "unknown";
          }
        } else {
          description = elementKey;
        }
      } else {
        description = mLocalizedDisplayNames.get(elementKey);
      }
    }

    if (!isMetadataSyncRule) {
      description += " " + mColorRules.get(currentPosition).getOperator().getSymbol() + " "
          + mColorRules.get(currentPosition).getVal();
    }
    TextView label = (TextView) row.findViewById(R.id.row_label);
    label.setText(description);
    final int backgroundColor = mColorRules.get(currentPosition).getBackground();
    final int textColor = mColorRules.get(currentPosition).getForeground();
    // Will demo the color rule.
    TextView exampleView = (TextView) row.findViewById(R.id.row_ext);
    exampleView.setText(this.mTableLevelActivity.getString(R.string.status_column));
    exampleView.setTextColor(textColor);
    exampleView.setBackgroundColor(backgroundColor);
    exampleView.setVisibility(View.VISIBLE);
    // The radio button is meaningless here, so get it off the screen.
    final RadioButton radioButton = (RadioButton) row.findViewById(R.id.radio_button);
    radioButton.setVisibility(View.GONE);

    return row;
  }
}
