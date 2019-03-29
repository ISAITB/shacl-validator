package eu.europa.ec.itb.shacl;

public enum ValidatorChannel {
	
    FORM("form"),
    EMAIL("email"),
	REST_API("rest_api"),
	SOAP_API("soap_api");

    private String name;

    private ValidatorChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ValidatorChannel byName(String name) {
        if (FORM.getName().equals(name)) {
            return FORM;
        } else if (EMAIL.getName().equals(name)) {
            return EMAIL;
        } else if (REST_API.getName().equals(name)) {
            return REST_API;
        } else if (SOAP_API.getName().equals(name)) {
            return SOAP_API;            
        } else {
            throw new IllegalArgumentException("Unknown validator channel ["+name+"]");
        }
    }
}
