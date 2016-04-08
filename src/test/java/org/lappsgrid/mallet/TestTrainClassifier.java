package org.lappsgrid.mallet;

// JUnit modules for unit tests
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

// more APIs for testing code
import org.lappsgrid.api.WebService;

import java.io.IOException;


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
        String string = this.service.execute("masc_500k_texts");
    }
}