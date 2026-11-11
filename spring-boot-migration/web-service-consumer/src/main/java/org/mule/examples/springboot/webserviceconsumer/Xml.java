package org.mule.examples.springboot.webserviceconsumer;

final class Xml {

    private Xml() {
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
