package no.uis.service.ws.studinfosolr.json;

import java.io.IOException;

import no.uis.fsws.studinfo.data.FsEksamensdato;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class FsEksamensdatoSerializer extends TypeAdapter<FsEksamensdato>{

  @Override
  public void write(JsonWriter out, FsEksamensdato value) throws IOException {
    if (value != null) {
      out.value(value.toString());
    } else {
      out.nullValue();
    }
  }

  @Override
  public FsEksamensdato read(JsonReader in) throws IOException {
    throw new UnsupportedOperationException();
  }
}
