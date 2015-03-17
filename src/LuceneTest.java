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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds
 * files into this index based on the input of the user.
 */
public class LuceneTest {

	public static void main(String[] args) throws IOException, ParseException {
		// Get query from user
		TREC trecParser = new TREC();
		ArrayList<Target> TRECQueries = trecParser.parseXML();

		HashMap<String, Integer> ngramCountMap = new HashMap<String, Integer>();
		ArrayList<String> ngramKeys = new ArrayList<String>();

		for (int trecQueryTargetIndex = 7; trecQueryTargetIndex <= TRECQueries
				.size(); trecQueryTargetIndex++) {
			Target trecQueryTarget = TRECQueries.get(trecQueryTargetIndex);
			ArrayList<String> querySetForEachTarget = trecQueryTarget.questions;
			for (int queryIndex = 0; queryIndex <= querySetForEachTarget.size(); queryIndex++) {
				// String primaryUnformulatedQuery =
				// querySetForEachTarget.get(queryIndex);
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		        System.out.print("Enter your Question...(or type TREC to pick a TREC query)\n:");		        
				String primaryUnformulatedQuery = br.readLine();				
				if(primaryUnformulatedQuery.equals("TREC")){
					primaryUnformulatedQuery = querySetForEachTarget.get((int) Math.round(Math.random()*(querySetForEachTarget.size()-1)));
				}
				reformulateQueryAndFindAnswers(primaryUnformulatedQuery,
						ngramCountMap, ngramKeys);
				break;
			}
			break;
		}
	}

	public static void reformulateQueryAndFindAnswers(
			String primaryUnformulatedQuery,
			HashMap<String, Integer> ngramCountMap, ArrayList<String> ngramKeys) {
		// #############################################################################
		// #############################################################################
		// #############################################################################
		boolean useWebSearch = true;

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		StandardAnalyzer analyzer = new StandardAnalyzer();

		String query = primaryUnformulatedQuery;

		// Create templates, their weights, and where to search (i.e. left or
		// right of the template)
		ArrayList<String> templates = new ArrayList<String>();
		ArrayList<Double> weights = new ArrayList<Double>();
		ArrayList<Integer> look_locs = new ArrayList<Integer>();
		TemplateSearcher.getTemplates(query, templates, weights, look_locs);

		// Array list for storing the files from each *individual* search
		ArrayList<Document> foundDocs;
		// Array list storing each *individual* template (one search at a time)
		ArrayList<String> search_query = new ArrayList<String>();
		search_query.add("");

		// Array list containing the returned document
		ArrayList<String> docs = new ArrayList<String>();
		// Corresponds to 'docs', stores the associated template that found the
		// doc
		ArrayList<String> doc_templates = new ArrayList<String>();
		// Corresponds to 'docs', stores the associated weight for the template
		// that found the doc
		ArrayList<Double> doc_weights = new ArrayList<Double>();
		// Corresponds to 'docs', stores which side of the template to look for
		// n-grams (0 = both, 1 = left, 2 = right)
		ArrayList<Integer> doc_look_locs = new ArrayList<Integer>();

		// Array list of template queries, which will have all the reformulated
		// queries and their corresponding top fetched documents
		ArrayList<TemplateQuery> templateQueries = new ArrayList<TemplateQuery>();

		// Search the files
		if (useWebSearch) {
			// init the WebSearcher
			WebSearch webSearch = new WebSearch();
			for (int i = 0; i < templates.size(); i++) {
				TemplateQuery templateQuery = new TemplateQuery(ngramCountMap,
						ngramKeys);

				templateQuery.queryString = templates.get(i);
				templateQuery.weight = weights.get(i);
				templateQuery.topFetchedDocuments = webSearch.getTopSearchSummaries(templateQuery.queryString);
				templateQuery.lookLocation = look_locs.get(i);

				templateQueries.add(templateQuery);
				templateQuery.mineNGrams();

				System.out.println("log");
				// //TODO: Remove this.
				// break;
			}

			TreeMap<String, Integer> rankedNGrams = sortHashMap(TemplateQuery.ngramCountMap);
			System.out.println(rankedNGrams);
			HashMap<String, Double> tiledNGrams = tileNGrams(rankedNGrams);
			System.out.println("\nTiled N-Grams: " + tiledNGrams);

		} else {
			// for(int i = 0; i < templates.size(); i++)
			// {
			// //Index the files
			// // Indexer fileIndexer = new Indexer(analyzer);
			// // String indexLocation = fileIndexer.getFiles(br);
			// String indexLocation = "lucene-index/";
			// search_query.set(0,templates.get(i));
			// foundDocs = TemplateSearcher.SearchFiles(search_query,
			// indexLocation, analyzer);
			// for(int j = 0; j < foundDocs.size(); j++)
			// {
			// docs.add(foundDocs.get(j).get("path"));
			// doc_templates.add(templates.get(i));
			// doc_weights.add(weights.get(i));
			// doc_look_locs.add(look_locs.get(i));
			// }
			// }
			//
			// //Lists storing the ngrams and their corresponding weights
			// ArrayList<String> ngrams = new ArrayList<String>();
			// ArrayList<Double> ngram_weights = new ArrayList<Double>();
			// //List of documents paths associated to each ngram (so we don't
			// add weights for n-grams found in the same document)
			// ArrayList<String> ngram_docs = new ArrayList<String>();
			//
			// //Get n-grams
			// NGramMinder.mine(docs, doc_templates, doc_weights, ngrams,
			// ngram_weights, ngram_docs, doc_look_locs);
			//
			// for(int i = 0; i < ngram_weights.size(); i++)
			// System.out.println(ngrams.get(i) + ": " + ngram_weights.get(i));
		}
		// #############################################################################
		// #############################################################################
		// #############################################################################
	}

