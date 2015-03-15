import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;

import org.apache.commons.io.FileUtils;

public class NGramMinder {
	public static void mine(ArrayList<String> docs, ArrayList<String> templates, ArrayList<Double> weights, ArrayList<String> ngrams, ArrayList<Double> ngram_weights) throws IOException
	{		
		//String containing the current template
		String curr_template;
		
		//Contains all subsets of the current template
		ArrayList<String> search_strings = new ArrayList<String>(); 
		
		//Position tracker
		int pos = 0;
		int begin = 0;
		
		//Loop through each document (sometimes more than once if it appeared for multiple templates)
		for(int i = 0; i < docs.size(); i++)
		{
			//Split the template by 'AND's
			//ASSUMPTION: All characters were set to lowercase before adding the 'AND' to the template
			curr_template = templates.get(i);
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
			
			//Search for an *exact* match of each template subset
			for(int j = 0; j < search_strings.size(); j++)
				getNGrams(docs.get(i), ngrams, ngram_weights, weights.get(i),search_strings.get(j));			
		}
	}
	
	//If the template is found, create 6 1-grams, 3 from each side and add them to the list of n-grams, along with their corresponding weights
	public static void getNGrams(String path, ArrayList<String> ngrams, ArrayList<Double> weights, double weight, String template)
	{
		  //Get file
		  String str = "";
		  try {
			   str = FileUtils.readFileToString(new File(path));
			 } catch (IOException e) {
			    e.printStackTrace();
			}
		  
		  //Get rid of newlines
		  str = str.replace("\n","");
		  
		  //Find beginning and of string
		  Pattern p = Pattern.compile("\\b" + template + "\\b");
		  Matcher m = p.matcher(str);
		  
		  //If the template is not found, add nothing
		  if(!m.find())
			  return;
		  
		  int pre_start = m.start() - 1;
		  int pre_end = m.start() - 1;
		  int post_start = m.end() + 1;
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
		  
		  //Get 3 strings before
		  while(pre_start >= 0 && count < 3)
		  {
			  if(pre_start == 0 || str.charAt(pre_start) == ' ')
			  {
				  if(str.charAt(pre_start) == ' ')
					  ngrams.add(str.substring(pre_start + 1, pre_end));
				  else
					  ngrams.add(str.substring(pre_start, pre_end));
				  
				  weights.add(weight);
				  count++;
				  pre_end = pre_start;
			  }
			  pre_start--;		  
		  }
		  
		  //Get 3 strings after
		  count = 0;
		  while(post_end < str.length() && count < 3)
		  {
			  if(post_end == str.length() - 1 || str.charAt(post_end) == ' ')
			  {
				  ngrams.add(str.substring(post_start, post_end));
				  weights.add(weight);
				  count++;
				  post_start = post_end + 1;
			  }
			  post_end++;
		  }
		  
	}
}
