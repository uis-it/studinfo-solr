package no.uis.service.ws.studinfosolr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.util.List;

import no.uis.fsws.studinfo.util.PropertyInfo;
import no.uis.fsws.studinfo.util.PropertyInfoUtils;
import no.usit.fsws.schemas.studinfo.Studieprogram;

import org.junit.Test;

public class TestBeanDescriptors {

  @Test
  public void testStudieprogram() throws Exception {
    List<PropertyInfo> infos = PropertyInfoUtils.getPropertyInfos(Studieprogram.class);
    
    assertThat(infos, is(notNullValue()));
    
    PropertyInfo piGrunnstudium = new PropertyInfo("grunnstudium", Studieprogram.class.getMethod("setGrunnstudium", Boolean.class), Studieprogram.class.getMethod("isGrunnstudium"), Studieprogram.class.getMethod("isSetGrunnstudium"));
    assertThat(infos, hasItem(piGrunnstudium));
    
    PropertyInfo piClass = new PropertyInfo("class", Studieprogram.class.getMethod("getClass"), null, null); 
    assertThat(infos, not(hasItem(piClass)));
  }
}
