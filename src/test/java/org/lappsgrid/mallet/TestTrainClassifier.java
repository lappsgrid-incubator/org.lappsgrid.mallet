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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;


public class TestTrainClassifier {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new TrainClassifier();
    }

    // then destroy it after the test
    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testMetadata() {
        WebService service = new TrainClassifier();

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
        assertEquals("Name is not correct", TrainClassifier.class.getName(), metadata.getName());
        assertEquals("\"allow\" field not equal", Discriminators.Uri.ANY, metadata.getAllow());
        assertEquals("License not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        List<String> list = requires.getFormat();
        assertTrue("Text not accepted", list.contains(Discriminators.Uri.TEXT));
        assertTrue("Required languages do not contain English", requires.getLanguage().contains("en"));
    }

    @Test
    public void testExecute() {

        // create a Data object to hold parameters
        Data input = new Data<>(Discriminators.Uri.TEXT, null);

        // add parameters
        input.setParameter("directory", this.getClass().getResource("/masc_500k_texts"));
        input.setParameter("path", "models");
        input.setParameter("classifierName", "masc_500k_texts.classifier");

        // execute service and prints out the returned String
        String string = this.service.execute(input.asJson());
        System.out.println(string);
    }

    @Test
    public void testExecute2() {
        // create a Data object to hold parameters
        Data input = new Data<>(Discriminators.Uri.TEXT, null);

        // add parameters
        input.setParameter("directory", this.getClass().getResource("/masc_500k_texts"));
        input.setParameter("path", "models");

        // test all of the trainer types
        String[] trainers =
                {"NaiveBayes", "MaxEnt", "BalancedWinnow", "C45", "DecisionTree",
                        "MaxEntL1", "MCMaxEnt", "NaiveBayesEMT", "Winnow"};
        for (String trainer : trainers) {
            excuteService(input, trainer);
        }
    }

    public void excuteService(Data input, String trainer){
        // set the trainer as a parameter
        input.setParameter("trainer", trainer);
        input.setParameter("classifierName", "masc_500k_texts_" + trainer + ".classifier");

        // execute service and prints out the returned String
        String string = this.service.execute(input.asJson());
        System.out.println(string);
    }
}