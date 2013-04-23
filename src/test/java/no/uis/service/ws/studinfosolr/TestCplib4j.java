package no.uis.service.ws.studinfosolr;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.jdom.Element;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.corepublish.api.ArticleQuery;
import com.corepublish.api.ArticleQueryResult;
import com.corepublish.impl.defaultt.DefaultArticleQuery;
import com.corepublish.impl.xml.XmlAccessor;
import com.corepublish.impl.xml.XmlAccessorHolder;
import com.corepublish.impl.xml.XmlAccessorHolder2;
import com.corepublish.impl.xml.XmlRequest;
import com.corepublish.util.DomainUrl;

public class TestCplib4j {

  private static XmlAccessorHolder accessorHolder = new XmlAccessorHolder2();
  private XmlAccessor cpAccessor;

  @Before
  public void initAccessor() throws Exception {
    File configFile = new File(System.getProperty("user.home"), "ws-studinfo-solr.xml");
    Assume.assumeTrue(configFile.canRead());
    
    Properties configProps = new Properties();
    configProps.loadFromXML(new FileInputStream(configFile));

    String cpUrl = configProps.getProperty("cplib4j.url");
    int domainId = Integer.parseInt(configProps.getProperty("cplib4j.domainId"));
    try {
      cpAccessor = (XmlAccessor)accessorHolder.getAccessor(new DomainUrl(cpUrl, domainId));
    } catch (Exception ex) {
      Assume.assumeNoException(ex);
    }
  }
  
  @After
  public void destroyAccessor() {
    if (cpAccessor != null) {
      cpAccessor.shutdown();
    }
  }
  
  @AfterClass
  public static void destroyAccessorHolder() {
    accessorHolder.destroy();
  }
  
  @Test
  public void testCplib4j() {
    Assume.assumeNotNull(cpAccessor);
    ArticleQuery articleQuery = new DefaultArticleQuery();
    //articleQuery.includeCategoryId(5793, true);
    articleQuery.setSiteList(1, 21);
    articleQuery.includeTemplateIds(10,11,22); // EVU article and EVU category article template
    List<ArticleQueryResult> aqr = cpAccessor.getArticleQueryResult(articleQuery);
    assertNotNull(aqr);
    assertFalse(aqr.isEmpty());
  }
  
  // disabled: makes Corepublish hang: reported to corepublish
  //@Test
  public void testSiteIds() {
    Assume.assumeNotNull(cpAccessor);
    List<Integer> siteIds = cpAccessor.getSiteIds();
    assertNotNull(siteIds);
    assertFalse(siteIds.isEmpty());
  }
  
  // disabled: makes Corepublish hang: reported to corepublish
  //@Test
  public void testSiteIdsManual() {
    Assume.assumeNotNull(cpAccessor);
    XmlRequest xmlRequest = new XmlRequest(cpAccessor, "cplib4j.SiteListService");
    xmlRequest.addParameter("site_id", 1);
    Element sites = xmlRequest.execute();
    
    assertNotNull(sites);
  }
}
