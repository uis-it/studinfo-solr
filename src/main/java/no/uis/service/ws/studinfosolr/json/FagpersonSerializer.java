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

package no.uis.service.ws.studinfosolr.json;

import java.io.IOException;

import no.usit.fsws.schemas.studinfo.Fagperson;
import no.usit.fsws.schemas.studinfo.Personnavn;

import org.apache.commons.lang.NotImplementedException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * A Json type adapter for {@link Fagperson}.
 */
public class FagpersonSerializer extends TypeAdapter<Fagperson> {

  @Override
  public void write(JsonWriter out, Fagperson fp) throws IOException {
    out.beginObject();
    if(fp.isSetPersonid()) {
      out.name("personid").value(fp.getPersonid().intValue());
    }
    if (fp.isSetPersonrolle()) {
      out.name("personrolle").value(fp.getPersonrolle());
    }
    if (fp.isSetDatoFra()) {
      out.name("datofra").value(fp.getDatoFra().toXMLFormat());
    }
    if (fp.isSetDatoTil()) {
      out.name("datotil").value(fp.getDatoTil().toXMLFormat());
    }
    if (fp.isSetPersonnavn()) {
      StringBuilder sbNavn = new StringBuilder();
      Personnavn personnavn = fp.getPersonnavn();
      if (personnavn.isSetFornavn()) {
        sbNavn.append(personnavn.getFornavn());
      }
      if (personnavn.isSetEtternavn()) {
        if (sbNavn.length() > 0) {
          sbNavn.append(' ');
        }
        sbNavn.append(personnavn.getEtternavn());
      }
      if (sbNavn.length() > 0) {
        out.name("navn").value(sbNavn.toString());
      }
    }
    out.endObject();
  }

  @Override
  public Fagperson read(JsonReader in) throws IOException {
    throw new NotImplementedException();
  }
}
