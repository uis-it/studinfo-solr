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

package no.uis.service.ws.studinfosolr.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import lombok.SneakyThrows;

import no.uis.fsws.proxy.StudInfoImport;
import no.uis.fsws.studinfo.data.FsSemester;
import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateException;
import no.uis.service.ws.studinfosolr.SolrUpdater;
import no.uis.service.ws.studinfosolr.StudinfoSolrService;
import no.usit.fsws.schemas.studinfo.Emne;
import no.usit.fsws.schemas.studinfo.FsStudieinfo;
import no.usit.fsws.schemas.studinfo.Kurs;
import no.usit.fsws.schemas.studinfo.Sprakkode;
import no.usit.fsws.schemas.studinfo.Studieprogram;
import no.usit.fsws.schemas.studinfo.StudinfoProxy;
import no.usit.fsws.schemas.studinfo.Terminkode;

import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;

/**
 * Implementation of {@link StudinfoSolrService}. 
 */
@ManagedResource(
  objectName = "uis:service=ws-studinfo-solr,component=webservice",
  description = "StudinfoSolrService",
  log = false
)
public class StudinfoSolrServiceImpl implements StudinfoSolrService, NotificationPublisherAware {

  private static final Logger LOG = Logger.getLogger(StudinfoSolrServiceImpl.class);
  private static final AtomicLong SEQUENCE = new AtomicLong();
  
  private StudinfoProxy studinfoImport;
  private SolrUpdater solrUpdater;
  private NotificationPublisher jmxPublisher;
  private int[] defaultFaculties = {-1};
  private int institution = 217;
  private DatatypeFactory dtFactory;

  public void setStudinfoImport(StudinfoProxy studinfoImport) {
    this.studinfoImport = studinfoImport;
  }

  public void setSolrUpdater(SolrUpdater solrUpdater) {
    this.solrUpdater = solrUpdater;
  }

  public void setInstitution(int inst) {
    this.institution = inst;
  }
  
  public void setDefaultFaculties(String facultiesString) {
    int[] newFaculties = null;
    if (defaultFaculties != null && !facultiesString.trim().isEmpty()) {
      String[] tokens = facultiesString.split("\\s*,\\s*");
      newFaculties = new int[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        newFaculties[i] = Integer.parseInt(tokens[i]);
      }
    }
    if (newFaculties == null) {
      defaultFaculties = new int[] {-1};
    } else {
      defaultFaculties = newFaculties;
    }
  }
  
  @Override
  public void updateSolrKurs(int year, String semester, String language, SolrType solrType) throws SolrUpdateException {
    long seq = SEQUENCE.incrementAndGet();
    sendNotification(seq, null, solrType, year, semester, language, "KURS-start");
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);
      
      XMLGregorianCalendar arstall = createXmlCalendar(year, null, null);
      
