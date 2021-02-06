package solr;

import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
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

    private Map<String, Book> bookMap = new HashMap<>();
    private Map<Integer, List<Book>> bookBucketMap = new HashMap<>();
    private List<FacetField> facetFields;
    String urlString = "http://localhost:8983/solr/solr-homework";
    SolrClient solr = new HttpSolrClient.Builder(urlString).build();
    String query;

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public String result(@RequestParam(value = "query", required = false) String query,
                         @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                         Model model) {
        if (query != null) {
            this.query = query;
            bookMap = null;
            bookBucketMap = null;
            facetFields = new ArrayList<>();

            getRequestedBooks("*:*");
        }

        page = page < 1 ? 1 : page > bookBucketMap.size() ? bookBucketMap.size() : page;

        model.addAttribute("page", page);
        model.addAttribute("books", bookMap);
        model.addAttribute("booksBucket", bookBucketMap);
        model.addAttribute("facets", facetFields);

        return "search";
    }

    private void getRequestedBooks(String filterQuery) {

        List<Book> bookList = new ArrayList<>();

        List<Book> fullMatched = getFullMatchedFields(solr, query, filterQuery);

        List<Book> partiallyMatched = getPartiallyMatchedFields(solr, query, filterQuery);

        mergeBooks(bookList, fullMatched);
        mergeBooks(bookList, partiallyMatched);

        Collections.sort(bookList);

        createBooksMaps(bookList);
    }


    private List<Book> getFullMatchedFields(SolrClient solr, String query, String filterQuery) {
        List<Book> fullMatchedBooks = new ArrayList<>();

        List<Book> idFullMatchedBooks = getFullMatchedBooks(solr, "id", query, filterQuery);
        List<Book> genreFullMatchedBooks = getFullMatchedBooks(solr, "genre", query, filterQuery);
        List<Book> authorNameFullMatchedBooks = getFullMatchedBooks(solr, "authorName", query, filterQuery);
        List<Book> authorSurnameFullMatchedBooks = getFullMatchedBooks(solr, "authorSurname", query, filterQuery);
        List<Book> bookNameFullMatchedBooks = getFullMatchedBooks(solr, "bookName", query, filterQuery);
        List<Book> dateFullMatchedBooks = getFullMatchedBooks(solr, "date", query, filterQuery);
        List<Book> languageFullMatchedBooks = getFullMatchedBooks(solr, "language", query, filterQuery);
        List<Book> versionFullMatchedBooks = getFullMatchedBooks(solr, "version", query, filterQuery);

        mergeBooks(fullMatchedBooks, idFullMatchedBooks);
        mergeBooks(fullMatchedBooks, genreFullMatchedBooks);
        mergeBooks(fullMatchedBooks, authorNameFullMatchedBooks);
        mergeBooks(fullMatchedBooks, authorSurnameFullMatchedBooks);
        mergeBooks(fullMatchedBooks, bookNameFullMatchedBooks);
        mergeBooks(fullMatchedBooks, dateFullMatchedBooks);
        mergeBooks(fullMatchedBooks, languageFullMatchedBooks);
        mergeBooks(fullMatchedBooks, versionFullMatchedBooks);

        return fullMatchedBooks;
    }

    private List<Book> getPartiallyMatchedFields(SolrClient solr, String query, String filterQuery) {
        String splitPattern = "[:-_,/\\\\\\s]+";
        List<String> queries = Arrays.stream(query.toLowerCase().split(splitPattern))
                .filter(subQuery -> subQuery.length() >= 2)
                .limit(10)
                .collect(Collectors.toList());

        return queries.stream().map(subQuery -> {
            List<Book> idPartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "id", subQuery, "%s", filterQuery, 3, true);
            List<Book> genrePartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "genre", subQuery, "%s", filterQuery, 3, true);
            List<Book> languagePartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "language", subQuery, "%s", filterQuery, 2, true);
            List<Book> authorNamePartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "authorName", subQuery, "%s", filterQuery, 5, true);
            List<Book> authorName1PartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "authorName", subQuery, "%s*", filterQuery, 3, false);
            List<Book> authorSurnamePartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "authorSurname", subQuery, "%s", filterQuery, 5, true);
            List<Book> authorSurname1PartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "authorSurname", subQuery, "%s*", filterQuery, 3, false);
            List<Book> bookNamePartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "bookName", subQuery, "*%s*", filterQuery, 3, true);
            List<Book> annotationPartiallyMatchedBooks = getPartiallyMatchedBooks(solr, "annotation", subQuery, "*%s*", filterQuery, 1, false);
            List<Book> datePartiallyMatchedBooks = getDateMatchedBooks(solr, subQuery, filterQuery);
            List<Book> versionPartiallyMatchedBooks = getVersionMatchedBooks(solr, subQuery, filterQuery);

            List<Book> partiallyMatchedBooks = new ArrayList<>();

            mergeBooks(partiallyMatchedBooks, idPartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, genrePartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, languagePartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, authorNamePartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, authorName1PartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, authorSurnamePartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, authorSurname1PartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, bookNamePartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, annotationPartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, datePartiallyMatchedBooks);
            mergeBooks(partiallyMatchedBooks, versionPartiallyMatchedBooks);

            return partiallyMatchedBooks;
        }).flatMap(List::stream).collect(Collectors.toList());
    }

    private List<Book> getFullMatchedBooks(SolrClient solr, String field, String query, String filterQuery) {
        try {
            SolrQuery solrQuery = getSolrQuery();

            solrQuery.setQuery(String.format("%s:\"%s\"", field, query));
            solrQuery.setFilterQueries(filterQuery);

            QueryResponse response = solr.query(solrQuery);
            List<Book> bookList = response.getBeans(Book.class);

            bookList.forEach(book -> book.addPriority(fullMatchedBooksPriority));

            return bookList;
        } catch (IOException | SolrServerException e) {
            return new ArrayList<>();
        }
    }

    private List<Book> getPartiallyMatchedBooks(SolrClient solr, String field, String subQuery, String queryPattern, String filterQuery, int priority, boolean allowFacets) {
        try {
            SolrQuery solrQuery = new SolrQuery();

            solrQuery.setQuery(String.format("%s:%s", field, String.format(queryPattern, subQuery)));
            solrQuery.setFilterQueries(filterQuery);

            if (allowFacets) {
                solrQuery.setFacetMinCount(1);
                solrQuery.setFacetLimit(10);
                solrQuery.addFacetField("id_copy", "genre_copy", "authorName_copy", "authorSurname_copy",
                        "bookName_copy", "date_copy", "language_copy", "version_copy");
            }

            QueryResponse response = solr.query(solrQuery);
            List<Book> bookList = response.getBeans(Book.class);

            if (allowFacets) {
                List<FacetField> fields = response.getFacetFields();

                if (!fields.get(0).getValues().isEmpty()) {
                    mergeFacets(fields);
                }
            }

            if (bookList != null) {
                bookList.forEach(book -> book.addPriority(priority));

                return bookList;
            } else {

                return new ArrayList<>();
            }
        } catch (SolrServerException | IOException e) {
            return new ArrayList<>();
        }
    }

    private List<Book> getDateMatchedBooks(SolrClient solr, String query, String filterQuery) {

        SolrQuery solrQuery = getSolrQuery();

        try {
            switch (query.length()) {
                case 4:
                    solrQuery.setQuery(String.format("date:%s", query));
                    break;
                case 3:
                    solrQuery.setQuery(String.format("date:%s*", query));
                    break;
                case 2:
                    solrQuery.setQuery(String.format("date:*%s", query));
                    break;
                default:
                    return new ArrayList<>();
            }

            solrQuery.setFilterQueries(filterQuery);
            solrQuery.setFacetMinCount(1);
            solrQuery.setFacetLimit(10);
            solrQuery.addFacetField("id_copy", "genre_copy", "authorName_copy", "authorSurname_copy",
                    "bookName_copy", "date_copy", "language_copy", "version_copy");

            QueryResponse response = solr.query(solrQuery);
            List<Book> bookList = response.getBeans(Book.class);
            List<FacetField> fields = response.getFacetFields();

            if (!fields.get(0).getValues().isEmpty()) {
                mergeFacets(fields);
            }

            if (bookList != null) {
                bookList.forEach(book -> book.addPriority(2));
                return bookList;
            }

            return new ArrayList<>();

        } catch (SolrServerException | IOException e) {
            return new ArrayList<>();
        }
    }


    private List<Book> getVersionMatchedBooks(SolrClient solr, String query, String filterQuery) {
        SolrQuery solrQuery = getSolrQuery();

        try {
            if (Pattern.compile("^([0-9.])+$").matcher(query).find()) {

                while (query.endsWith("0") && !query.endsWith(".0")) {
                    query = query.substring(0, query.length() - 1);
                }

                solrQuery.setFilterQueries(filterQuery);
                solrQuery.setFacetMinCount(1);
                solrQuery.setFacetLimit(10);
                solrQuery.addFacetField("id_copy", "genre_copy", "authorName_copy", "authorSurname_copy",
                        "bookName_copy", "date_copy", "language_copy", "version_copy");
                solrQuery.setQuery(String.format("version:%s", query));

                QueryResponse response = solr.query(solrQuery);
                List<Book> bookList = response.getBeans(Book.class);
                List<FacetField> fields = response.getFacetFields();

                if (!fields.get(0).getValues().isEmpty()) {
                    mergeFacets(fields);
                }

                if (bookList != null) {
                    bookList.forEach(book -> book.addPriority(2));
                    return bookList;
                }
                return new ArrayList<>();
            }
            return new ArrayList<>();
        } catch (SolrServerException | IOException e) {
            return new ArrayList<>();
        }
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

    private void mergeFacets(List<FacetField> from) {

        Map<String, List<FacetField.Count>> fromMap = new HashMap<>();
        from.forEach(facetField -> fromMap.put(facetField.getName(), facetField.getValues()));
        Map<String, List<FacetField.Count>> toMap = new HashMap<>();
        facetFields.forEach(facetField -> toMap.put(facetField.getName(), facetField.getValues()));

        Map<String, List<FacetField.Count>> result = new HashMap<>();

        for (String key : fromMap.keySet()) {
            result.put(key, fromMap.get(key));
        }

        for (String key : toMap.keySet()) {
            if (result.containsKey(key)) {
                List<FacetField.Count> counts = new ArrayList<>();
                counts.addAll(result.get(key));
                counts.addAll(toMap.get(key));
                for (int i = 0; i < counts.size(); i++) {
                    for (int j = i + 1; j < counts.size(); j++) {
                        if (counts.get(j).getName().equals(counts.get(i).getName())) {
                            FacetField.Count count;
                            if (counts.get(i).getCount() > counts.get(j).getCount()) {
                                count = counts.get(i);
                            } else {
                                count = counts.get(j);
                            }
                            counts.remove(j);
                            counts.add(j, count);
                            counts.remove(i);
                            j--;
                        }
                    }
                }
                counts = counts.stream().limit(10).collect(Collectors.toList());
                result.put(key, counts);
            } else {
                result.put(key, toMap.get(key));
            }
        }

        facetFields = result.entrySet().stream().map(entry -> {
            FacetField facetField = new FacetField(entry.getKey());
            entry.getValue().forEach(entry1 -> facetField.add(entry1.getName(), entry1.getCount()));
            return facetField;
        }).collect(Collectors.toList());

        facetFields = facetFields
                .stream()
                .sorted(Comparator.comparing(FacetField::getName))
                .collect(Collectors.toList());

        facetFields
                .forEach(facetField -> facetField
                        .getValues()
                        .sort(Comparator.comparingLong(count -> -count.getCount())));
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


    @RequestMapping(value = "/facet", method = RequestMethod.POST)
    public String facet(@RequestParam(name = "query") String filterQuery, Model model) {

        String[] filter = filterQuery.split(":");

        filterQuery = String.format("%s:\"%s\"", filter[0], filter[1]);

        getRequestedBooks(filterQuery);

        model.addAttribute("page", 1);
        model.addAttribute("books", bookMap);
        model.addAttribute("booksBucket", bookBucketMap);
        model.addAttribute("facets", facetFields);

        return "search";
    }
}
