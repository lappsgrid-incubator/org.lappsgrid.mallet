package org.lappsgrid.mallet;

// JUnit modules for unit tests
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;

import java.io.IOException;

// more APIs for testing code


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
    public void testMetadata() {  }

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