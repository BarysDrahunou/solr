import org.apache.solr.client.solrj.beans.Field;

public class Book {

    @Field
    public String id;
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
    private String version;
    @Field
    private String text;
    @Field
    private String rawText;
    @Field
    private String bookFileName;

    public static class Builder {

        private final Book book;

        public Builder() {
            book = new Book();
        }

        public Builder withId(String id) {
            book.id = id;
            return this;
        }

        public Builder withGenre(String genre) {
            book.genre = genre;
            return this;
        }

        public Builder withAuthorName(String authorName) {
            book.authorName = authorName;
            return this;
        }

        public Builder withAuthorSurname(String authorSurname) {
            book.authorSurname = authorSurname;
            return this;
        }

        public Builder withBookName(String bookName) {
            book.bookName = bookName;
            return this;
        }

        public Builder withAnnotation(String annotation) {
            book.annotation = annotation;
            return this;
        }

        public Builder withDate(int date) {
            book.date = date;
            return this;
        }

        public Builder withLanguage(String language) {
            book.language = language;
            return this;
        }

        public Builder withVersion(String version) {
            book.version = version;
            return this;
        }

        public Builder withText(String text) {
            book.text = text;
            return this;
        }

        public Builder withRawText(String rawText) {
            book.rawText = rawText;
            return this;
        }

        public Builder withBookFileName(String bookFileName) {
            book.bookFileName = bookFileName;
            return this;
        }

        public Book build() {
            return book;
        }
    }
}