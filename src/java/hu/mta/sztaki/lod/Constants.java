package hu.mta.sztaki.lod;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Sac
 */
public class Constants {

    final public static String[] jsonformats = new String[]{"application/json", "application/json+rdf"};
    final public static String[] xmlformats = new String[]{"application/rdf+xml", "text/turtle", "application/turtle", "application/n-triples", "application/ld+json", "application/owl+xml", "text/trig", "application/n-quads"};
    //final public static String[] trivialconnections = new String[]{"http://dbpedia.org/ontology/Person", "http://dbpedia.org/ontology/Person", "http://schema.org/Person", "http://xmlns.com/foaf/0.1/Person", "http://xmlns.com/foaf/0.1/Agent", "http://xmlns.com/foaf/0.1/Group", "http://schema.org/Thing", "http://schema.org/CreativeWork", "http://dbpedia.org/ontology/Work"};
    final public static String[] trivialconnections = new String[]{"http://dbpedia.org/ontology/", "http://schema.org/", "http://xmlns.com/", "http://lexvo.org/", "http://purl.org/dc"};
    final public static String[] trivialconnectiontypes = new String[]{/*"http://purl.org/dc/terms/language", */"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://purl.org/dc/terms/type", "http://purl.org/dc/terms/licence"};
    final public static String[] trivialconnectiontypeprefs = new String[]{};
    final public static List jsonformatsList = Arrays.asList(jsonformats);
    final public static List xmlformatsList = Arrays.asList(xmlformats);
    final public static List trivialconnectiontypesList = Arrays.asList(trivialconnectiontypes);
}
