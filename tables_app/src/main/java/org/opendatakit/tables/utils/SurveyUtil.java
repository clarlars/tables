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
package org.opendatakit.tables.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.data.KeyValueStoreEntry;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.utilities.KeyValueStoreUtils;
import org.opendatakit.dependencies.DependencyChecker;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.provider.FormsProviderUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Various functions and utilities necessary
 * to use Survey to interact with ODKTables.
 *
 * @author sudar.sam@gmail.com
 */
public class SurveyUtil {

  private static final String TAG = SurveyUtil.class.getSimpleName();

  /**
   * This is prepended to each row/instance uuid.
   */
  private static final String INSTANCE_UUID_PREFIX = "uuid:";

  /**
   * Survey's package name as declared in the manifest.
   */
  private static final String SURVEY_PACKAGE_NAME = IntentConsts.Survey.SURVEY_PACKAGE_NAME;
  /**
   * The full path to Survey's main menu activity.
   */
  private static final String SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME = IntentConsts.Survey.SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME;

  private static final String SURVEY_ADDROW_FORM_ID_PREFIX = "_generated_";

  /**
   * Return the formId for the single file that will be written when there is no
   * custom form defined for a table.
   *
   * @param tableId the id of the table we're trying to add data to
   * @return the default autogenerated survey id
   */
  private static String getDefaultAddRowFormId(String tableId) {
    return SURVEY_ADDROW_FORM_ID_PREFIX + tableId;
  }

  /**
   * Acquire an intent that will be set up to add a row using Survey. It
   * should eventually be able to prepopulate the row with the values in
   * elementKeyToValue. However! Much of this is still unimplemented.
   *
   * @param context              unused
   * @param appName              the app name
   * @param tableId              the id of the table we want to add a row to
   * @param surveyFormParameters an object that contains a form id, whether the form was
   *                             automatically generated, and the screen path to the form
   * @param elementKeyToValue    a mapping of elementName to value for the values
   *                             that you wish to prepopulate in the add row.
   * @return an intent to open Survey to a particular form
   */
  public static Intent getIntentForOdkSurveyAddRow(Context context, String appName, String tableId,
      SurveyFormParameters surveyFormParameters, Map<String, Object> elementKeyToValue) {

    // To launch to a specific form we need to construct an Intent meant for
    // the MainMenuActivity. This Intent takes a Uri as its setData element
    // that specifies the form and the instance. For more detail on what this
    // Uri must look like, see getUriForSurveyHelper.
    Intent intent = new Intent();
    intent.setComponent(
        new ComponentName(SURVEY_PACKAGE_NAME, SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME));
    intent.setAction(Intent.ACTION_EDIT);
    Uri addUri = getUriForSurveyAddRow(appName, tableId, surveyFormParameters, elementKeyToValue);
    intent.setData(addUri);
    return intent;
  }

  /**
   * Acquire an Intent that can be launched to edit a given row using Survey.
   * The row that will be edited will be that pointed to by instanceId, which
   * must correspond to the rowId of that row. The form that will be used will
   * be that specified by surveyFormParameters.
   * <p>
   * TODO: does supporting both a form and an instance make sense with Survey?
   * Does the row perhaps presuppose some structure on the form? Should clarify
   * this.
   *
   * @param context              unused
   * @param appName              the app name
   * @param tableId              the table id
   * @param surveyFormParameters an object that contains a form id, whether the form was
   *                             automatically generated, and the screen path to the form
   * @param instanceId           which row to edit
   * @return an intent that can be started to switch to survey
   */
  public static Intent getIntentForOdkSurveyEditRow(Context context, String appName, String tableId,
      SurveyFormParameters surveyFormParameters, String instanceId) {
    // To launch a specific form for a particular row we need to construct up
    // an Intent for Survey's MainMenuActivity. This Intent takes a Uri via its
    // setData function that specifies the form and the instance. See
    // getUriForSurveyHelper for more detail.
    Intent intent = new Intent();
    intent.setComponent(
        new ComponentName(SURVEY_PACKAGE_NAME, SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME));
    intent.setAction(Intent.ACTION_EDIT);
    Uri editUri = getUriForSurveyEditRow(appName, tableId, surveyFormParameters, instanceId);
    intent.setData(editUri);
    return intent;
  }

