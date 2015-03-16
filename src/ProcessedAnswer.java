public class ProcessedAnswer {
	public int wordLength;
	public String content;
	public double score;
	public double weight;
	
	public ProcessedAnswer(String content){
		this.wordLength = 0;
		this.content = content;
		this.score = 0;
		this.weight = 0;
	}
}
