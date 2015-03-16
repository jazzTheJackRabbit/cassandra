import java.util.ArrayList;

public class Target {
	public ArrayList<String> questions;
	public ArrayList<String> unresolvedQuestions;
	
	public String targetContext;
	
	public Target(){
		this.questions = new ArrayList<String>();
		this.unresolvedQuestions = new ArrayList<String>();
	}
}
