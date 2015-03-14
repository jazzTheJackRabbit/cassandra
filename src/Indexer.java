import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class Indexer {
	  private IndexWriter writer;
	  private ArrayList<File> queue = new ArrayList<File>();
	  private static StandardAnalyzer analyzer;
	  
	  public Indexer(StandardAnalyzer inAnalyzer)
	  {
		  analyzer = inAnalyzer;
	  }
	
	  //Get the directory to index the files and the files to be indexed. Then index them
	  //@param br: The buffered reader for getting user input
	  //@return the location of the index's directory
	  public String getFiles(BufferedReader br) throws IOException
	  {
		    System.out.println("Enter the path where the index will be created: (e.g. /tmp/index or c:\\temp\\index)");

		    String indexLocation = null;
		    String s = br.readLine();

		    //LuceneTest indexer = null;
		    try {
		      indexLocation = s;
		      Directory dir = FSDirectory.open(Paths.get(s));
		      IndexWriterConfig config = new IndexWriterConfig(analyzer);
		      writer = new IndexWriter(dir, config);
		      //indexer = new LuceneTest(s);
		    } catch (Exception ex) {
		      System.out.println("Cannot create index..." + ex.getMessage());
		      System.exit(-1);
		    }

		    //===================================================
		    //read input from user until he enters q for quit
		    //===================================================
		    while (!s.equalsIgnoreCase("q")) {
		      try {
		        System.out.println("Enter the full path to add into the index (q=quit): (e.g. /home/ron/mydir or c:\\Users\\ron\\mydir)");
		        System.out.println("[Acceptable file types: .xml, .html, .html, .txt]");
		        s = br.readLine();
		        if (s.equalsIgnoreCase("q")) {
		          break;
		        }

		        //try to add file into the index
		        indexFileOrDirectory(s);
		      } catch (Exception e) {
		        System.out.println("Error indexing " + s + " : " + e.getMessage());
		      }
		    }

		    //===================================================
		    //after adding, we always have to call the
		    //closeIndex, otherwise the index is not created    
		    //===================================================
		    closeIndex();
		    
		    return indexLocation;
	  }
	  
	  /**
	   * Indexes a file or directory
	   * @param fileName the name of a text file or a folder we wish to add to the index
	   * @throws java.io.IOException when exception
	   */
	  public void indexFileOrDirectory(String fileName) throws IOException 
	  {
		  //===================================================
		  //gets the list of files in a folder (if user has submitted
		  //the name of a folder) or gets a single file name (is user
		  //has submitted only the file name) 
		  //===================================================
		  addFiles(new File(fileName));
	    
		  int originalNumDocs = writer.numDocs();
		  for (File f : queue) 
		  {
			  FileReader fr = null;
			  try {
				  Document doc = new Document();

				  //===================================================
				  //add contents of file
				  //===================================================
				  fr = new FileReader(f);
				  doc.add(new TextField("contents", fr));
				  doc.add(new StringField("path", f.getPath(), Field.Store.YES));
				  doc.add(new StringField("filename", f.getName(), Field.Store.YES));

				  writer.addDocument(doc);
				  System.out.println("Added: " + f);
			  } catch (Exception e) {
				  System.out.println("Could not add: " + f);
			  } finally {
				  fr.close();
			  }
		  }
		  
		  int newNumDocs = writer.numDocs();
		  System.out.println("");
		  System.out.println("************************");
		  System.out.println((newNumDocs - originalNumDocs) + " documents added.");
		  System.out.println("************************");
		  
		  queue.clear();
	  }
	
	  private void addFiles(File file) 
	  {
		  if (!file.exists())
			  System.out.println(file + " does not exist.");

		  if (file.isDirectory()) 
		  {
			  for (File f : file.listFiles())
				  addFiles(f);
		  } 
		  else 
		  {
			  String filename = file.getName().toLowerCase();
		      
		      // Only index text files
			  if (filename.endsWith(".htm") || filename.endsWith(".html") || 
					  filename.endsWith(".xml") || filename.endsWith(".txt"))
				  queue.add(file);
		      else
		        System.out.println("Skipped " + filename);
		  }
	  }

	  /**
	   * Close the index.
	   * @throws java.io.IOException when exception closing
	   */
	  public void closeIndex() throws IOException 
	  {
		  writer.close();
	  }  
}