package no.uis.service.ws.studinfosolr;

import static org.junit.Assert.*;

import java.util.List;

import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.corepublish.api.Accessor;
import com.corepublish.api.ArticleQuery;
import com.corepublish.api.ArticleQueryResult;
import com.corepublish.impl.defaultt.DefaultArticleQuery;
import com.corepublish.impl.xml.XmlAccessor;
import com.corepublish.impl.xml.XmlAccessorHolder;
import com.corepublish.impl.xml.XmlRequest;
import com.corepublish.util.DomainUrl;

public class TestCplib4j {

  private XmlAccessor cpAccessor;

  @Before
  public void initAccessor() {
    cpAccessor = new XmlAccessor(new DomainUrl("http://coreprod02.uis.no/corepublish", 5));
    cpAccessor.startup();
  }
  
  @After
  public void destroyAccessor() {
    if (cpAccessor != null) {
      cpAccessor.shutdown();
    }
  }
  
  //@Test
  public void testCplib4j() {
    ArticleQuery articleQuery = new DefaultArticleQuery();
    articleQuery.includeCategoryId(5793, true);
    articleQuery.includeTemplateIds(10, 11); // EVU article and EVU category article template
    List<ArticleQueryResult> aqr = cpAccessor.getArticleQueryResult(articleQuery);
    assertNotNull(aqr);
    assertFalse(aqr.isEmpty());
  }
  
  @Test
  public void testSiteIds() {
    List<Integer> siteIds = cpAccessor.getSiteIds();
    assertNotNull(siteIds);
    assertFalse(siteIds.isEmpty());
  }
  
  @Test
  public void testSiteIdsManual() {
    XmlRequest xmlRequest = new XmlRequest(cpAccessor, "cplib4j.SiteListService");
    xmlRequest.addParameter("siteID", 1);
    Element sites = xmlRequest.execute();
    
    assertNotNull(sites);
    
  }
}
