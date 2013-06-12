/*
 Copyright 2012-2013 University of Stavanger, Norway

 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
    super.destroy();
    MultiThreadedHttpConnectionManager.shutdownAll();
  }
}
