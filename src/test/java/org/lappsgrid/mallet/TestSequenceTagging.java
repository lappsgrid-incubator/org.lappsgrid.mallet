package org.lappsgrid.mallet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TestSequenceTagging {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new SequenceTagging();
    }

    // then destroy it after the test
    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testMetadata() {
        WebService service = new SequenceTagging();

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
        assertEquals("Name is not correct", SequenceTagging.class.getName(), metadata.getName());
        assertEquals("\"allow\" field not equal", Discriminators.Uri.ANY, metadata.getAllow());
        assertEquals("License not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        List<String> list = requires.getFormat();
        assertTrue("Tokens not accepted", list.contains(Discriminators.Uri.TOKEN));
        assertTrue("Required languages do not contain English", requires.getLanguage().contains("en"));
    }

    @Test
    public void testExecute() {
        // Entering the tokens for "Don't count the days. Make the days count." into a Data object
        Container container = new Container();
        container.setText("Don't count the days. Make the days count.");
        View view = container.newView();
        Annotation a;

        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 0, 2);
        a.addFeature(Features.Token.WORD, "Don");
        a = view.newAnnotation("tok1", Discriminators.Uri.TOKEN, 3, 4);
        a.addFeature(Features.Token.WORD, "'t");
        a = view.newAnnotation("tok2", Discriminators.Uri.TOKEN, 6, 10);
        a.addFeature(Features.Token.WORD, "count");
        a = view.newAnnotation("tok3", Discriminators.Uri.TOKEN, 12, 14);
        a.addFeature(Features.Token.WORD, "the");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 16, 19);
        a.addFeature(Features.Token.WORD, "days");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 20, 20);
        a.addFeature(Features.Token.WORD, ".");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 22, 25);
        a.addFeature(Features.Token.WORD, "Make");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 27, 29);
        a.addFeature(Features.Token.WORD, "the");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 31, 34);
        a.addFeature(Features.Token.WORD, "days");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 36, 40);
        a.addFeature(Features.Token.WORD, "count");
        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 41, 41);
        a.addFeature(Features.Token.WORD, ".");

        Data data = new DataContainer(container);
        data.setDiscriminator(Discriminators.Uri.TOKEN);

        // call `execute()` with jsonized input,
        String string = this.service.execute(data.asJson());

        System.out.println(string);
    }

    @Test
    public void testExecuteWithParameters() {
        // Entering the tokens for "Don't count the days. Make the days count." into a Data object
        Container container = new Container();
        container.setText("Don't count the days. Make the days count.");
        View view = container.newView();
        Annotation a;

        a = view.newAnnotation("tok0", Discriminators.Uri.TOKEN, 0, 2);
        a.addFeature(Features.Token.WORD, "Don");
        a = view.newAnnotation("tok1", Discriminators.Uri.TOKEN, 3, 4);
        a.addFeature(Features.Token.WORD, "'t");
        a = view.newAnnotation("tok2", Discriminators.Uri.TOKEN, 6, 10);
        a.addFeature(Features.Token.WORD, "count");
        a = view.newAnnotation("tok3", Discriminators.Uri.TOKEN, 12, 14);
        a.addFeature(Features.Token.WORD, "the");
        a = view.newAnnotation("tok4", Discriminators.Uri.TOKEN, 16, 19);
        a.addFeature(Features.Token.WORD, "days");
        a = view.newAnnotation("tok5", Discriminators.Uri.TOKEN, 20, 20);
        a.addFeature(Features.Token.WORD, ".");
        a = view.newAnnotation("tok6", Discriminators.Uri.TOKEN, 22, 25);
        a.addFeature(Features.Token.WORD, "Make");
        a = view.newAnnotation("tok7", Discriminators.Uri.TOKEN, 27, 29);
        a.addFeature(Features.Token.WORD, "the");
        a = view.newAnnotation("tok8", Discriminators.Uri.TOKEN, 31, 34);
        a.addFeature(Features.Token.WORD, "days");
        a = view.newAnnotation("tok9", Discriminators.Uri.TOKEN, 36, 40);
        a.addFeature(Features.Token.WORD, "count");
        a = view.newAnnotation("tok10", Discriminators.Uri.TOKEN, 41, 41);
        a.addFeature(Features.Token.WORD, ".");

        Data data = new DataContainer(container);
        data.setDiscriminator(Discriminators.Uri.TOKEN);

        // add parameters
        data.setParameter("model", this.getClass().getResource("/masc_500k_texts_word_by_word.model"));

        // call `execute()` with jsonized input,
        String string = this.service.execute(data.asJson());

        System.out.println(string);
    }
}