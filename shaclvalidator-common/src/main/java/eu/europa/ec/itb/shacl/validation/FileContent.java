package eu.europa.ec.itb.shacl.validation;

import com.gitb.core.ValueEmbeddingEnumeration;

public class FileContent {

    public static final String embedding_URL     	= "URL" ;
    public static final String embedding_BASE64		= "BASE64" ;
    public static final String embedding_STRING		= "STRING" ;

    private String content;
    private String embeddingMethod;
    private String syntax;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbeddingMethod() {
        return embeddingMethod;
    }

    public void setEmbeddingMethod(String embeddingMethod) {
        this.embeddingMethod = embeddingMethod;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }
    
    public static boolean isValid(String type) {
    	return embedding_BASE64.equals(type) || embedding_URL.equals(type)  || embedding_STRING.equals(type);
    }

    public static String fromValueEmbeddingEnumeration(ValueEmbeddingEnumeration type) {
        if (type == ValueEmbeddingEnumeration.STRING || type == ValueEmbeddingEnumeration.BASE_64) {
            return type.value();
        } else if (type == ValueEmbeddingEnumeration.URI) {
            return embedding_URL;
        } else {
            throw new IllegalArgumentException("Unknown type ["+type+"]");
        }
    }

}
