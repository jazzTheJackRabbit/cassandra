import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class TREC {
	
	public TREC(){
		
	}
	
	public ArrayList<Target> parseXML(){
		BufferedReader br;
		ArrayList<Target> targets = new ArrayList<Target>();
		try {
			br = new BufferedReader(new FileReader("eval/QA2007_testset.xml"));	    
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append(System.lineSeparator());
	            line = br.readLine();
	        }
	        String everything = sb.toString();
	        br.close();	    	    
	    
			String html = everything.toString();			
		    Document doc = Jsoup.parse(html, "", Parser.xmlParser());
		    for (Element e : doc.select("target")) {
		    	Elements questionsXML = e.getElementsByTag("q");
		    	Target target = new Target();
		    	target.targetContext = e.attr("text");
		    	for(Element question : questionsXML){
		    		if(question.attr("type").contains("FACTOID")){		    			
		    			String questionString = question.text().toLowerCase();
		    			
		    			String coreferenceResolvedQuestion = this.naiveCoreferenceResolution(target, questionString);    						    		

		    			target.questions.add(coreferenceResolvedQuestion);
		    			target.unresolvedQuestions.add(questionString);
		    		}
		    	}		      
		    	targets.add(target);
		    }		    		  
		}
		catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return targets;
	}
	
	public String naiveCoreferenceResolution(Target target,String question){		
		String[] pronounsList = {"he","she","they","it","his","him","her","its","hers","their","them"};
		StringBuilder coreferenceResolvedQuestion = null;
		for(String pronoun : pronounsList){
			String[] permutationsOfPronounInString = {" "+pronoun+" "," "+pronoun+"."," "+pronoun+","," "+pronoun+"?"};
			for(String permutation : permutationsOfPronounInString){
				if(question.contains(permutation)){
					
					if(coreferenceResolvedQuestion == null)
						coreferenceResolvedQuestion = new StringBuilder("");
					int positionToStartReplacementAt = question.indexOf(permutation);
					int positionToEndReplacementAt = positionToStartReplacementAt + permutation.length();
					
					coreferenceResolvedQuestion.append(question.substring(0, positionToStartReplacementAt));
					coreferenceResolvedQuestion.append(" "+target.targetContext);
					if(positionToEndReplacementAt != question.length() - 1){
						coreferenceResolvedQuestion.append(" "+question.substring(positionToEndReplacementAt,question.length()));
					}						
					
					break;
				}	
			}
			
			if(coreferenceResolvedQuestion != null){
				break;
			}
			else{
				String permutation = pronoun+" ";
				if(question.indexOf(permutation) == 0){
					
					if(coreferenceResolvedQuestion == null)
						coreferenceResolvedQuestion = new StringBuilder("");					
					int positionToEndReplacementAt = permutation.length();
										
					coreferenceResolvedQuestion.append(target.targetContext);
					if(positionToEndReplacementAt != question.length() - 1){
						coreferenceResolvedQuestion.append(" "+question.substring(positionToEndReplacementAt,question.length()));
					}						
					
					break;
				}	
			}
		}
		if(coreferenceResolvedQuestion == null){
			return question;
		}
		else{
			return coreferenceResolvedQuestion.toString();
		}		
	}
}