      Terminkode terminkode = Terminkode.valueOf(semester);
      Sprakkode sprak = Sprakkode.valueOf(language);
      int institusjonsnr = 217;
      Integer fakultetsnr = null;
      Integer instituttnr = null;
      Integer gruppenr = null;
      List<Kurs> kursList = studinfoImport.getKurs(arstall, terminkode, sprak, institusjonsnr, fakultetsnr, instituttnr, gruppenr);
      if (!kursList.isEmpty()) {
        solrUpdater.pushCourses(kursList, year, fsSemester, language, solrType == null ? SolrType.WWW : solrType);
      }
      sendNotification(seq, null, solrType, year, semester, language, "KURS-end");
    } catch(Exception e) {
      sendNotification(seq, e, solrType, year, semester, language, "KURS-error");
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrEmne(int[] faculties, int year, String semester, String language, SolrType solrType) throws SolrUpdateException {
    final SolrType mySolrType = solrType == null ? SolrType.WWW : solrType;

    int[] fa = (faculties == null || faculties.length == 0) ? defaultFaculties : faculties;  
    for (int faculty : fa) {
      updateSolrEmne(faculty, year, semester, language, mySolrType);
    }
  }
  
  private void updateSolrEmne(int faculty, int year, String semester, String language, SolrType solrType) throws SolrUpdateException {

    long seq = SEQUENCE.incrementAndGet();
    sendNotification(seq, null, solrType, faculty, year, semester, language, "EMNE-start");
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);
      XMLGregorianCalendar arstall = createXmlCalendar(year, null, null);
      Terminkode terminkode = Terminkode.valueOf(semester);
      Sprakkode sprak = Sprakkode.valueOf(language);
      int institusjonsnr = 217;
      List<Emne> emneList = studinfoImport.getEmnerForOrgenhet(arstall, terminkode, sprak, institusjonsnr, faculty, null, null);
      solrUpdater.pushSubjects(emneList, year, fsSemester, language, solrType);
      sendNotification(seq, null, solrType, faculty, year, semester, language, "EMNE-end");
    } catch(Exception e) {
      sendNotification(seq, e, solrType, faculty, year, semester, language, "EMNE-error");
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void updateSolrStudieprogram(int[] faculties, int year, String semester, String language, SolrType solrType) throws SolrUpdateException {
    final SolrType mySolrType = solrType == null ? SolrType.WWW : solrType;

    int[] fa = (faculties == null || faculties.length == 0) ? defaultFaculties : faculties;  
    for (int faculty : fa) {
      updateSolrStudieprogram(faculty, year, semester, language, mySolrType);
    }
  }
  
  private void updateSolrStudieprogram(int faculty, int year, String semester, String language, SolrType solrType) throws SolrUpdateException {

    long seq = SEQUENCE.incrementAndGet();
    sendNotification(seq, null, solrType, faculty, year, semester, language, "PROGRAM-start");
    try {
      FsSemester fsSemester = FsSemester.stringToUisSemester(semester);

      XMLGregorianCalendar arstall = createXmlCalendar(year, null, null);
      Terminkode terminkode = Terminkode.valueOf(semester);
      Sprakkode sprak = Sprakkode.valueOf(language);
      int institusjonsnr = 217;
      List<Studieprogram> progList = studinfoImport.getStudieprogrammerForOrgenhet(arstall, terminkode, sprak, institusjonsnr, faculty, null, null, true);
      solrUpdater.pushPrograms(progList, year, fsSemester, language, solrType);
      sendNotification(seq, null, solrType, faculty, year, semester, language, "PROGRAM-end");
    } catch(Exception e) {
      sendNotification(seq, e, solrType, faculty, year, semester, language, "PROGRAM-error");
      throw new SolrUpdateException(e);
    }
  }

  @Override
  public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
    this.jmxPublisher = notificationPublisher;
  }
  
  private void sendNotification(long seqNo, Throwable error, Object... args) {
    if (this.jmxPublisher == null) {
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (Object arg : args) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(arg);
    }
    final String msg = sb.toString();
    Notification notification = new Notification("SolrUpdate", this, seqNo, msg);
    if (error != null) {
      notification.setUserData(errorToString(error));
      LOG.error(msg, error);
    } else {
      LOG.info(msg);
    }
    this.jmxPublisher.sendNotification(notification);
  }

  private String errorToString(Throwable tr) {
    StringWriter sw = new StringWriter();
    tr.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }
  
  @SneakyThrows
  private synchronized DatatypeFactory getDatatypeFactory() {
    if (dtFactory == null) {
      dtFactory = DatatypeFactory.newInstance();
    }
    return dtFactory;
  }
  
  private XMLGregorianCalendar createXmlCalendar(Integer year, Integer month, Integer day) {
    
    int y = year != null ? year.intValue() : DatatypeConstants.FIELD_UNDEFINED;
    int m = month != null ? month.intValue() : DatatypeConstants.FIELD_UNDEFINED;
    int d = day != null ? day.intValue() : DatatypeConstants.FIELD_UNDEFINED;

    return getDatatypeFactory().newXMLGregorianCalendarDate(y, m, d, DatatypeConstants.FIELD_UNDEFINED);
  }
}
