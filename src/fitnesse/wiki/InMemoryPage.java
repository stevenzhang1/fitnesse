// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.wiki;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import util.Clock;
import fitnesse.wiki.storage.FileSystem;

// Remove this page, create FileSystemPage with right FileSystem: MemoryFileSystem.
@Deprecated
public class InMemoryPage extends BaseWikiPage {
  private static final long serialVersionUID = 1L;

  protected static final String currentVersionName = "current_version";

  protected Map<String, PageData> versions = new ConcurrentHashMap<String, PageData>();
  protected Map<String, WikiPage> children = new ConcurrentHashMap<String, WikiPage>();

    public InMemoryPage(String rootPath, String rootPageName, VersionsController versionsController) {
      this(rootPageName, null);
    }

    public InMemoryPage(String rootPath, String rootPageName, FileSystem fileSystem, VersionsController versionsController) {
      this(rootPageName, null);
    }

  protected InMemoryPage(String name, WikiPage parent) {
    super(name, parent);
    versions.put(currentVersionName, new PageData(this, ""));
  }

  public WikiPage addChildPage(String name) {
    WikiPage page = createChildPage(name);
    children.put(name, page);
    return page;
  }

  public static WikiPage makeRoot(String name) {
    return new InMemoryPage(name, null);
  }

  protected WikiPage createChildPage(String name) {
    BaseWikiPage newPage = new InMemoryPage(name, this);
    children.put(newPage.getName(), newPage);
    return newPage;
  }

  public void removeChildPage(String name) {
    children.remove(name);
  }

  public boolean hasChildPage(String pageName) {
    return children.containsKey(pageName);
  }

  protected VersionInfo makeVersion() {
    PageData current = getDataVersion(currentVersionName);

    String name = String.valueOf(VersionInfo.nextId());
    VersionInfo version = makeVersionInfo(current, name);
    versions.put(version.getName(), current);
    return version;
  }

  protected WikiPage getNormalChildPage(String name) {
    return children.get(name);
  }

  public List<WikiPage> getNormalChildren() {
    return new LinkedList<WikiPage>(children.values());
  }

  public PageData getData() {
    return new PageData(getDataVersion(currentVersionName));
  }

  public ReadOnlyPageData readOnlyData() {
      return getDataVersion(currentVersionName);
  }

  @Override
  public Collection<VersionInfo> getVersions() {
    Set<VersionInfo> set = new HashSet<VersionInfo>();
    for (Map.Entry<String, PageData> entry : versions.entrySet()) {
      if (!currentVersionName.equals(entry.getKey()))
        set.add(makeVersionInfo(entry.getValue(), entry.getKey()));
    }
    return set;
  }

  public VersionInfo commit(PageData newData) {
    VersionInfo previousVersion = makeVersion();
    newData.setWikiPage(this);
    newData.getProperties().setLastModificationTime(Clock.currentDate());
    versions.put(currentVersionName, newData);
    return previousVersion;
  }

  public PageData getDataVersion(String versionName) {
    PageData version = versions.get(versionName);
    if (version == null)
      throw new NoSuchVersionException("There is no version '" + versionName + "'");

    Set<String> names = new HashSet<String>(versions.keySet());
    names.remove(currentVersionName);
    List<VersionInfo> pageVersions = new LinkedList<VersionInfo>();
    for (String name : names) {
      PageData data = versions.get(name);
      pageVersions.add(makeVersionInfo(data, name));
    }
    return new PageData(version);
  }

  protected VersionInfo makeVersionInfo(PageData current, String name) {
    String author = current.getAttribute(PageData.LAST_MODIFYING_USER);
    if (author == null)
      author = "";
    Date date = current.getProperties().getLastModificationTime();
    return new VersionInfo(name, author, date);
  }
}
