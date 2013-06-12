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

package no.uis.service.ws.studinfosolr;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(
  name = "SolrUpdateException",
  namespace = "http://studinfosolr.ws.service.uis.no/",
  propOrder = { "shortMessage", "messageDetail" }
)
public class SolrUpdateException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private String shortMessage;
  private String messageDetail;

  public SolrUpdateException() {
  }
  
  public SolrUpdateException(Exception ex) {
    initCause(ex);
    shortMessage = ex.getMessage();
    StringWriter sw = new StringWriter();
    ex.printStackTrace(new PrintWriter(sw));
    messageDetail = sw.toString();
  }

  public String getShortMessage() {
    return this.shortMessage;
  }

  public void setShortMessage(String msg) {
    this.shortMessage = msg;
  }

  public String getMessageDetail() {
    return this.messageDetail;
  }

  public void setMessageDetail(String msgDetail) {
    this.messageDetail = msgDetail;
  }

  @Override
  @XmlTransient
  public StackTraceElement[] getStackTrace() {
    return super.getStackTrace();
  }
}
