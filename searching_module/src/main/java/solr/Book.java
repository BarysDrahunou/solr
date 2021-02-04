package solr;

import org.apache.solr.client.solrj.beans.Field;

public class Book {

    @Field
    private String id;
    @Field
    private String genre;
    @Field
    private String authorName;
    @Field
    private String authorSurname;
    @Field
    private String bookName;
    @Field
    private String annotation;
    @Field
    private int date;
    @Field
    private String language;
    @Field
    private Double version;
    @Field
    private String bookFileName;
    @Field
    private String text;
    @Field
    private String rawText;

    public String getId() {
        return id;
    }

    public String getGenre() {
        return genre;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorSurname() {
        return authorSurname;
    }

    public String getBookName() {
        return bookName;
    }

    public String getAnnotation() {
        return annotation;
    }

    public int getDate() {
        return date;
    }

    public String getLanguage() {
        return language;
    }

    public Double getVersion() {
        return version;
    }

    public String getBookFileName() {
        return bookFileName;
    }

    public String getText() {
        return text;
    }

    public String getRawText() {
        return rawText;
    }
}