package no.uis.service.ws.studinfosolr.json;

import java.io.IOException;

import org.apache.commons.lang.NotImplementedException;

import no.uis.service.studinfo.data.Fagperson;
import no.uis.service.studinfo.data.Personnavn;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

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
