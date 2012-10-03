package org.stirrat.ecm.wsu.filter;

import intradoc.common.ExecutionContext;
import intradoc.common.SystemUtils;
import intradoc.shared.FilterImplementor;

import org.ucmtwine.annotation.Filter;

public class WSUFilters {

  /**
   * Filter to clean out the WSU cache folder on startup.
   */
  @Filter(event = "extraAfterProvidersStartedInit")
  public int cleanCachedScripts(ExecutionContext ctx) {
    SystemUtils.trace("wsu", "Cleaning WSU cached scripts");

    return FilterImplementor.CONTINUE;
  }
}
