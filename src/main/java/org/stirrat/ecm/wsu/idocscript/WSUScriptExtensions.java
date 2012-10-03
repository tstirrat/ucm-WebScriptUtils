package org.stirrat.ecm.wsu.idocscript;

import intradoc.common.ExecutionContext;
import intradoc.common.LocaleUtils;
import intradoc.common.ServiceException;
import intradoc.common.SystemUtils;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.DataResultSet;
import intradoc.data.Workspace;
import intradoc.provider.Provider;
import intradoc.provider.Providers;
import intradoc.server.Service;
import intradoc.server.ServiceData;
import intradoc.server.ServiceManager;
import intradoc.server.UserStorage;
import intradoc.shared.SharedObjects;
import intradoc.shared.UserData;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.stirrat.ecm.wsu.service.WSUServiceHandler;
import org.ucmtwine.annotation.IdocFunction;

/**
 * Idoc Script Extensions
 * 
 * @author Tim Stirrat <tim.stirrat@gmail.com>
 */
public class WSUScriptExtensions {

  // result set field indices
  private static final int SCRIPT_ROW_PATH = 1;

  private static final int SCRIPT_ROW_GROUP = 2;

  private static final int SCRIPT_ROW_TYPE = 0;

  /**
   * Environment variable that contains the boolean to instruct whether to
   * combine scripts.
   */
  private static final String ENV_COMBINE_SCRIPTS = "WSUCombineScripts";

  /**
   * Environment variable that contains the boolean to instruct whether to
   * compress scripts.
   */
  private static final String ENV_COMPRESS_SCRIPTS = "WSUCompressScripts";

  private static final String TAG_TEMPLATE_JS = "<script type=\"text/javascript\" src=\"%s\"%s></script>";
  private static final String TAG_TEMPLATE_CSS = "<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\"%s />";

  private static final String BINDER_RESULT_SET = "xWSUScripts";

  @IdocFunction
  public void addJs(String item, String group, Long priority, DataBinder binder) {
    addScript(binder, WSUServiceHandler.TYPE_JS, item, group, priority);
  }

  @IdocFunction
  public String renderJs(String group, DataBinder binder) throws IllegalArgumentException, DataException,
      ServiceException {
    return renderScripts(binder, WSUServiceHandler.TYPE_JS, group);
  }

  @IdocFunction
  public void addCss(String item, String group, Long priority, DataBinder binder) {
    addScript(binder, WSUServiceHandler.TYPE_CSS, item, group, priority);
  }

  @IdocFunction
  public String renderCss(String group, DataBinder binder) throws IllegalArgumentException, DataException,
      ServiceException {
    return renderScripts(binder, WSUServiceHandler.TYPE_CSS, group);
  }

  @IdocFunction
  public String combineScripts(String typeString, String itemList, boolean compress, DataBinder binder)
      throws IllegalArgumentException, DataException, ServiceException {

    int type = WSUServiceHandler.TYPE_JS;

    if (typeString.equalsIgnoreCase("text/css") || typeString.equalsIgnoreCase("css")) {
      type = WSUServiceHandler.TYPE_CSS;
    }

    List<String> items = WSUServiceHandler.getCleanItemList(itemList);

    return combineScripts(binder, type, items, "", compress);
  }

  /**
   * Creates the resultset which stores the script information.
   * 
   * @param type
   * @param contentId
   * @param group
   * @param order
   */
  private DataResultSet createScriptsResultSet(DataBinder binder) {
    String[] fields = new String[] { "type", "script", "group", "sort" };

    DataResultSet rsItems = new DataResultSet(fields);

    binder.addResultSet(BINDER_RESULT_SET, rsItems);

    return rsItems;
  }

  /**
   * Wrapper for addJavascript/Stylesheet
   * 
   * @param type
   * @param contentId
   * @param group
   * @param order
   * @throws DataException
   */
  private void addScript(DataBinder binder, int type, String contentId, String group, long order) {
    DataResultSet rsItems = (DataResultSet) binder.getResultSet(BINDER_RESULT_SET);

    if (rsItems == null) {
      rsItems = createScriptsResultSet(binder);
    }

    Vector<String> values = new Vector<String>();

    values.add(Integer.toString(type));
    values.add(contentId);
    values.add(group);
    values.add(Long.toString(order));

    rsItems.addRow(values);

    binder.addResultSet(BINDER_RESULT_SET, rsItems);
  }

  /**
   * Wrapper for renderJavascripts/stylesheets
   * 
   * @param type
   * @param group
   * @return
   * @throws ServiceException
   * @throws DataException
   * @throws IllegalArgumentException
   */
  private static String renderScripts(DataBinder binder, int type, String group) throws IllegalArgumentException,
      DataException, ServiceException {
    boolean compress = SharedObjects.getEnvValueAsBoolean(ENV_COMPRESS_SCRIPTS, false);

    DataResultSet rsItems = (DataResultSet) binder.getResultSet(BINDER_RESULT_SET);

    if (rsItems == null || rsItems.isEmpty()) {
      SystemUtils.trace("system", "Resultset was empty.");
      return "";
    }

    List<String> scripts = new ArrayList<String>();

    rsItems.first();

    do {
      @SuppressWarnings("unchecked")
      Vector<String> scriptRow = rsItems.getCurrentRowValues();

      if (scriptRow.get(SCRIPT_ROW_TYPE).equals(Integer.toString(type))
          && scriptRow.get(SCRIPT_ROW_GROUP).equalsIgnoreCase(group)) {
        scripts.add(scriptRow.get(SCRIPT_ROW_PATH));
      }
    } while (rsItems.next());

    if (scripts.size() > 0) {
      // return single script with combined content
      if (SharedObjects.getEnvValueAsBoolean(ENV_COMBINE_SCRIPTS, false)) {
        return combineScripts(binder, type, scripts, group, compress);
      }

      // return individual script tags
      return writeScriptTags(type, scripts);
    }

    return "";
  }

