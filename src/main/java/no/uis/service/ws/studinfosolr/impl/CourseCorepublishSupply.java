package no.uis.service.ws.studinfosolr.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.uis.fsws.studinfo.data.Kurs;
import no.uis.service.ws.studinfosolr.SolrType;
import no.uis.service.ws.studinfosolr.SolrUpdateListener;
import no.uis.studinfo.commons.Utils;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.corepublish.api.Accessor;
import com.corepublish.api.Article;
import com.corepublish.api.ArticleQuery;
import com.corepublish.api.ArticleQueryResult;
import com.corepublish.api.article.element.ArticleElement;
import com.corepublish.api.article.element.ArticleElementType;
import com.corepublish.api.article.element.NewTextElement;
import com.corepublish.api.article.element.ProgramaticElement;
import com.corepublish.api.article.element.TextElement;
import com.corepublish.api.article.richtext.HtmlFragmentToken;
import com.corepublish.api.article.richtext.Token;
import com.corepublish.impl.defaultt.DefaultArticleQuery;
import com.corepublish.impl.xml.XmlAccessorHolder;
import com.corepublish.util.DomainUrl;

@ManagedResource(
  objectName="uis:service=ws-studinfo-solr,component=course.corepublish.supplier",
  description="Corepublish Data Supplier",
  log=false
)
public class CourseCorepublishSupply implements SolrUpdateListener<Kurs> {