  /**
   * Get a Uri that can be added to a Survey Intent in order to add a row to
   * the specified table and app using the form pointed to by
   * surveyFormParameters.
   *
   * @param appName              the app name
   * @param tableId              the table id for the table that we want to add a row to
   * @param surveyFormParameters an object that contains a form id, whether the form was
   *                             automatically generated, and the screen path to the form
   * @param elementKeyToValue    a map of prepopulated values to add to the form.
   * @return
   */
  private static Uri getUriForSurveyAddRow(String appName, String tableId,
      SurveyFormParameters surveyFormParameters, Map<String, Object> elementKeyToValue) {
    // We'll create a UUID, as that will tell survey we want a new one.
    String newUuid = INSTANCE_UUID_PREFIX + UUID.randomUUID().toString();
    Uri helpedUri = getUriForSurveyHelper(appName, tableId, surveyFormParameters, newUuid,
        elementKeyToValue);
    return helpedUri;
  }

  /**
   * Get a Uri that can be added to a Survey Intent in order to edit the row
   * specified by instanceId using the form specified by surveyFormParameters.
   *
   * @param appName              the app name for the uri
   * @param tableId              the table id for the uri
   * @param surveyFormParameters an object that contains a form id, whether the form was
   *                             automatically generated, and the screen path to the form
   * @param instanceId           the id of the row we want to edit
   * @return a URI that survey can parse to figure out which row we want to edit and how we want
   * to edit it
   */
  private static Uri getUriForSurveyEditRow(String appName, String tableId,
      SurveyFormParameters surveyFormParameters, String instanceId) {
    // The helper function does most of the heavy lifting here. Unlike the
    // add row call, all we do is hand off the info, as we already have our
    // instanceId, which is pointing to an existing row.
    // Note that while the code path might exist to add key-value pairs to the
    // end of the URI, since we're editing we're not going to allow this for
    // now. It's conceivable, perhaps, that we'll want to allow specification
    // of subforms or something, but for now we're not going to allow it.
    return getUriForSurveyHelper(appName, tableId, surveyFormParameters, instanceId, null);
  }

  /**
   * Helper function for getting an Intent to add or edit data using Survey.
   *
   * @param appName              the app name
   * @param tableId              the table that will be receiving the add Survey
   * @param surveyFormParameters the parameters detailing the form to use.
   * @param instanceId           the instance of the id that will be edited or added. A
   *                             newly-generated id will result in an add row--an existent ID will
   *                             result in an edit row for the specified id.
   * @param elementKeyToValue    optional map of key/value pairs that will be added after the #
   * @return
   */
  public static Uri getUriForSurveyHelper(String appName, String tableId,
      SurveyFormParameters surveyFormParameters, String instanceId,
      Map<String, Object> elementKeyToValue) {
    // We're operating for the moment under the assumption that Survey expects
    // a uri like the following:
    // content://org.opendatakit.survey.provider.FormProvider/appname/formId/#.
    // # is a url frame and it accepts query parameters as in a URL. Pertinent
    // ones include those specified in SurveyFormParameters. Of particular note
    // are the following:
    // instanceId (to edit a particular instance)
    // screenPath (to open to a particular screen--not yet useful, but perhaps later).
    return Uri.parse(FormsProviderUtils
        .constructSurveyUri(appName, tableId, surveyFormParameters.getFormId(), instanceId,
            surveyFormParameters.getScreenPath(), elementKeyToValue));
  }

