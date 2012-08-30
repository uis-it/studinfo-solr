package no.uis.service.component.studinfosolr.mock;

import static org.easymock.EasyMock.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;

import com.corepublish.api.Article;
import com.corepublish.api.ArticleQuery;
import com.corepublish.api.ArticleQueryResult;
import com.corepublish.api.article.richtext.Token;
import com.corepublish.impl.defaultt.DefaultArticle;
import com.corepublish.impl.defaultt.DefaultArticleElementList;
import com.corepublish.impl.defaultt.article.element.DefaultNewTextElement;
import com.corepublish.impl.defaultt.article.element.DefaultProgramaticElement;
import com.corepublish.impl.defaultt.article.richtext.DefaultHtmlFragmentToken;
import com.corepublish.impl.defaultt.article.richtext.DefaultRichText;
import com.corepublish.impl.xml.XmlAccessor;
import com.corepublish.impl.xml.XmlAccessorHolder;
import com.corepublish.util.DomainUrl;

public class XmlAccessorHolderFactoryBean implements FactoryBean<XmlAccessorHolder> {

  
  @Override
  public XmlAccessorHolder getObject() throws Exception {
    XmlAccessorHolder holder = createMock(XmlAccessorHolder.class);
    
    XmlAccessor accessor = createMock(XmlAccessor.class);
    
    DefaultArticle article = new DefaultArticle();
    article.setId(1);
    article.setMainCategoryId(1);
    DefaultArticleElementList elements = new DefaultArticleElementList();
    article.setElements(elements);
    
    DefaultProgramaticElement kurskode = new DefaultProgramaticElement();
    //kurskode.setName("kurskode");
    kurskode.setProgrammaticName("kurskode");
    elements.add(kurskode);

    DefaultProgramaticElement tidskode = new DefaultProgramaticElement();
    //tidskode.setName("tidskode");
    tidskode.setProgrammaticName("tidskode");
    elements.add(tidskode);

    DefaultNewTextElement mainText = new DefaultNewTextElement();
    mainText.setProgrammaticName("mainText");
    DefaultRichText content = new DefaultRichText();
    Token token = new DefaultHtmlFragmentToken("This is <b>test</b> text");
    content.addToken(token);
    mainText.setContent(content);
    elements.add(mainText);
    
    expect(holder.getAccessor(anyObject(DomainUrl.class))).andStubReturn(accessor);

    expect(accessor.getArticleQueryResult(anyObject(ArticleQuery.class))).andStubReturn(Collections.<ArticleQueryResult>emptyList());
    @SuppressWarnings("unchecked")
    List<ArticleQueryResult> anyResult = anyObject(List.class);
    expect(accessor.getArticleIds(anyResult)).andReturn(Arrays.asList(1)).atLeastOnce();
    @SuppressWarnings("unchecked")
    List<Integer> anyArticleIds = anyObject(List.class);
    expect(accessor.getArticles(anyArticleIds)).andReturn(Arrays.asList((Article)article)).atLeastOnce();
    expect(accessor.getProgrammaticElementObjectValue(eq(kurskode))).andReturn("PBYGG").atLeastOnce();
    expect(accessor.getProgrammaticElementObjectValue(eq(tidskode))).andReturn("2011V").atLeastOnce();
    
    replay(accessor, holder);
    return holder;
  }

  @Override
  public Class<XmlAccessorHolder> getObjectType() {
    return XmlAccessorHolder.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }
}
