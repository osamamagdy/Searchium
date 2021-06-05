package WebIndexer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import DB.MongoDB;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



/**
 * WebIndexer Stages
 * 1- Tokenizer (Gamal) Done
 * 2- Remove Stopping Words [Optional: To make it Smarter] (Yahia) Done
 * 3- Remove HTML tags (Yahia) Done
 * 4- Stemming [Smart Stemmer] (Gamal) Done
 * 5- Indexing and TF-IDF (Yahia and Gamal)
 * 6- Integrating Module and Testing of simple Corpus (Yahia and Gamal)
 */

public class WebIndexerMain {

    MongoDB DB;
    List<String>stop_words;

    public WebIndexerMain(MongoDB DB,List<String> stop_words) {
        this.DB = DB;
         this.stop_words = stop_words;

    }

    public  void runIndexer(String page, String page_url) throws IOException {
        Iterator<Document> pageItr = DB.findPageInd(page_url).iterator();
        if (pageItr.hasNext() && DB.isIndexed(page_url))
            return;



        page = page.replaceAll("[^a-zA-Z]", " ");
        List<String> words = Tokenizer.tokenizeWord(page);
        StopWordsRemover.removeStopWord(words, this.stop_words);
        int count = words.size();
        if (!pageItr.hasNext())
        {
            DB.insertPageInd(page_url, count);
            System.out.println("new page added");
        }

       for (String word : words)
        {
            if (word.length() != 0)
            {
                Stemmer s = new Stemmer(word);
                String stemed_word = s.toString();
                Iterator<Document> wordItr = DB.getWordInd(stemed_word).iterator();
                if (!wordItr.hasNext())
                {
                    DB.insertWordInd(stemed_word, page_url);
                }
                else
                {
                    boolean found = false;
                    for (String url : DB.getUrlsForWordInd(stemed_word))
                    {
                        if (page_url.equals(url))
                        {
                            found = true;
                            DB.increaseWordCount(stemed_word, page_url);
                            break;
                        }
                    }
                    if (!found)
                        DB.insertNewUrl(stemed_word, page_url);
                }
            }
        }

        for (String word : words) {
            if (word.length() != 0) {
                Stemmer s = new Stemmer(word);
                String stemed_word = s.toString();
                DB.calcTF(stemed_word, page_url, count);
            }
        }
        DB.finishPageIndex(page_url);
    }

    public void updateIDF(int t_all)
    {
        DB.calacIDF(t_all);
    }



}
