package com.corepublish.impl.xml;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import com.corepublish.util.DomainUrl;

public class XmlAccessorHolder2 extends XmlAccessorHolder {

  @Override
  protected XmlAccessor createAccessor(DomainUrl url) {
    XmlAccessor accessor = new XmlAccessor(url);
    boolean validated = false;
    try {
      setTokenConfiguration(url, accessor);
      accessor.startup();
      validated = true;
    } finally {
      if (!validated) {
        accessor.shutdown();
      }
    }
    return accessor;
  }
  
  @Override
  public void destroy() {
    // TODO Auto-generated method stub
    super.destroy();
    MultiThreadedHttpConnectionManager.shutdownAll();
  }
}
