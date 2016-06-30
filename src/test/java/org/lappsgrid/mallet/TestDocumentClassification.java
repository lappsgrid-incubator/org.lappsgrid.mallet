package org.lappsgrid.mallet;

// JUnit modules for unit tests

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// more APIs for testing code
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;


public class TestDocumentClassification {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new DocumentClassification();
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
        assertEquals("\"allow\" field not equal", Discriminators.Uri.ANY, metadata.getAllow());
        assertEquals("License not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        List<String> list = requires.getFormat();
        assertTrue("Text not accepted", list.contains(Discriminators.Uri.TEXT));
        assertTrue("Required languages do not contain English", requires.getLanguage().contains("en"));
    }

    @Test
    public void testExecute() {
        // test text
        final String text =
                "When you access Basic Completion by pressing Ctrl+Space, you get basic suggestions for variables, types, methods, expressions, and so on. When you call Basic Completion twice, it shows you more results, including private members and non-imported static members.\n" +
                        "The Smart Completion feature is aware of the expected type and data flow, and offers the options relevant to the context. To call Smart Completion, press Ctrl+Shift+Space. When you call Smart Completion twice, it shows you more results, including chains.\n" +
                        "To overwrite the identifier at the caret, instead of just inserting the suggestion, press Tab. This is helpful if you're editing part of an identifier, such as a file name.\n" +
                        "To let IntelliJ IDEA complete a statement for you, press Ctrl+Shift+Enter. Statement Completion will automatically add the missing parentheses, brackets, braces and the necessary formatting.\n" +
                        "If you want to see the suggested parameters for any method or constructor, press Ctrl+P. IntelliJ IDEA shows the parameter info for each overloaded method or constructor, and highlights the best match for the parameters already typed.\n" +
                        "The Postfix Completion feature lets you transform an already typed expression to another one based on the postfix you type after a period, the expression type, and its context.";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());

        System.out.println(string);
    }

    @Test
    public void testExecuteWithParameters() {
        // test text
        final String text =
                "When you access Basic Completion by pressing Ctrl+Space, you get basic suggestions for variables, types, methods, expressions, and so on. When you call Basic Completion twice, it shows you more results, including private members and non-imported static members.\n" +
                        "The Smart Completion feature is aware of the expected type and data flow, and offers the options relevant to the context. To call Smart Completion, press Ctrl+Shift+Space. When you call Smart Completion twice, it shows you more results, including chains.\n" +
                        "To overwrite the identifier at the caret, instead of just inserting the suggestion, press Tab. This is helpful if you're editing part of an identifier, such as a file name.\n" +
                        "To let IntelliJ IDEA complete a statement for you, press Ctrl+Shift+Enter. Statement Completion will automatically add the missing parentheses, brackets, braces and the necessary formatting.\n" +
                        "If you want to see the suggested parameters for any method or constructor, press Ctrl+P. IntelliJ IDEA shows the parameter info for each overloaded method or constructor, and highlights the best match for the parameters already typed.\n" +
                        "The Postfix Completion feature lets you transform an already typed expression to another one based on the postfix you type after a period, the expression type, and its context.";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // add parameters
        input.setParameter("classifier", this.getClass().getResource("/masc_500k_texts(MaxEnt).classifier"));

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());

        System.out.println(string);
    }
}
