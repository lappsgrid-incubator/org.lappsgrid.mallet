package org.lappsgrid.mallet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;

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
        String string = this.service.execute("masc_500k_texts");
    }
}
