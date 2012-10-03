package org.stirrat.ecm.wsu.service;

import intradoc.common.ExecutionContext;
import intradoc.common.LocaleUtils;
import intradoc.common.ServiceException;
import intradoc.common.SystemUtils;
import intradoc.data.DataBinder;
import intradoc.data.DataException;
import intradoc.data.Workspace;
import intradoc.provider.Provider;
import intradoc.provider.Providers;
import intradoc.server.Service;
import intradoc.server.ServiceData;
import intradoc.server.ServiceManager;
import intradoc.server.UserStorage;
import intradoc.shared.SharedObjects;
import intradoc.shared.UserData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import org.ucmtwine.annotation.Binder;
import org.ucmtwine.annotation.ServiceMethod;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

public class WSUServiceHandler {

  public static final String COMBINE_SERVICE_NAME = "WSU_COMBINE_SCRIPTS";
  public static final int TYPE_JS = 0;
  public static final int TYPE_CSS = 1;
  public static final String[] contentType = { "text/javascript", "text/css" };
  public static final String cacheDir = "resources/wsu/";

  /**
   * WSU_COMBINE_SCRIPTS
   * 
   * @param typeString
   *          Content type: "text/javascript" or "text/css"
   * @param items
   *          The comma separated list of paths relative to the weblayout root
   * @param compress
   *          (optional) overrides environment variable
   * @param group
   *          The compression group
   * @throws ServiceException
   */
  @ServiceMethod(name = "WSU_COMBINE_SCRIPTS", accessLevel = 49, errorMessage = "WSU Service Error", template = "BLANK")
  public void WSUCombineScripts(@Binder(name = "type") String typeString, @Binder(name = "items") String items,
      @Binder(name = "compress", required = false) Boolean compress,
      @Binder(name = "group", required = false) String group, DataBinder binder) throws ServiceException, DataException {

    // if not supplied, default to environment specified compress directive
    if (compress == null) {
      compress = SharedObjects.getEnvValueAsBoolean("WSUCompressScripts", false);
    }

    String httpRelativeWebRoot = SharedObjects.getEnvironmentValue("HttpRelativeWebRoot");

    int type = parseType(typeString);

    List<String> itemList = getCleanItemList(items);

    if (group == null || "" == group) {
      group = "scripts";
    }

    // check for cached copy
    String combinedScriptsPath = getCachedScripts(itemList, group, type, compress, binder);

    binder.putLocal("filePath", httpRelativeWebRoot + combinedScriptsPath);
  }

  /**
   * Returns the filename of the cached scripts. If the cache is invalid, it
   * recreates it.
   * 
   * @param itemList
   * @param type
   * @param compress
   * @param binder
   * @return
   * @throws ServiceException
   */
  public static String getCachedScripts(List<String> itemList, String group, int type, boolean compress,
      DataBinder binder) throws ServiceException {
    String filename = cacheDir + group.toLowerCase().trim() + "_" + getHash(itemList);

    if (compress) {
      filename += "_c";
    }

    if (type == TYPE_CSS) {
      filename += ".css";
    } else {
      filename += ".js";
    }

    SystemUtils.trace("wsu", "getCachedScripts: read from: " + filename);

    if (cacheFileIsValid(itemList, filename)) {
      return filename;
    }

    return createCache(itemList, type, filename, compress);
  }