  /**
   * TODO: Eventually will launch the Intent with the correct return code.
   * For now, however, just starts it directly without waiting for return.
   *
   * @param activityToAwaitReturn the activity that will be notified when the user is done adding
   *                              the row
   * @param tableId               the id of the table to add the row to
   * @param surveyAddIntent       an intent object used by startActivityForResult
   */
  public static void launchSurveyToAddRow(AbsBaseActivity activityToAwaitReturn, String tableId,
      Intent surveyAddIntent) {
    Context ctxt = activityToAwaitReturn.getApplicationContext();
    if (DependencyChecker.isPackageInstalled(ctxt, DependencyChecker.surveyAppPkgName)) {
      activityToAwaitReturn.setActionTableId(tableId);
      activityToAwaitReturn
          .startActivityForResult(surveyAddIntent, Constants.RequestCodes.ADD_ROW_SURVEY);

    } else {
      Toast.makeText(ctxt, ctxt.getString(R.string.survey_not_installed), Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Add a row with Survey. Convenience method for calling
   * {@see #getIntentForOdkSurveyAddRow(String, String, SurveyFormParameters, Map)} followed by
   * {@see #launchSurveyToAddRow(Activity, String, Intent)}.
   *
   * @param activity             activity to await activity return
   * @param appName              the app name
   * @param tableId              the table we want to add a row to
   * @param surveyFormParameters information about the form to use
   * @param prepopulatedValues   values you want to prepopulate the form with.
   *                             Should be element key to value.
   */
  public static void addRowWithSurvey(AbsBaseActivity activity, String appName, String tableId,
      SurveyFormParameters surveyFormParameters, Map<String, Object> prepopulatedValues) {
    Intent addRowIntent = SurveyUtil
        .getIntentForOdkSurveyAddRow(activity, appName, tableId, surveyFormParameters,
            prepopulatedValues);
    SurveyUtil.launchSurveyToAddRow(activity, tableId, addRowIntent);
  }

  /**
   * Launch survey to edit a row. Convenience method for calling
   * {@see #getIntentForOdkSurveyEditRow(Context, String, String, *SurveyFormParameters, String)} followed by
   * {@see #launchSurveyToEditRow(Activity, Intent, String, String)}.
   *
   * @param activity             activity to await the return of the launch
   * @param appName              the app name
   * @param tableId              the table that the row is in
   * @param instanceId           id of the row to edit
   * @param surveyFormParameters information about which form to use to edit the row
   */
  public static void editRowWithSurvey(AbsBaseActivity activity, String appName, String tableId,
      String instanceId, SurveyFormParameters surveyFormParameters) {
    Intent editRowIntent = SurveyUtil
        .getIntentForOdkSurveyEditRow(activity, appName, tableId, surveyFormParameters, instanceId);
    SurveyUtil.launchSurveyToEditRow(activity, tableId, editRowIntent, instanceId);
  }

  /**
   * TODO: eventually launch with the correct return code. For now, just starts
   * the activity without waiting for the return.
   *
   * @param activityToAwaitReturn the activity that will be notified when the user is done editing
   *                              the row
   * @param tableId               the id of the table to add the row to
   * @param surveyEditIntent      an intent object used by startActivityForResult
   * @param rowId                 unused
   */
  public static void launchSurveyToEditRow(AbsBaseActivity activityToAwaitReturn, String tableId,
      Intent surveyEditIntent, String rowId) {
    Context ctxt = activityToAwaitReturn.getApplicationContext();
    if (DependencyChecker.isPackageInstalled(ctxt, DependencyChecker.surveyAppPkgName)) {
      activityToAwaitReturn.setActionTableId(tableId);
      activityToAwaitReturn
          .startActivityForResult(surveyEditIntent, Constants.RequestCodes.EDIT_ROW_SURVEY);
    } else {
      Toast.makeText(ctxt, ctxt.getString(R.string.survey_not_installed), Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Holds parameters for a Survey form.
   *
   * @author sudar.sam@gmail.com
   */
  public static class SurveyFormParameters {

    /**
     * The app-wide unique id for the form. Must obey standard Survey rules for
     * form ids. This includes beginning with a letter and followed by only
     * letters, "_", or 0-9.
     */
    private String mFormId;
    /**
     * The screen path specifying where to start within a form. Can be null,
     * and will imply to start at the beginning of the form.
     */
    private String mScreenPath;
    /**
     * Flag indicating whether this form has been user-defined. False indicates
     * that it is NOT custom, and has been generated by Tables based on the
     * table definition. True indicates that the form IS custom and has been
     * created by a user.
     */
    private boolean mIsUserDefined;

    /**
     * Empty constructor
     */
    @SuppressWarnings("unused")
    private SurveyFormParameters() {
      // including this in case it needs to be serialized to json.
    }

    /**
     * Constructor that stores its three arguments
     *
     * @param isUserDefined whether the form was autogenerated or not
     * @param formId        the id of the form
     * @param screenPath    the screen path of the form
     */
    public SurveyFormParameters(boolean isUserDefined, String formId, String screenPath) {
      this.mIsUserDefined = isUserDefined;
      this.mFormId = formId;
      this.mScreenPath = screenPath;
    }

    /**
     * Construct a SurveyFormParameters object from the given tableId.
     * The object is determined to have custom parameters if a formId can be
     * retrieved from the tableId. Otherwise the default addrow
     * parameters are set.
     * <p>
     * The display name of the row will be the display name of the table.
     *
     * @param context unused
     * @param appName the app name
     * @param tableId the id of the table to add/edit a row in
     * @return a SurveyFormParameters object that contains information about what form survey
     * should open
     * @throws ServicesAvailabilityException if the database is down
     */
    public static SurveyFormParameters constructSurveyFormParameters(Context context,
        String appName, String tableId) throws ServicesAvailabilityException {
      String formId;
      DbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(appName);
        List<KeyValueStoreEntry> kvsList = Tables.getInstance().getDatabase()
            .getTableMetadata(appName, db, tableId,
                LocalKeyValueStoreConstants.DefaultSurveyForm.PARTITION,
                LocalKeyValueStoreConstants.DefaultSurveyForm.ASPECT,
                LocalKeyValueStoreConstants.DefaultSurveyForm.KEY_FORM_ID, null).getEntries();
        if (kvsList.size() != 1) {
          formId = null;
        } else {
          formId = KeyValueStoreUtils.getString(appName, kvsList.get(0));
        }
      } finally {
        if (db != null) {
          Tables.getInstance().getDatabase().closeDatabase(appName, db);
        }
      }
      if (formId == null) {
        return new SurveyFormParameters(false, getDefaultAddRowFormId(tableId), null);
      }
      // Else we know it is custom.
      return new SurveyFormParameters(true, formId, null);
    }

    /**
     * standard getter for the form id
     *
     * @return the form id
     */
    public String getFormId() {
      return this.mFormId;
    }

    /**
     * sets the form id but also sets user defiend to true
     *
     * @param formId the form id to use to add or edit a row in survey
     */
    public void setFormId(String formId) {
      this.mIsUserDefined = true;
      this.mFormId = formId;
    }

    /**
     * standard getter for the screen path
     *
     * @return the screen path
     */
    public String getScreenPath() {
      return this.mScreenPath;
    }

    /**
     * sets the screen path and also sets user defined to true
     *
     * @param screenPath
     */
    public void setScreenPath(String screenPath) {
      this.mIsUserDefined = true;
      this.mScreenPath = screenPath;
    }

    /**
     * standard getter for whether the form is user defined or autogenerated
     *
     * @return false if the form is autogenerated or true otherwise
     */
    public boolean isUserDefined() {
      return this.mIsUserDefined;
    }

    /**
     * standard setter for whether the form is user defined or not
     *
     * @param isUserDefined whether the form is user defined or not
     */
    public void setIsUserDefined(boolean isUserDefined) {
      this.mIsUserDefined = isUserDefined;
    }

    public void persist(String appName, DbHandle db, String tableId)
        throws ServicesAvailabilityException {
      KeyValueStoreEntry entry = KeyValueStoreUtils
          .buildEntry(tableId, LocalKeyValueStoreConstants.DefaultSurveyForm.PARTITION,
              LocalKeyValueStoreConstants.DefaultSurveyForm.ASPECT,
              LocalKeyValueStoreConstants.DefaultSurveyForm.KEY_FORM_ID, ElementDataType.string,
              this.isUserDefined() ? this.mFormId : null);

      Tables.getInstance().getDatabase().replaceTableMetadata(appName, db, entry);
    }
  }

}