  /**
   * Simple wrapper to the WSU_COMBINE_SCRIPTS service
   * 
   * @param type
   *          Content type: text/css or text/javascript
   * @param items
   *          Comma separated list of content items
   * @param compress
   *          Whether to compress the combined result
   * @return Html script tag
   * @throws ServiceException
   * @throws DataException
   */
  private static String combineScripts(DataBinder binder, int type, List<String> items, String group, boolean compress)
      throws IllegalArgumentException, DataException, ServiceException {
    DataBinder serviceBinder = new DataBinder();
    serviceBinder.putLocal("IdcService", "WSU_COMBINE_SCRIPTS");

    String dUser = binder.getLocal("dUser");

    if (dUser == null || dUser.equals("")) {
      dUser = "sysadmin";
    }

    if (compress) {
      serviceBinder.putLocal("compress", "true");
    } else {
      serviceBinder.putLocal("compress", "false");
    }

    serviceBinder.putLocal("items", StringUtils.join(items, ","));

    if (type == WSUServiceHandler.TYPE_CSS) {
      serviceBinder.putLocal("type", "css");
    } else {
      serviceBinder.putLocal("type", "js");
    }

    serviceBinder.putLocal("group", group);

    executeService(serviceBinder, dUser, false);

    String filePath = serviceBinder.getLocal("filePath");

    String groupClause = String.format(" id=\"wsu-combined-%d-group-%s\"", type, group);

    if (type == WSUServiceHandler.TYPE_CSS) {
      return String.format(TAG_TEMPLATE_CSS, filePath, groupClause);
    }

    return String.format(TAG_TEMPLATE_JS, filePath, groupClause);
  }

  /**
   * Writes separated script tags for each script in [items]
   * 
   * @param type
   *          Content type: text/css or text/javascript
   * @param items
   *          Comma separated list of content items
   * @param compress
   *          Whether to compress the combined result
   * @return Html script tags
   */
  private static String writeScriptTags(int type, List<String> items) {
    String output = "";

    for (String item : items) {

      if (type == WSUServiceHandler.TYPE_CSS) {
        output += String.format(TAG_TEMPLATE_CSS, item, "") + "\n\t";
      } else {
        output += String.format(TAG_TEMPLATE_JS, item, "") + "\n\t";
      }
    }

    return output;
  }

  /**
   * Execute a service call based on the data in the binder using the
   * credentials of the supplied user
   */
  public static void executeService(DataBinder binder, String userName, boolean suppressServiceError)
      throws DataException, ServiceException {

    // obtain a connection to the database
    Workspace workspace = getSystemWorkspace();
    // check for an IdcService value
    String cmd = binder.getLocal("IdcService");
    if (cmd == null)
      throw new DataException("!csIdcServiceMissing");
    // obtain the service definition
    ServiceData serviceData = ServiceManager.getFullService(cmd);
    if (serviceData == null)
      throw new DataException(LocaleUtils.encodeMessage("!csNoServiceDefined", null, cmd));
    // create the service object for this service
    Service service = ServiceManager.createService(serviceData.m_classID, workspace, null, binder, serviceData);
    // obtain the full user data for this user
    UserData fullUserData = getFullUserData(userName, service, workspace);
    service.setUserData(fullUserData);
    binder.m_environment.put("REMOTE_USER", userName);
    ServiceException error = null;
    try {
      // init the service to not send HTML back
      service.setSendFlags(true, true);
      // create all the ServiceHandlers and implementors
      service.initDelegatedObjects();
      // do a security check
      service.globalSecurityCheck();
      // prepare for the service
      service.preActions();
      // execute the service
      service.doActions();
      // do any cleanup
      service.postActions();
      // store any new personalization data

      service.updateSubjectInformation(true);
      service.updateTopicInformation(binder);
    } catch (ServiceException e) {
      error = e;
    } finally {
      // Remove all the temp files.
      service.cleanUp(true);
      workspace.releaseConnection();
    }
    // handle any error
    if (error != null) {
      if (suppressServiceError) {
        error.printStackTrace();
        if (binder.getLocal("StatusCode") == null) {
          binder.putLocal("StatusCode", String.valueOf(error.m_errorCode));
          binder.putLocal("StatusMessage", error.getMessage());
        }
      } else {
        throw new ServiceException(error.m_errorCode, error.getMessage());
      }
    }
  }

  /**
   * Obtain information about a user. Only the 'userName' parameter must be
   * non-null.
   */
  public static UserData getFullUserData(String userName, ExecutionContext cxt, Workspace ws) throws DataException,
      ServiceException {
    if (ws == null)
      ws = getSystemWorkspace();
    UserData userData = UserStorage.retrieveUserDatabaseProfileDataFull(userName, ws, null, cxt, true, true);
    ws.releaseConnection();
    return userData;
  }

  /**
   * Get the current workspace
   * 
   * @return
   */
  public static Workspace getSystemWorkspace() {
    Workspace workspace = null;
    Provider wsProvider = Providers.getProvider("SystemDatabase");
    if (wsProvider != null)
      workspace = (Workspace) wsProvider.getProvider();
    return workspace;
  }
}