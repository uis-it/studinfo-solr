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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import no.uis.fsws.studinfo.data.Emnekombinasjon;
import no.uis.fsws.studinfo.data.FsYearSemester;
import no.uis.fsws.studinfo.data.KravSammensetting;
import no.uis.fsws.studinfo.data.Studieprogram;
import no.uis.fsws.studinfo.data.Utdanningsplan;
import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;
import no.uis.studinfo.commons.StudinfoContext;
import no.uis.studinfo.commons.Studinfos;

/**
 * Remove course combinations that don't have any courses (emner).
 */
public class CleanUtdanningsplanListener implements SolrUpdateListener<Studieprogram> {

  @Override
  public void fireBeforeSolrUpdate(ThreadLocal<StudinfoContext> context, SolrType solrType, Studieprogram studinfoElement,
      Map<String, Object> beanmap)
  {
  }

  @Override
  public void fireBeforePushElements(ThreadLocal<StudinfoContext> context, SolrType solrType, List<Studieprogram> elements) {

    final FsYearSemester currentSemester = context.get().getStartYearSemester();
    for (Studieprogram prog : elements) {
      if (prog.isSetUtdanningsplan()) {
        
        final Utdanningsplan uplan = prog.getUtdanningsplan();
        Studinfos.cleanUtdanningsplan(uplan, prog.getStudieprogramkode(), currentSemester,
          Studinfos.numberOfSemesters(prog));

        if (uplan.isSetKravSammensettingListe()) {
          cleanKravList(uplan.getKravSammensettingListe());
        }
      }
    }
  }

  public void cleanKravList(final List<KravSammensetting> kravList) {
    Iterator<KravSammensetting> kravIter = kravList.iterator();
    while (kravIter.hasNext()) {
      KravSammensetting krav = kravIter.next();
      final Emnekombinasjon ek = krav.getEmnekombinasjon();
      if (ek.isSetEmneListe()) {
        continue;
      }
      boolean isEmpty = true;
      if (ek.isSetEmnekombinasjonListe()) {
        isEmpty = cleanEmnekombinajson(ek.getEmnekombinasjonListe());
      }
      if (isEmpty) {
        kravIter.remove();
      }
    }
  }

  /**
   * @param ekList
   * @return true if the list doesn't contain any courses.
   */
  private boolean cleanEmnekombinajson(List<Emnekombinasjon> ekList) {
    final Iterator<Emnekombinasjon> ekIter = ekList.iterator();
    int nonEmpty = 0;
    while (ekIter.hasNext()) {
      final Emnekombinasjon ek = ekIter.next();
      if (ek.isSetEmneListe()) {
        nonEmpty++;
        continue;
      }
      if (ek.isSetEmnekombinasjonListe()) {
        if (!cleanEmnekombinajson(ek.getEmnekombinasjonListe())) {
          nonEmpty++;
          continue;
        }
      }
      ekIter.remove();
    }
    return nonEmpty == 0;
  }

  @Override
  public void fireAfterPushElements(ThreadLocal<StudinfoContext> context, SolrType solrType) {
  }

  @Override
  public void cleanup() {
  }

}
