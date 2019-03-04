package eu.europa.ec.itb.shacl;

public enum ValidatorChannel {

    FORM("form"),
    WEB_SERVICE("webservice"),
    EMAIL("email");

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
        } else if (WEB_SERVICE.getName().equals(name)) {
            return WEB_SERVICE;
        } else if (EMAIL.getName().equals(name)) {
            return EMAIL;
        } else {
            throw new IllegalArgumentException("Uknown validator channel ["+name+"]");
        }
    }
}
