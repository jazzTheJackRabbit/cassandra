import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class TemplateSearcher {
	//Create templates based on a query
	//@param query: Query to be templated
	public static void getTemplates(String query, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		//Remove any question marks from the query
		query = query.replace("?","");		
		//Convert string to all lowercase to avoid case sensitivity issues
		query = query.toLowerCase();
		
		query = query.replace("for which","what");
		query = query.replace("which","what");
		
		//Find the roving word
		String rove_word = findRovingWord(query);
		
		//Find the question type
		//ASSUMPTION: ONLY ONE QUESTION WORD PER QUERY
		if(query.contains("how many"))
			how_many_templates(query,rove_word,templates,weights,look_loc);
		else if(query.contains("how"))
			how_templates(query,rove_word,templates,weights,look_loc);
		else if(query.contains("why"))
			why_templates(query,rove_word,templates,weights,look_loc);
		else if(query.contains("what"))
			what_templates(query,rove_word,templates,weights,look_loc);
		else if(query.contains("who"))
			who_templates(query,rove_word,templates,weights,look_loc);
		else if(query.contains("where"))
			where_templates(query,rove_word,templates,weights,look_loc);
		else
			when_templates(query,rove_word,templates,weights,look_loc);	
	}	
		
	//Find the verb that will be moved throughout the template
	public static String findRovingWord(String query)
	{
		// Initialize the tagger and POS tag the query
		MaxentTagger tagger = new MaxentTagger("taggers/english-bidirectional-distsim.tagger");
		String tagged_query = tagger.tagString(query);
		
		//Find the position of the _VB tag
		int pos = tagged_query.indexOf("_VB");
		
		//Find the beginning of the word to remove (the roving word)
		int begin = pos;
		while(begin >= 0 && tagged_query.charAt(begin) != ' ')
			begin--;
		begin++;
	
		//If there is no verb (error in the POS parser), return an empty string
		if(pos == -1)
			return "";
		
		//Get the roving word
		return tagged_query.substring(begin,pos);
	}
	
	//Search files
	//@param br: Buffered reader to get user input
	//@param indexLocation: Directory of the index to search
	public static ArrayList<Document> SearchFiles(ArrayList<String> templates, String indexLocation, StandardAnalyzer analyzer) throws ParseException, IOException
	{
		  ArrayList<Document> foundDocs = new ArrayList<Document>();
		  IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation)));
		  IndexSearcher searcher = new IndexSearcher(reader);
		  TopScoreDocCollector collector = TopScoreDocCollector.create(100);
		  
		  String s;
		  for (int j = 0; j < templates.size(); j++) 
		  {
			  s = templates.get(j);
			  Query q = new QueryParser("contents", analyzer).parse(s);
			  searcher.search(q, collector);
			  ScoreDoc[] hits = collector.topDocs().scoreDocs;

			  for(int i=0;i<hits.length;++i) 
			  {
				  int docId = hits[i].doc;
				  foundDocs.add(searcher.doc(docId));
				  //System.out.println((i + 1) + ". " + foundDocs.get(foundDocs.size() - 1).get("path") + " score=" + hits[i].score);
			  }

		  }
		  return foundDocs;
	}
		
	public static void how_many_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		//Template 1: Existential there
		String template1 = query.replace("how many ","");
		template1 = template1.replace("there","");
		
		if(!rove_word.equals(""))
			template1 = template1.replace(rove_word + " ", "");
		
		template1 = "AND " + template1;
		template1 = "there " + template1;
		template1 = template1.replace("  ", " ");		
		templates.add(rove_word + " " + template1);
		weights.add(1.0);
		look_loc.add(0);
		String add_string;
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " " + rove_word + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(0);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " " + rove_word;
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(0);
			}
		}
		
		//Template 2: Generic Search
		String template2 = query.replace("how many ","");
		templates.add(template2);
		weights.add(0.5);
		look_loc.add(1);
		
		//Template 3: AND
		String template3 = query.replace("how many ","");
		for(int i = 0; i < template3.length(); i++)
		{
			if(template3.charAt(i) == ' ' && i != template3.length() - 1)
			{
				template3 = template3.substring(0,i) + " AND" + template3.substring(i, template3.length());
				i += 5;
			}
		}
		templates.add(template3);
		weights.add(0.1);
		look_loc.add(0);
	}
	
	public static void how_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		query = query.replace("how ","");
		
		//Template 1: How to complete an action (rove first verb)
		String template1 = query.replace(rove_word + " ", "");
		template1 = template1 + " by";		
		templates.add(rove_word + " " + template1);
		weights.add(1.0);
		look_loc.add(2);
		String add_string;
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " " + rove_word + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " " + rove_word;
				templates.add(add_string);
				weights.add(1.0);	
				look_loc.add(2);
			}
		}
		
		//Template 2: How to complete an action (rove second verb)
		String template2 = query.replace(rove_word + " ", "");
		String second_rove = findRovingWord(template2);
		if(second_rove != "")
		{
			template2 = template2 + " by";
			templates.add(second_rove + " " + template2);
			weights.add(1.0);
			look_loc.add(2);
			for(int i = 0; i < template2.length(); i++)
			{
				if(template2.charAt(i) == ' ')
				{
					add_string = template2.substring(0,i) + " " + second_rove + template2.substring(i, template2.length());
					templates.add(add_string);
					weights.add(1.0);
					look_loc.add(2);
				}
				else if(i == template1.length() - 1)
				{
					add_string = template2 + " " + second_rove;
					templates.add(add_string);
					weights.add(1.0);	
					look_loc.add(2);
				}
			}
		}
		else
		{
			template2 = template2 + " by";
			templates.add(template2);
			weights.add(1.0);
			look_loc.add(2);
		}
			
		//Template 3: AND
		String template3 = query;
		for(int i = 0; i < template3.length(); i++)
		{
			if(template3.charAt(i) == ' ' && i != template3.length() - 1)
			{
				template3 = template3.substring(0,i) + " AND" + template3.substring(i, template3.length());
				i += 5;
			}
		}
		templates.add(template3);
		weights.add(0.1);
		look_loc.add(0);
	}	
	
	public static void what_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		query = query.replace("what ","");
		
		//Template 1: Definition based what
		String template1 = query.replace(rove_word, "");
		//Add without any rove word
		templates.add(template1);
		weights.add(1.0);	
		look_loc.add(2);
		template1 = template1.replace("  ", " ");
		String add_string;
		//Rove "to be" in several tenses
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ' || i == template1.length())
			{
				add_string = template1.substring(0,i) + " is" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " was" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " will be" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
		}
		
		//Template 2: AND
		String template2 = query;
		for(int i = 0; i < template2.length(); i++)
		{
			if(template2.charAt(i) == ' ' && i != template2.length())
			{
				template2 = template2.substring(0,i) + " AND" + template2.substring(i, template2.length());
				i += 5;
			}
		}
		templates.add(template2);
		weights.add(0.1);
		look_loc.add(0);
	}
	
	public static void why_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		query = query.replace("why ","");
		
		//Template 1: Add "because"
		String template1 = query.replace(rove_word + " ", "");
		template1 = template1.replace("  ", " ");
		template1 = template1 + " because";
		templates.add(template1);
		weights.add(0.8);
		look_loc.add(2);
		String add_string;
		//Rove rove word
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " " + rove_word + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
		}
		
		//Template 2: Add "due to"
		String template2 = query.replace(rove_word + " ", "");
		template2 = template2 + " due to";
		//Rove "to be" in several tenses
		for(int i = 0; i < template2.length(); i++)
		{
			if(template2.charAt(i) == ' ')
			{
				add_string = template2.substring(0,i) + " " + rove_word + template2.substring(i, template2.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
		}
		
		//Template 3: AND
		String template3 = query;
		for(int i = 0; i < template3.length(); i++)
		{
			if(template3.charAt(i) == ' ' && i != template3.length() - 1)
			{
				template3 = template3.substring(0,i) + " AND" + template3.substring(i, template3.length());
				i += 5;
			}
		}
		templates.add(template3);
		weights.add(0.1);	
		look_loc.add(0);
	}

	public static void who_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		query = query.replace("who ","");
		
		//Template 1: Keep phrase
		String template1 = query;
		templates.add(template1);
		weights.add(1.0);
		look_loc.add(2);
		//Move rove word to end
		template1 = template1.replace(rove_word + " ","");
		templates.add(template1 + " " + rove_word);
		weights.add(1.0);
		look_loc.add(2);
		
		//Template 2: AND
		String template2 = query;
		for(int i = 0; i < template2.length(); i++)
		{
			if(template2.charAt(i) == ' ' && i != template2.length() - 1)
			{
				template2 = template2.substring(0,i) + " AND" + template2.substring(i, template2.length());
				i += 5;
			}
		}
		templates.add(template2);
		weights.add(0.1);
		look_loc.add(0);
	}

	public static void where_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		query = query.replace("where ","");
		
		//Template 1: is, is in, in
		String template1 = query;
		template1 = template1.replace(rove_word + " ","");
		template1 = template1.replace("  "," ");
		String add_string;
		//Rove "to be" in several tenses
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " is" + template1.substring(i, template1.length());
				templates.add(add_string);
				look_loc.add(2);
				weights.add(1.0);
				add_string = template1.substring(0,i) + " was" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " will be" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " is";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1 + " was";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1 + " will be";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);				
			}
		}
		//Rove "to be" + "in" in several tenses
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " is in" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " was in" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " will be in" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " is in";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1 + " was in";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1 + " will be in";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
		}
		//Rove "in"
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " in" + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(0.5);
				look_loc.add(2);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " in";
				templates.add(add_string);
				weights.add(0.5);
				look_loc.add(2);
			}
		}
		
		//Template 2: AND
		String template2 = query;
		for(int i = 0; i < template2.length(); i++)
		{
			if(template2.charAt(i) == ' ' && i != template2.length() - 1)
			{
				template2 = template2.substring(0,i) + " AND" + template2.substring(i, template2.length());
				i += 5;
			}
		}
		templates.add(template2);
		weights.add(0.1);
		look_loc.add(0);
	}		
	
	public static void when_templates(String query, String rove_word, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<Integer> look_loc)
	{
		query = query.replace("when ","");
		
		//Template 1: is in, in
		String template1 = query;
		template1 = template1.replace(rove_word,"");
		template1 = template1.replace("  "," ");
		String add_string;
		//Rove "to be" + "in" in several tenses
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " is in " + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " was in " + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1.substring(0,i) + " will be in " + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " is in ";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1 + " was in ";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
				add_string = template1 + " will be in ";
				templates.add(add_string);
				weights.add(1.0);
				look_loc.add(2);
			}
		}
		//Rove "in"
		for(int i = 0; i < template1.length(); i++)
		{
			if(template1.charAt(i) == ' ')
			{
				add_string = template1.substring(0,i) + " in " + template1.substring(i, template1.length());
				templates.add(add_string);
				weights.add(0.5);
				look_loc.add(2);
			}
			else if(i == template1.length() - 1)
			{
				add_string = template1 + " in ";
				templates.add(add_string);
				weights.add(0.5);
				look_loc.add(2);
			}
		}
		
		//Template 2: AND
		String template2 = query;
		for(int i = 0; i < template2.length(); i++)
		{
			if(template2.charAt(i) == ' ' && i != template2.length() - 1)
			{
				template2 = template2.substring(0,i) + " AND" + template2.substring(i, template2.length());
				i += 5;
			}
		}
		templates.add(template2);
		weights.add(0.1);
		look_loc.add(0);
	}				
}