  /**
   * Determine if the cache file is still valid by comparing it to each item's
   * last modified date.
   * 
   * @param itemList
   * @param filename
   * @return
   */
  public static boolean cacheFileIsValid(List<String> itemList, String filename) {
    String ucmDataRoot = SharedObjects.getEnvironmentValue("WeblayoutDir");
    String HttpRelativeWebRoot = SharedObjects.getEnvironmentValue("HttpRelativeWebRoot");

    String cachePath = ucmDataRoot + filename;

    File file = new File(cachePath);

    if (!file.exists()) {
      SystemUtils.trace("wsu", "cacheFileIsValid: MISS: cache doesn't exist: " + cachePath);
      return false;
    }

    Long cacheLastModified = file.lastModified();

    for (String s : itemList) {
      String fullPath = ucmDataRoot + s.substring(HttpRelativeWebRoot.length());

      File item = new File(fullPath);
      if (item.exists()) {
        if (item.lastModified() > cacheLastModified) {
          SystemUtils.trace("wsu", "cacheFileIsValid: MISS: " + fullPath + " modified after: " + cacheLastModified);
          return false;
        }
      } else {
        SystemUtils.trace("wsu", "cacheFileIsValid: MISS: cant find file: " + fullPath);
      }
    }
    return true;
  }

  /**
   * Create the output for caching from each item in the list
   * 
   * @param itemList
   * @param type
   * @param filename
   * @param compress
   * @return
   * @throws ServiceException
   */
  public static String createCache(List<String> itemList, int type, String filename, boolean compress)
      throws ServiceException {

    String ucmDataRoot = SharedObjects.getEnvironmentValue("WeblayoutDir");

    String HttpRelativeWebRoot = SharedObjects.getEnvironmentValue("HttpRelativeWebRoot");

    String content = "";

    for (String filePath : itemList) {
      // skip url items for now
      if (filePath.startsWith(HttpRelativeWebRoot)) {
        String fullPath = ucmDataRoot + filePath.substring(HttpRelativeWebRoot.length());

        try {
          content += getFileContents(fullPath);
        } catch (IOException ioe) {
          SystemUtils.trace("wsu", "IO error: " + fullPath + ": " + ioe.getMessage());
          content += String.format("/* IO error: %s */\n", fullPath);
          ioe.printStackTrace();
        }
      }

      if (compress) {
        content = compressContent(content, type);
      }

    }

    storeCache(content, filename);

    return filename;
  }