	public static TreeMap<String, Integer> sortHashMap(
			HashMap<String, Integer> hashMap) {
		HashMap<String, Integer> map = hashMap;
		ValueComparator bvc = new ValueComparator(map);
		TreeMap<String, Integer> sorted_map = new TreeMap<String, Integer>(bvc);
		
		System.out.println("\n******************************************\n");
		System.out.println("RESULTS");
		System.out.println("\n******************************************\n");
		System.out.println("\nUnsorted N-grams: " + map);

		sorted_map.putAll(map);

		System.out.println("\nSorted N-grams: " + sorted_map);

		return sorted_map;
	}

	public static HashMap<String, Double> tileNGrams(
			TreeMap<String, Integer> treeMap) {
		// Create a list of ProcessedAnswers to store all ngrams
		ArrayList<ProcessedAnswer> ngrams = new ArrayList<ProcessedAnswer>();

		// Store all keys and scores (NOT WEIGHTS) in ngrams
		for (Map.Entry<String, Integer> entry : treeMap.entrySet()) {
			ProcessedAnswer add_processedAnswer = new ProcessedAnswer(entry.getKey());
			add_processedAnswer.score = entry.getValue();
			ngrams.add(add_processedAnswer);
		}

		// Loop through every key
		for (int i = 0; i < ngrams.size(); i++) {
			// Loop through every other key
			for (int j = i + 1; j < ngrams.size(); j++) {
				// Check if either string is a subset of the other
				if (checkSubstring(ngrams, i, j)) {
					j = i;
					continue;
				}
				if (checkSubstring(ngrams, j, i)) {
					j = i;
					continue;
				}

				// Check if there is overlap within the strings
				if (checkOverlap(ngrams, i, j)) {
					j = i;
					continue;
				}
				if (checkOverlap(ngrams, j, i)) {
					j = i;
					continue;
				}
			}
		}

		// Create a new hashmap
		HashMap<String, Double> tiled_map = new HashMap<String, Double>();

		// Add each processed answer to the treemap
		for (int i = 0; i < ngrams.size(); i++)
			tiled_map.put(ngrams.get(i).content, ngrams.get(i).score);

		// Return the new hashmap
		return tiled_map;
	}

