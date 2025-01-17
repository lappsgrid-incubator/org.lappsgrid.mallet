package org.lappsgrid.mallet;

// JUnit modules for unit tests
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;

public class TestTopicModeling {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new TopicModeling();
    }

    // then destroy it after the test
    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testMetadata() {
        WebService service = new TopicModeling();

        // Retrieve metadata, remember `getMetadata()` returns a serialized JSON string
        String json = service.getMetadata();
        assertNotNull("service.getMetadata() returned null", json);

        // Instantiate `Data` object with returned JSON string
        Data data = Serializer.parse(json, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Discriminators.Uri.ERROR, data.getDiscriminator());

        // Then, convert it into `Metadata` datastructure
        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());
        IOSpecification produces = metadata.getProduces();
        IOSpecification requires = metadata.getRequires();

        // Now, see each field has correct value
        assertEquals("Name is not correct", TopicModeling.class.getName(), metadata.getName());
        assertEquals("\"allow\" field not equal", Uri.ANY, metadata.getAllow());
        assertEquals("License not correct", Uri.APACHE2, metadata.getLicense());

        List<String> list = requires.getFormat();
        assertTrue("Text not accepted", list.contains(Uri.TEXT));
        assertTrue("Required languages do not contain English", requires.getLanguage().contains("en"));
    }

    @Test
    public void testExecute() {
        // example text for testing
        final String text =
                "Research scientists are the primary audience for the journal, but summaries and accompanying articles are intended to make many of the most important papers understandable to scientists in other fields and the educated public. Towards the front of each issue are editorials, news and feature articles on issues of general interest to scientists, including current affairs, science funding, business, scientific ethics and research breakthroughs. There are also sections on books and arts. The remainder of the journal consists mostly of research papers (articles or letters), which are often dense and highly technical. Because of strict limits on the length of papers, often the printed text is actually a summary of the work in question with many details relegated to accompanying supplementary material on the journal's website.";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());

        System.out.println(string);
    }

    @Test
    public void testExecuteWithParameters() {
        // example text for testing
        final String text =
                "Research scientists are the primary audience for the journal, but summaries and accompanying articles are intended to make many of the most important papers understandable to scientists in other fields and the educated public. Towards the front of each issue are editorials, news and feature articles on issues of general interest to scientists, including current affairs, science funding, business, scientific ethics and research breakthroughs. There are also sections on books and arts. The remainder of the journal consists mostly of research papers (articles or letters), which are often dense and highly technical. Because of strict limits on the length of papers, often the printed text is actually a summary of the work in question with many details relegated to accompanying supplementary material on the journal's website.";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // load up inferencer file
        String inferencerName = "/masc_500k_texts(blog10topics).inferencer";
        URL url = this.getClass().getResource(inferencerName);
        input.setParameter("inferencer", url);

       // load up topic keys file
        String keysName = "/masc_500k_texts(blog10topics)_keys.txt";
        url = this.getClass().getResource(keysName);
        input.setParameter("keys", url);

        // add parameters to the data
        input.setParameter("numIterations", 200);
        input.setParameter("burnIn", 50);
        input.setParameter("thinning", 15);

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());

        System.out.println(string);
    }
}