  /**
   * Store the output into the cache file
   * 
   * @param content
   * @param filename
   * @return
   * @throws ServiceException
   */
  public static boolean storeCache(String content, String filename) throws ServiceException {
    String ucmDataRoot = SharedObjects.getEnvironmentValue("WeblayoutDir");

    File baseDir = new File(ucmDataRoot + cacheDir);

    String fullPath = ucmDataRoot + filename;

    if (!baseDir.exists()) {
      if (baseDir.mkdir()) {
        SystemUtils.trace("wsu", "storeCache: created: " + baseDir);
      } else {
        throw new ServiceException("Cannot create required directory: " + baseDir);
      }
    }

    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(fullPath));
      out.write(content);
      out.close();
    } catch (IOException e) {
      SystemUtils.trace("wsu", "Failed to write file: " + fullPath);
      return false;
    }

    SystemUtils.trace("wsu", "storeCache: " + fullPath);

    return true;
  }

  /**
   * Get items from the supplied string, no duplicates
   * 
   * @param items
   * @return
   */
  public static List<String> getCleanItemList(String items) {
    List<String> itemList = new ArrayList<String>();

    // no cache, create from scratch
    StringTokenizer st = new StringTokenizer(items, ",");

    while (st.hasMoreTokens()) {
      String contentID = st.nextToken().trim();

      // add without duplicates
      if (itemList.indexOf(contentID) == -1)
        itemList.add(contentID);
    }

    return itemList;
  }

  /**
   * Get file contents
   * 
   * @param filepath
   * @return
   * @throws IOException
   */
  public static String getFileContents(String filepath) throws IOException {
    SystemUtils.trace("wsu", "getFileContents: READ: " + filepath);

    FileInputStream stream = new FileInputStream(new File(filepath));
    try {
      FileChannel fc = stream.getChannel();
      MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
      /* Instead of using default, pass in a decoder. */
      return Charset.defaultCharset().decode(bb).toString();
    } finally {
      stream.close();
    }
  }

  /**
   * Gets a unique hash based on a sorted, unique list of items and whether the
   * request was compressed
   * 
   * @param items
   * @return
   * @throws ServiceException
   */
  private static String getHash(List<String> items) throws ServiceException {
    List<String> sortedList = new ArrayList<String>();

    for (String s : items) {
      sortedList.add(s);
    }

    // sort alphabetically
    Collections.sort(sortedList, String.CASE_INSENSITIVE_ORDER);

    String combinedStr = "";

    // create single string
    for (String s : sortedList) {
      combinedStr += s;
    }

    String hash = "";

    try {
      hash = getAdler32(combinedStr);
    } catch (NoSuchAlgorithmException e) {
      throw new ServiceException("Requested algorithm unavailable");
    }

    return hash;
  }

  /**
   * Get MD5 for a String
   * 
   * @param input
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static String getMd5(String input) throws NoSuchAlgorithmException {
    MessageDigest m = MessageDigest.getInstance("MD5");
    byte[] data = input.getBytes();
    m.update(data, 0, data.length);
    BigInteger i = new BigInteger(1, m.digest());
    return String.format("%1$032X", i);
  }

  /**
   * Get CRC32 for a String
   * 
   * @param input
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static String getAdler32(String input) throws NoSuchAlgorithmException {
    Checksum m = new Adler32();
    byte[] data = input.getBytes();
    m.update(data, 0, data.length);
    long checksum = m.getValue();
    return String.format("%1$08X", checksum);
  }

  /**
   * Compress content based on supplied type
   * 
   * @param content
   * @param type
   * @return
   */
  public static String compressContent(String content, int type) {
    StringWriter sw = new StringWriter();

    StringReader in = new StringReader(content);
    int linebreakpos = -1;

    try {

      if (type == TYPE_JS) {
        JavaScriptCompressor compressor = new JavaScriptCompressor(in, null);

        compressor.compress(sw, linebreakpos, false, false, false, false);
      } else if (type == TYPE_CSS) {
        CssCompressor compressor = new CssCompressor(in);
        compressor.compress(sw, linebreakpos);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return sw.toString();
  }

  /**
   * Parse the supplied "type" parameter
   * 
   * @param typeString
   * @return
   * @throws ServiceException
   */
  public static int parseType(String typeString) throws ServiceException {
    if (typeString == null || typeString.equals("")) {
      throw new ServiceException("\"type\" cannot be empty");
    }

    if (typeString.equalsIgnoreCase("css") || typeString.equalsIgnoreCase("text/css")) {
      return TYPE_CSS;
    }

    if (typeString.equalsIgnoreCase("js") || typeString.equalsIgnoreCase("text/javasript")
        || typeString.equalsIgnoreCase("javasript")) {
      return TYPE_JS;
    }

    throw new ServiceException("Invalid \"type\" supplied, use one of  'css' or 'js'");
  }

  /**
   * Execute a service call based on the data in the binder using the
   * credentials of the supplied user
   */
  public void executeService(DataBinder binder, String userName, boolean suppressServiceError) throws DataException,
      ServiceException {

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
        throw new ServiceException(error);
      }
    }
  }

  /**
   * Obtain information about a user. Only the 'userName' parameter must be
   * non-null.
   */
  public UserData getFullUserData(String userName, ExecutionContext cxt, Workspace ws) throws DataException,
      ServiceException {
    if (ws == null)
      ws = getSystemWorkspace();
    UserData userData = UserStorage.retrieveUserDatabaseProfileDataFull(userName, ws, null, cxt, true, true);
    ws.releaseConnection();
    return userData;
  }

  /**
   * Obtain the workspace connector to the database
   */
  public Workspace getSystemWorkspace() {
    Workspace workspace = null;
    Provider wsProvider = Providers.getProvider("SystemDatabase");
    if (wsProvider != null)
      workspace = (Workspace) wsProvider.getProvider();
    return workspace;
  }
}
