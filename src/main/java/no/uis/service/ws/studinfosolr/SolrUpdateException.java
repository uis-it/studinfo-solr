package no.uis.service.ws.studinfosolr;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder =
  { "shortMessage", "messageDetail" })
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
