package org.lappsgrid.mallet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;

import java.io.IOException;


public class TestTrainTopicModeling {

    // this will be the sandbag
    protected WebService service;

    // initiate the service before each test
    @Before
    public void setUp() throws IOException {
        service = new TrainTopicModeling();
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
        input.setParameter("directory", "src/test/resources/masc_500k_texts");
        input.setParameter("path", "models");
        input.setParameter("inferencerName", "masc_500k_texts_topics.inferencer");
        input.setParameter("keysName", "masc_500k_texts_topic_keys.txt");
        input.setParameter("numTopics", 10);
        input.setParameter("wordsPerTopic", 10);

        // execute the tool and prints out the result
        String string = this.service.execute(input.asJson());
        System.out.println(string);
    }
}