	// Check if str_index2 (index of string2 in ngrams) is a substring of
	// str_index1 (index of string1 in ngrams)
	public static boolean checkSubstring(ArrayList<ProcessedAnswer> ngrams,
			int str_index1, int str_index2) {
		// if(ngrams.get(str_index2).content.equals("located in paris,"))//ngrams.get(str_index2).content.equals("paris,")
		// &&
		// System.out.println("YOUNG MONEY");

		Pattern p = Pattern.compile("\\b" + ngrams.get(str_index2).content + "\\b");
		Matcher m = p.matcher(ngrams.get(str_index1).content);
		if (m.find() == false)
			return false;

		// If string1 comes before string2
		if (str_index1 < str_index2) {
			// Add string2's score to string1's score
			ngrams.get(str_index1).score += ngrams.get(str_index2).score;
			// Delete string2
			ngrams.remove(str_index2);
		} else {
			ngrams.get(str_index2).content = ngrams.get(str_index1).content;
			// Add string2's score to string1's score
			ngrams.get(str_index2).score += ngrams.get(str_index1).score;
			// Delete string1
			ngrams.remove(str_index1);
		}

		return true;
	}

	// Check if str_index2 (index of string2 in ngrams) can be tiled onto the
	// end of str_index1 (index of string1 in ngrams)
	public static boolean checkOverlap(ArrayList<ProcessedAnswer> ngrams,
			int str_index1, int str_index2) {
		// Break into two lists of words
		ArrayList<String> str1_words = splitIntoWords(ngrams.get(str_index1).content);
		ArrayList<String> str2_words = splitIntoWords(ngrams.get(str_index2).content);

		// Tracks the position of the words in the first string when iterating
		// through the second string
		int str_pos1 = 0;

		// String to add to the end of string 1
		String add_string = "";

		// Loop through first list of words (string 1)
		for (int i = 0; i < str1_words.size(); i++) {
			// If the word equals the first word of second string
			if (str1_words.get(i).equals(str2_words.get(0))) {
				// Start at the next position of string 1
				str_pos1 = i + 1;

				// Loop through all words in the second string
				for (int j = 1; j < str2_words.size(); j++) {

					if (TemplateQuery.checkStopWord(str2_words.get(0)) == true)
						break;
					// If the end of the first string has been reached
					if (str_pos1 == str1_words.size()) {
						// Add the current word in string2, plus any words after
						// to string1 in ngrams
						for (int k = j; k < str2_words.size(); k++) {
							add_string += str2_words.get(k);

							if (k != str2_words.size() - 1)
								add_string += " ";
						}

						// If the first string is before the second string in
						// the array list
						if (str_index1 < str_index2) {
							ngrams.get(str_index1).content += add_string;
							// Set the score of string1 to the sum its score and
							// the score of string2
							ngrams.get(str_index1).score += ngrams
									.get(str_index2).score;
							// Delete string1 in ngrams
							ngrams.remove(str_index2);
						} else {
							// Create a word equal to string1 plus the current
							// word in string2 and any words after
							add_string = ngrams.get(str_index1).content
									+ add_string;
							// Set string2 to the new string
							ngrams.get(str_index2).content = add_string;
							// Set the score of string2 to the sum its score and
							// the score of string1
							ngrams.get(str_index2).score += ngrams
									.get(str_index1).score;
							// Delete string1 in ngrams
							ngrams.remove(str_index1);
						}
						return true;
					}

					// If the words do not match, break
					if (!str1_words.get(str_pos1).equals(str2_words.get(j)))
						break;

					str_pos1++;
				}
			}
		}
		return false;
	}

	// Splits a string into a list of words
	public static ArrayList<String> splitIntoWords(String str) {
		ArrayList<String> words = new ArrayList<String>();
		int start_pos = 0;
		int curr_pos = 0;
		String add_word;

		// Loop through every character
		for (int i = 0; i <= str.length(); i++) {
			// If a space or end of string is found
			if (i == str.length() || str.charAt(i) == ' ') {
				// Add the substring to the list of words
				add_word = str.substring(start_pos, curr_pos).replace(" ", "");

				if (!add_word.equals(""))
					words.add(add_word);

				// Set the start position to the current position (a space) plus
				// one
				start_pos = curr_pos + 1;
			}
			// Increment the current position
			curr_pos++;
		}
		return words;
	}
}

class ValueComparator implements Comparator<String> {

	Map<String, Integer> base;

	public ValueComparator(Map<String, Integer> base) {
		this.base = base;
	}

	// Note: this comparator imposes orderings that are inconsistent with
	// equals.
	public int compare(String a, String b) {
		if (base.get(a).compareTo(base.get(b)) >= 0) {
			return -1;
		} else {
			return 1;
		} // returning 0 would merge keys
	}
}