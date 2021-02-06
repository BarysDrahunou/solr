package solr;

import org.apache.solr.client.solrj.beans.Field;

import java.util.Objects;

public class Book implements Comparable<Book> {

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
    private String date;
    @Field
    private String language;
    @Field
    private String version;
    @Field
    private String bookFileName;
    @Field
    private String text;
    @Field
    private String rawText;

    private int priority;

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

    public String getDate() {
        return date;
    }

    public String getLanguage() {
        return language;
    }

    public String getVersion() {
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

    public int getPriority() {
        return priority;
    }

    public void addPriority(int priority) {
        this.priority += priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Book)) return false;
        Book book = (Book) o;
        return id.equals(book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(Book o) {
        return o.priority-this.priority;
    }

    @Override
    public String toString() {
        return
                "bookName= " + bookName;
    }
}