import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher; 	
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds files into this index
 * based on the input of the user.
 */
public class LuceneTest {

  public static void main(String[] args) throws IOException, ParseException
  {
	  boolean useWebSearch = true;
	  
	  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	  StandardAnalyzer analyzer = new StandardAnalyzer();
	  
	  //Index the files
//	  Indexer fileIndexer = new Indexer(analyzer);
//	  String indexLocation = fileIndexer.getFiles(br);
	  String indexLocation = "lucene-index/";
	  
	  //Get query from user
	  String query = "Where is the Louvre?";
	  
	  
	  //Create templates, their weights, and where to search (i.e. left or right of the template)
	  ArrayList<String> templates = new ArrayList<String>();
	  ArrayList<Double> weights = new ArrayList<Double>();
	  ArrayList<Integer> look_locs = new ArrayList<Integer>();
	  TemplateSearcher.getTemplates(query,templates,weights, look_locs);	
	  	  
	  //Array list for storing the files from each *individual* search
	  ArrayList<Document> foundDocs;
	  //Array list storing each *individual* template (one search at a time)
	  ArrayList<String> search_query = new ArrayList<String>();
	  search_query.add("");
	  
	  //Array list containing the returned document
	  ArrayList<String> docs = new ArrayList<String>();
	  //Corresponds to 'docs', stores the associated template that found the doc
	  ArrayList<String> doc_templates = new ArrayList<String>();
	  //Corresponds to 'docs', stores the associated weight for the template that found the doc
	  ArrayList<Double> doc_weights = new ArrayList<Double>();
	  //Corresponds to 'docs', stores which side of the template to look for n-grams (0 = both, 1 = left, 2 = right)
	  ArrayList<Integer> doc_look_locs = new ArrayList<Integer>();
	  
	  
	  //Array list of template queries, which will have all the reformulated queries and their corresponding top fetched documents
	  ArrayList<TemplateQuery> templateQueries = new ArrayList<TemplateQuery>();
	  
	  //Search the files	  
	  if(useWebSearch){
		  //init the WebSearcher
		  WebSearch webSearch = new WebSearch();		  
		  for(int i = 3; i < templates.size(); i++)
		  {
			  TemplateQuery templateQuery = new TemplateQuery();
			  
			  templateQuery.queryString = templates.get(i);
			  templateQuery.weight = weights.get(i);
			  templateQuery.topFetchedDocuments = webSearch.getTopSearchSummaries(templateQuery.queryString);
			  templateQuery.lookLocation = look_locs.get(i);	
			  
			  templateQueries.add(templateQuery);
			  templateQuery.mineNGrams();
			  
			  System.out.println("log");
			  //TODO: Remove this.
			  break;
		  }
		  
		  TreeMap<String, Integer> rankedNGrams = sortHashMap(TemplateQuery.ngramCountMap);
		  System.out.println(rankedNGrams);
		  
	  }
	  else{		  
		  for(int i = 0; i < templates.size(); i++)
		  {
			  search_query.set(0,templates.get(i)); 
			  foundDocs = TemplateSearcher.SearchFiles(search_query, indexLocation, analyzer);
			  for(int j = 0; j < foundDocs.size(); j++)
			  {
				  docs.add(foundDocs.get(j).get("path"));
				  doc_templates.add(templates.get(i));
				  doc_weights.add(weights.get(i));
				  doc_look_locs.add(look_locs.get(i));
			  }
		  }
		  
		  //Lists storing the ngrams and their corresponding weights
		  ArrayList<String> ngrams = new ArrayList<String>();
		  ArrayList<Double> ngram_weights = new ArrayList<Double>();
		  //List of documents paths associated to each ngram (so we don't add weights for n-grams found in the same document)
		  ArrayList<String> ngram_docs = new ArrayList<String>();
		  
		  //Get n-grams
		  NGramMinder.mine(docs, doc_templates, doc_weights, ngrams, ngram_weights, ngram_docs, doc_look_locs);
			  
		  for(int i = 0; i < ngram_weights.size(); i++)
			  System.out.println(ngrams.get(i) + ": " + ngram_weights.get(i));
	  }	  
	  
	  //Combine and weight n-grams
	  //Reweight n-grams based on question rules
	  //Rank n-grams
	  //Tile n-grams
	  //Re-rank n-grams
	  //Print results
  }
  
  public static TreeMap<String, Integer> sortHashMap(HashMap<String, Integer> hashMap){
	  HashMap<String,Integer> map = hashMap;
      ValueComparator bvc =  new ValueComparator(map);
      TreeMap<String,Integer> sorted_map = new TreeMap<String,Integer>(bvc);

      System.out.println("Unsorted N-grams: "+map);

      sorted_map.putAll(map);

      System.out.println("Sorted N-grams: "+sorted_map);
      
      return sorted_map;
  }
}

class ValueComparator implements Comparator<String> {

    Map<String, Integer> base;
    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}