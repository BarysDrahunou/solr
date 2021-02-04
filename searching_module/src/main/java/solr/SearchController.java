package solr;

import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private Map<String, Book> bookMap;
    private Map<Integer, List<Book>> bookBucketMap = new HashMap<>();

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public String result(@RequestParam(value = "query", required = false) String query, @RequestParam(value = "page", required = false, defaultValue = "1") int page, Model model) throws Exception {
        if (query != null) {

            QueryResponse response = getResponse(query);

            createBooksMaps(response);
        }

        page = page < 1 ? 1 : page > bookBucketMap.size() ? bookBucketMap.size() : page;

        model.addAttribute("page", page);
        model.addAttribute("books", bookMap);
        model.addAttribute("booksBucket", bookBucketMap);

        return "search";
    }


    private QueryResponse getResponse(String query) throws IOException, SolrServerException {
        String urlString = "http://localhost:8983/solr/solr-homework";
        SolrClient solr = new HttpSolrClient.Builder(urlString).build();
        SolrQuery solrQuery = new SolrQuery();

        solrQuery.setQuery(query);
        solrQuery.setRows(100000);

        return solr.query(solrQuery);
    }

    private void createBooksMaps(QueryResponse response) {
        List<Book> bookList = response.getBeans(Book.class);

        bookBucketMap = new HashMap<>();

        for (int i = 0; i < Math.ceil(((double) bookList.size()) / 8); i++) {
            bookBucketMap.put(i + 1, new ArrayList<>(bookList.subList(i * 8, Math.min(i * 8 + 8, bookList.size()))));
        }

        bookMap = bookList.stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
    }

    @RequestMapping(value = "/book", method = RequestMethod.POST)
    public String book(@RequestParam("id") String id, @RequestParam("page") int page, Model model) {
        model.addAttribute("book", bookMap.get(id));
        model.addAttribute("page", page);

        return "book";
    }
}
