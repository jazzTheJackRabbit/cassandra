import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateQuery {	
	public String queryString;
	public double weight;
	public int lookLocation;
	
	public ArrayList<SearchResult> topFetchedDocuments;	
	public ArrayList<ProcessedAnswer> topProcessedAnswers;
	
	public static HashMap<String, Integer> ngramCountMap;
	public static ArrayList<String> ngramKeys;
	
	public static String[] stopwords = 
	{ 
		"a", "an", "and", "are", "as", "at", "be", "but", "by",
		"for", "if", "in", "into", "is", "it",
		"no", "not", "of", "on", "or", "such",
		"that", "the", "their", "then", "there", "these",
		"they", "this", "to", "was", "will", "with"
	};
	
	//These are the templates which are essentially the queries for the search engine.
	public TemplateQuery(HashMap<String, Integer> ngramCountMap, ArrayList<String> ngramKeys){
		this.queryString = "";
		this.weight = 0.0f;
		this.lookLocation = 0;
		
		this.topFetchedDocuments = new ArrayList<SearchResult>();							
		this.topProcessedAnswers = new ArrayList<ProcessedAnswer>();
		
		TemplateQuery.ngramCountMap = ngramCountMap;
		TemplateQuery.ngramKeys = ngramKeys;
	}
	
	public void mineNGrams(){
		//String containing the current template
		String curr_template; 
		TemplateQuery templateQuery = this;
		//Contains all subsets of the current template
		ArrayList<String> search_strings = new ArrayList<String>(); 
		
		//Position tracker
		int pos = 0;
		int begin = 0;
		
		//Loop through each document (sometimes more than once if it appeared for multiple templates)
		//Limit to 50 documents
		for(int i = 0; i < templateQuery.topFetchedDocuments.size(); i++)
		{
			//Split the template by 'AND's
			//ASSUMPTION: All characters were set to lowercase before adding the 'AND' to the template
			
			curr_template = templateQuery.queryString;
			SearchResult currentDocumentContent = templateQuery.topFetchedDocuments.get(i);
			
			pos = 0;
			begin = 0;
			while(pos < curr_template.length())
			{
				//Skip 'AND' if its at the beginning of the template
				if(pos == 0 && curr_template.charAt(pos) == 'A')
				{
					pos += 4;
					begin = pos;
				}
				
				//Check for the end of the string or an 'A'
				if((pos == curr_template.length() - 1 || curr_template.charAt(pos) == 'A') && pos != 0)
				{
					search_strings.add(curr_template.substring(begin,pos - 1));
					pos += 3;
					begin = pos;
				}				
				pos++;
			}
			
			this.getNGrams(templateQuery, currentDocumentContent);
			
		}
	}
	
	public void getNGrams(TemplateQuery templateQuery, SearchResult currentDocumentContent){
		//Get file
		String str = currentDocumentContent.Description.toLowerCase();
		String template = templateQuery.queryString;
		int look_loc = templateQuery.lookLocation;
		double weight = templateQuery.weight;
		
//		ArrayList<String> ngrams = new ArrayList<String>();
		ArrayList<String> ngram_docs = new ArrayList<String>();
//		ArrayList<Double> weights = new ArrayList<Double>();
		
		//Get rid of newlines
		str = str.replace("\n","");
		
		//Find beginning and of string
		Pattern p = Pattern.compile("\\b" + template + "\\b");
		Matcher m = p.matcher(str); 
		
		//If the template is not found, add nothing
		if(!m.find())
			return;
		System.out.println("Found match in document:"+currentDocumentContent.ID+" for:"+template);
		//System.out.println(str.substring(m.start(),m.end() - 1)); 
		  
		int pre_start = m.start();
		int pre_end = m.start();
		if(pre_start > 0){
			pre_start -= 1;
			pre_end -= 1;
		}
		
		int post_start = m.end();
		int post_end = m.end() + 1;
		  
		//Move past any spaces
		while(str.charAt(pre_start) == ' ')
		{
			pre_start--;
			pre_end = pre_start + 1;
		}
		while(str.charAt(post_start) == ' ')
		{
			post_start++;
			post_end++;
		}		  
		//Count of words added
		int count = 0;
		 
		//Marks whether to move character index counter forward one space or not (i.e. do not include a " " in an n-gram)
		int forward_space;
				
		//Create 1, 2 and 3 grams
		String ngram_holder = "";
		
		//Get 3 strings before
		ProcessedAnswer processedAnswer;
		if(look_loc == 0 || look_loc == 1)
		{
			
			while(pre_start >= 0 && count < 3)
			{
				if(pre_start == 0 || str.charAt(pre_start) == ' ')
				{						
					if(pre_start == 0 || str.charAt(pre_start) == ' ')
					{
						//Start 1 space earlier if at a space
						forward_space = 0;
						if(str.charAt(pre_start) == ' ')
							forward_space = 1;
						
						processedAnswer = new ProcessedAnswer(str.substring(pre_start + forward_space, pre_end).replace(" ",""));
						ngram_holder = processedAnswer.content + " " + ngram_holder;
						if(!checkStopWord(processedAnswer.content))
						{
							processedAnswer.weight = weight;
							templateQuery.topProcessedAnswers.add(processedAnswer);
							this.addNGramToHashMap(processedAnswer);

						}		
						
						if(count != 0)
						{
							processedAnswer = new ProcessedAnswer(ngram_holder);
							processedAnswer.weight = weight;
							templateQuery.topProcessedAnswers.add(processedAnswer);
							this.addNGramToHashMap(processedAnswer);
						}	
						
						count++;
						pre_end = pre_start;
					}
					pre_start--;	
				}  	  
			}  
		}
		
		//Get 3 strings after
		count = 0;
		ngram_holder = "";
		
		if(look_loc == 0 || look_loc == 2)
		{
			while(post_end < str.length() && count < 3)
			{
				if(post_end == str.length() - 1 || str.charAt(post_end) == ' ')
				{
					processedAnswer = new ProcessedAnswer(str.substring(post_start, post_end));
					ngram_holder += str.substring(post_start, post_end) + " ";	
					if(!checkStopWord(processedAnswer.content))
					{
						processedAnswer.weight = weight;
						templateQuery.topProcessedAnswers.add(processedAnswer);
						this.addNGramToHashMap(processedAnswer);
					}
					if(count != 0)
					{
						processedAnswer = new ProcessedAnswer(ngram_holder);
						processedAnswer.weight = weight;
						templateQuery.topProcessedAnswers.add(processedAnswer);
						this.addNGramToHashMap(processedAnswer);	
					}
					
					count++;
					post_start = post_end + 1;
				}
				post_end++;
			}
		}
		System.out.println("Before Quitting");
	}	
	
	public void addNGramToHashMap(ProcessedAnswer processedAnswer){
		if(TemplateQuery.ngramCountMap.containsKey(processedAnswer.content)){
			String key = processedAnswer.content;
			TemplateQuery.ngramCountMap.put(key, TemplateQuery.ngramCountMap.get(processedAnswer.content) + 1); 
		}
		else{
			String key = processedAnswer.content;
			TemplateQuery.ngramCountMap.put(key, 1);
			TemplateQuery.ngramKeys.add(key);
		}
	}
	
	public static boolean checkStopWord(String str)
	{
		for(int i = 0; i < stopwords.length; i++)
		{
			if(str.equals(stopwords[i]))
				return true;
		}
		return false;
	}
	
}