  private Map<String, CPArticleInfo> descriptionCache = new HashMap<String, CPArticleInfo>();
  private DomainUrl cpUrl;
  private int siteId; 
  private XmlAccessorHolder xmlAccessorHolder;
  private int evuCategoryId = 5793;
  private int[] evuTemplateIds;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CourseCorepublishSupply.class);

  @Override
  public void fireBeforeSolrUpdate(SolrType solrType, Kurs kurs, Map<String, Object> beanmap) {
    String kurskode = beanmap.get("kursid").toString();
    CPArticleInfo articleInfo = descriptionCache.get(kurskode);
    if (articleInfo != null) {
      beanmap.put("cp_article_id", articleInfo.getArticleId());
      beanmap.put("cp_category_id", articleInfo.getCategoryId());
      beanmap.put("cp_text", articleInfo.getText());
    }
  }

  @Override
  public void fireBeforePushElements(SolrType solrType, List<Kurs> courses) {
    fillDescriptionCache();
  }
  
  @Override
  public void fireAfterPushElements(SolrType solrType) {
    this.descriptionCache.clear();
  }

  @Override
  public void cleanup() {
    xmlAccessorHolder.destroy();
  }

  private void fillDescriptionCache() {
    Accessor cpAccessor = null;
    if (cpUrl != null) {
      try {
        cpAccessor = xmlAccessorHolder.getAccessor(cpUrl);
      } catch(Exception ex) {
        log.info("Corepublish", ex);
      }
    }
    if (cpAccessor == null) {
      log.warn("CorePublish access disabled");
      return;
    }
    ArticleQuery articleQuery = new DefaultArticleQuery().includeCategoryId(evuCategoryId, true).includeTemplateIds(evuTemplateIds);
    articleQuery.setSiteList(this.siteId);
    List<ArticleQueryResult> aqr = cpAccessor.getArticleQueryResult(articleQuery);

    List<Integer> articleIds = cpAccessor.getArticleIds(aqr);

    for (Integer articleId : articleIds) {

      try {
        Article article = cpAccessor.getArticle(articleId.intValue());
        List<ArticleElement> elements = article.getArticleElements().getElements();
        String kursKode = null;
        String tidKode = null;
        String mainText = "";
        for (ArticleElement articleElement : elements) {
          String pName = articleElement.getProgrammaticName();
          if (pName.equalsIgnoreCase("pluss_course_code")) {
            kursKode = getStringValue(articleElement, cpAccessor);
          } else if (pName.equalsIgnoreCase("pluss_time_code")) {
            tidKode = getStringValue(articleElement, cpAccessor);
          } else if (pName.equalsIgnoreCase("maintext")) {
            mainText = getStringValue(articleElement, cpAccessor);
          }
        }
        if (kursKode != null && tidKode != null) {
          CPArticleInfo cpinfo = new CPArticleInfo(Utils.formatTokens(kursKode, tidKode));
          cpinfo.setText(mainText);
          cpinfo.setArticleId(article.getId());
          cpinfo.setCategoryId(article.getMainCategoryId());
          descriptionCache.put(cpinfo.getId(), cpinfo);
        }
      } catch(Exception ex) {
        log.warn("Problem with article " + articleId, ex);
      }
    }
  }
  
  private String getStringValue(ArticleElement ae, Accessor cpAccessor) {
    ArticleElementType aeType = ae.getArticleElementType();

    String value;
    switch (aeType) {
        case PROGRAMMATIC:
          ProgramaticElement progElem = (ProgramaticElement)ae;
          value = cpAccessor.getProgrammaticElementObjectValue(progElem);
          break;

        case TEXT:
          TextElement txt = (TextElement)ae;
          value = txt.getText();
          break;

        case NEWTEXT:
          NewTextElement ntxt = (NewTextElement)ae;
          List<Token> tokens = ntxt.getContent().getTokens();
          StringBuilder sb = new StringBuilder();
          for (Token token : tokens) {
            if (token instanceof HtmlFragmentToken) {
              HtmlFragmentToken html = (HtmlFragmentToken)token;
              sb.append(html.getHtml());
              sb.append(' ');
            }
          }
          value = sb.toString();
          break;

        default:
          value = "";
          break;
      }
    return value;
  }

  private static class CPArticleInfo {
    private final String id;
    private String text;
    private int articleId;
    private int categoryId;

    public CPArticleInfo(String cacheId) {
      this.id = cacheId;
    }

    public void setText(String mainText) {
      this.text = mainText;
    }

    public void setArticleId(int id) {
      this.articleId = id;
    }

    public void setCategoryId(int evuCategoryId) {
      this.categoryId = evuCategoryId;
    }

    public String getId() {
      return id;
    }

    public String getText() {
      return text;
    }

    public int getArticleId() {
      return articleId;
    }

    public int getCategoryId() {
      return categoryId;
    }
  }
  
  @ManagedOperation(description="Set Cp Url")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="cpUrl", description="Corepublish URL: <url>|domainId")
  })
  public void setCpUrl(String cpUrl) {
    this.cpUrl = DomainUrlAdapter.valueOf(cpUrl);
  }

  @ManagedOperation(description="Get the corepublish URL")
  public String getCpUrl() {
    return this.cpUrl.toString();
  }
  
  @ManagedOperation(description="Set siteId")
  public void setSiteId(int siteId) {
    this.siteId = siteId;
  }
  
  @ManagedOperation(description="Get siteId")
  public int getSiteId() {
    return this.siteId;
  }
  
  public void setCpAccessorHolder(XmlAccessorHolder holder) {
    this.xmlAccessorHolder = holder;
  }

  @ManagedOperation(description="Set Course category root Id")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="evuCategoryId", description="Category Id of EVU Courses Root")
  })
  public void setEvuCategoryId(int evuCategoryId) {
    this.evuCategoryId = evuCategoryId;
  }

  @ManagedOperation(description="Get Course category root Id")
  public int getEvuCategoryId() {
    return evuCategoryId;
  }

  private synchronized int[] _getEvuTemplateIds() {
    return evuTemplateIds;
  }

  private synchronized void _setEvuTemplateIds(int[] ids) {
    this.evuTemplateIds = ids;
  }

  @ManagedOperation(description="Set Course Article Template IDs")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name="csvString", description="comma-separated list of article template ids")
  })
  public void setEvuTemplateIds(String csvString) {
    String[] strArray = csvString.split("\\s*,\\s*");
    int[] ids = new int[strArray.length];
    int i = 0;
    for (String s : strArray) {
      ids[i++] = Integer.parseInt(s);
    }
    _setEvuTemplateIds(ids);
  }

  @ManagedOperation(description="Get Course Article Template IDs")
  public String getEvuTemplateIds() {
    int[] ids = _getEvuTemplateIds();
    if (ids == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (int i : ids) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(i);
    }
    return sb.toString();
  }
  
  private static class DomainUrlAdapter extends DomainUrl {

    private static final long serialVersionUID = 1L;

    public DomainUrlAdapter(String cpUrl, int domainId) {
      super(cpUrl, domainId);
    }
    
    private static DomainUrlAdapter valueOf(String domainUrl) {
      String[] urlParts = domainUrl.split("\\|");
      return new DomainUrlAdapter(urlParts[0], Integer.parseInt(urlParts[1]));
    }
    
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getUrl());
      sb.append('|');
      sb.append(this.getDomainId());
      return sb.toString();
    }
  }
}