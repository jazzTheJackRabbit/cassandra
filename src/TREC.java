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
	public String trecQueryPath;
	
	public TREC(String queryListPath){
		this.trecQueryPath = queryListPath;
	}
	
	public static void main(String[] args){
		parseXML();
	}
	
	public static void parseXML(){
		BufferedReader br;
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
			ArrayList<Target> targets = new ArrayList<Target>();
		    Document doc = Jsoup.parse(html, "", Parser.xmlParser());
		    for (Element e : doc.select("target")) {
		    	Elements questionsXML = e.getElementsByTag("q");
		    	Target target = new Target();
		    	for(Element question : questionsXML){
		    		if(question.attr("type").contains("FACTOID")){
		    			System.out.println(question.text());		    			
		    			target.questions.add(question.text());		    			
		    		}
		    	}		      
		    	targets.add(target);
		    }
		    System.out.println("");
		}
		catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
