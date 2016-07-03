package org.lappsgrid.mallet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;

import java.io.IOException;

public class TestTrainSequenceTagging {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new TrainSequenceTagging();
    }

    // then destroy it after the test
    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testMetadata() {
    }

    @Test
    public void testExecute() {
        // wrap input in a Data object
        Data input = new Data<>(Discriminators.Uri.TEXT, "");

        // add parameters
        input.setParameter("directory", "masc_500k_texts_word_by_word");
        input.setParameter("path", "models");
        input.setParameter("modelName", "masc_500k_texts_word_by_word234.model");
        
        // execute service and print out result
        String string = this.service.execute(input.asJson());
        System.out.println(string);
    }
}