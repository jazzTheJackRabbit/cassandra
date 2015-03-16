public class SearchResult extends Object {
	public String ID;
	public String Title;
	public String Description;
	public String DisplayUrl;
	public String Url;
	
	public SearchResult() {
		// TODO Auto-generated constructor stub
	}
	
	@Override public String toString(){
		StringBuilder outputString = new StringBuilder();
		outputString.append("\n\nSearch Result:");
		outputString.append("\nID:"+this.ID);
		outputString.append("\nTitle:"+this.Title);
		outputString.append("\nDescription:"+this.Description);
		outputString.append("\nDisplayURL:"+this.DisplayUrl);
		outputString.append("\nURL:"+this.Url);				
		return outputString.toString();
	}
		
}


//"ID":"9e689b54-bf5c-4c35-b003-4a1914ba6b12",
//"Title":"Louvre Museum Official Website - Site officiel du musée ...",
//"Description":"Most viewed pages. Search the Collection; Search louvre.fr; Selected Works; Visitor Trails; Advance Tickets ; Calendar. Thursday Mar 12. Featured events. Exhibition",
//"DisplayUrl":"www.louvre.fr/en",
//"Url":"http://www.louvre.fr/en