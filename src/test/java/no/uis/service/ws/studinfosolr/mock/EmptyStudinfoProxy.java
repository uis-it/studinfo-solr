/*
 Copyright 2013 University of Stavanger, Norway

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

package no.uis.service.ws.studinfosolr.mock;

import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import no.usit.fsws.schemas.studinfo.Emne;
import no.usit.fsws.schemas.studinfo.Kurs;
import no.usit.fsws.schemas.studinfo.Sprakkode;
import no.usit.fsws.schemas.studinfo.Studieprogram;
import no.usit.fsws.schemas.studinfo.StudinfoProxy;
import no.usit.fsws.schemas.studinfo.Terminkode;

public class EmptyStudinfoProxy implements StudinfoProxy {

  @Override
  public List<Studieprogram> getStudieprogram(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak,
      boolean medUPinfo, String studieprogramkode)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Kurs> getKurs(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak, int institusjonsnr,
      Integer fakultetsnr, Integer instituttnr, Integer gruppenr)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Studieprogram> getStudieprogrammerForOrgenhet(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak,
      int institusjonsnr, Integer fakultetsnr, Integer instituttnr, Integer gruppenr, boolean medUPinfo)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Emne> getEmne(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak, int institusjonsnr,
      String emnekode, String versjonskode)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Emne> getEmnerForOrgenhet(XMLGregorianCalendar arstall, Terminkode terminkode, Sprakkode sprak, int institusjonsnr,
      Integer fakultetsnr, Integer instituttnr, Integer gruppenr)
  {
    // TODO Auto-generated method stub
    return null;
  }

}
