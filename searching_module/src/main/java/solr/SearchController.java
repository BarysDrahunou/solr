package solr;

import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private final int fullMatchedBooksPriority = 10;
    private final int limitForWordsInQuery = 3;
    private final String splitPattern = "[:-_,/\\\\\\s]+";


    private Map<String, Book> bookMap = new HashMap<>();
    private Map<Integer, List<Book>> bookBucketMap = new HashMap<>();

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public String result(@RequestParam(value = "query", required = false) String query,
                         @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                         Model model) {
        if (query != null) {
            String urlString = "http://localhost:8983/solr/solr-homework";
            SolrClient solr = new HttpSolrClient.Builder(urlString).build();

            bookMap = null;
            bookBucketMap = null;

            List<Book> bookList = new ArrayList<>();

            List<Book> idBooks = getIdMatchedBooks(solr, query);
            List<Book> genreBooks = getGenreMatchedBooks(solr, query);
            List<Book> authorNameBooks = getAuthorNameMatchedBooks(solr, query);
            List<Book> authorSurnameBooks = getAuthorSurnameMatchedBooks(solr, query);
            List<Book> bookNameBooks = getBookNameMatchedBooks(solr, query);
            List<Book> annotationBooks = getAnnotationNameMatchedBooks(solr, query);
            List<Book> dateBooks = getDateMatchedBooks(solr, query);
            List<Book> languageBooks = getLanguageMatchedBooks(solr, query);
            List<Book> versionBooks = getVersionMatchedBooks(solr, query);

            mergeBooks(bookList, idBooks);
            mergeBooks(bookList, genreBooks);
            mergeBooks(bookList, authorNameBooks);
            mergeBooks(bookList, authorSurnameBooks);
            mergeBooks(bookList, bookNameBooks);
            mergeBooks(bookList, annotationBooks);
            mergeBooks(bookList, dateBooks);
            mergeBooks(bookList, languageBooks);
            mergeBooks(bookList, versionBooks);

            Collections.sort(bookList);

            createBooksMaps(bookList);
        }

        page = page < 1 ? 1 : page > bookBucketMap.size() ? bookBucketMap.size() : page;

        model.addAttribute("page", page);
        model.addAttribute("books", bookMap);
        model.addAttribute("booksBucket", bookBucketMap);

        return "search";
    }

    private List<Book> getIdMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "id", query);

        List<Book> partiallyMatchedBooks = getPartiallyMatchedBooks(solr, "id", query, "%s", 4, 3, limitForWordsInQuery);

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getGenreMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "genre", query);

        List<Book> partiallyMatchedBooks = getPartiallyMatchedBooks(solr, "genre", query, "%s", 3, 3, limitForWordsInQuery);

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getAuthorNameMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "authorName", query);

        List<Book> partiallyMatchedBooks = getPartiallyMatchedBooks(solr, "authorName", query, "%s", 3, 5, limitForWordsInQuery);

        List<Book> partiallyMatchedBooks1 = getPartiallyMatchedBooks(solr, "authorName", query, "%s*", 3, 3, limitForWordsInQuery);

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);
        mergeBooks(fullMatchedBooks, partiallyMatchedBooks1);

        return fullMatchedBooks;
    }

    private List<Book> getAuthorSurnameMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "authorSurname", query);

        List<Book> partiallyMatchedBooks = getPartiallyMatchedBooks(solr, "authorSurname", query, "%s", 3, 5, limitForWordsInQuery);
        List<Book> partiallyMatchedBooks1 = getPartiallyMatchedBooks(solr, "authorSurname", query, "%s*", 3, 3, limitForWordsInQuery);

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);
        mergeBooks(fullMatchedBooks, partiallyMatchedBooks1);

        return fullMatchedBooks;
    }

    private List<Book> getBookNameMatchedBooks(SolrClient solr, String query) {

        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "bookName", query);

        List<Book> partiallyMatchedBooks = getPartiallyMatchedBooks(solr, "bookName", query, "*%s*", 3, 3, limitForWordsInQuery);

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getAnnotationNameMatchedBooks(SolrClient solr, String query) {

        return getPartiallyMatchedBooks(solr, "annotation", query, "*%s*", 3, 1, 10);
    }

    private List<Book> getDateMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "date", query);

        SolrQuery solrQuery = getSolrQuery();

        List<String> queries = Arrays.stream(query.split("[-_,/\\\\\\sw]+"))
                .collect(Collectors.toList());

        List<Book> partiallyMatchedBooks = queries.stream()
                .limit(limitForWordsInQuery)
                .map(query1 -> {
                    try {
                        switch (query1.length()) {
                            case 4:
                                solrQuery.setQuery(String.format("date:%s", query1));
                                break;
                            case 3:
                                solrQuery.setQuery(String.format("date:%s*", query1));
                                break;
                            case 2:
                                solrQuery.setQuery(String.format("date:*%s", query1));
                                break;
                            default:
                                return new ArrayList<Book>();
                        }

                        QueryResponse response = solr.query(solrQuery);
                        List<Book> bookList = response.getBeans(Book.class);

                        return bookList != null ? bookList : new ArrayList<Book>();

                    } catch (SolrServerException | IOException e) {
                        return new ArrayList<Book>();
                    }
                }).flatMap(List::stream).collect(Collectors.toList());

        partiallyMatchedBooks.forEach(book -> book.addPriority(2));

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getLanguageMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "language", query);

        List<Book> partiallyMatchedBooks = getPartiallyMatchedBooks(solr, "language", query, "%s", 2, 2, 3);

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getVersionMatchedBooks(SolrClient solr, String query) {
        List<Book> fullMatchedBooks = getFullMatchedBooks(solr, "version", query);

        SolrQuery solrQuery = getSolrQuery();

        List<String> queries = Arrays.stream(query.split(splitPattern))
                .collect(Collectors.toList());

        List<Book> partiallyMatchedBooks = queries.stream()
                .limit(limitForWordsInQuery)
                .map(query1 -> {
                    try {
                        if (Pattern.compile("^([0-9.])+$").matcher(query1).find()) {

                            while (query1.endsWith("0") && !query1.endsWith(".0")) {
                                query1 = query1.substring(0, query1.length() - 1);
                            }

                            solrQuery.setQuery(String.format("version:%s", query1));

                            QueryResponse response = solr.query(solrQuery);

                            return response.getBeans(Book.class);
                        }
                        return new ArrayList<Book>();
                    } catch (SolrServerException | IOException e) {
                        return new ArrayList<Book>();
                    }
                }).flatMap(List::stream).collect(Collectors.toList());

        partiallyMatchedBooks.forEach(book -> book.addPriority(2));

        mergeBooks(fullMatchedBooks, partiallyMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getFullMatchedBooks(SolrClient solr, String field, String query) {
        try {
            SolrQuery solrQuery = getSolrQuery();
            solrQuery.setQuery(String.format("%s:\"%s\"", field, query));
            QueryResponse response = solr.query(solrQuery);
            List<Book> bookList = response.getBeans(Book.class);
            bookList.forEach(book -> book.addPriority(fullMatchedBooksPriority));
            return bookList;
        } catch (IOException | SolrServerException e) {
            return new ArrayList<>();
        }
    }

    private List<Book> getPartiallyMatchedBooks(SolrClient solr, String field, String query, String subQueryPattern, int minSymbolCount, int priority, int limit) {
        SolrQuery solrQuery = getSolrQuery();

        List<String> queries = Arrays.stream(query.toLowerCase().split(splitPattern))
                .filter(subQuery -> subQuery.length() >= minSymbolCount)
                .limit(limit)
                .collect(Collectors.toList());

        List<Book> partiallyMatchedBooks = queries.stream().map(subQuery -> {
            try {
                solrQuery.setQuery(String.format("%s:%s", field, String.format(subQueryPattern, subQuery)));

                QueryResponse response = solr.query(solrQuery);
                List<Book> bookList = response.getBeans(Book.class);

                return bookList != null ? bookList : new ArrayList<Book>();

            } catch (SolrServerException | IOException e) {
                return new ArrayList<Book>();
            }
        }).flatMap(List::stream).collect(Collectors.toList());

        partiallyMatchedBooks.forEach(book -> book.addPriority(priority));

        return partiallyMatchedBooks;
    }


    private void mergeBooks(List<Book> to, List<Book> from) {
        from.forEach(book -> {
            if (to.contains(book)) {
                to.get(to.indexOf(book))
                        .addPriority(book.getPriority());
            } else {
                to.add(book);
            }
        });
    }

    private SolrQuery getSolrQuery() {
        SolrQuery solrQuery = new SolrQuery();
        int rows = 100;
        solrQuery.setRows(rows);
        return solrQuery;
    }

    private void createBooksMaps(List<Book> bookList) {
        bookBucketMap = new HashMap<>();

        int booksOnPage = 8;
        for (int i = 0; i < Math.ceil(((double) bookList.size()) / booksOnPage); i++) {
            bookBucketMap.put(i + 1, new ArrayList<>(bookList.subList(i * booksOnPage, Math.min(i * booksOnPage + booksOnPage, bookList.size()))));
        }

        bookMap = bookList.stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
    }

    @RequestMapping(value = "/book", method = RequestMethod.POST)
    public String book(@RequestParam("id") String id, @RequestParam("page") int page, Model model) {

        if (!bookBucketMap.isEmpty()) {

            model.addAttribute("book", bookMap.get(id));
            model.addAttribute("page", page);

            return "book";
        } else {
            return "search";
        }
    }
}
