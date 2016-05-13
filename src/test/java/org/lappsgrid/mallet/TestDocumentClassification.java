package org.lappsgrid.mallet;

// JUnit modules for unit tests

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

// more APIs for testing code
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.io.IOException;
import java.util.List;


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
    }

    @Test
    public void testExecute() {
        final String text = "testString Why did the chicken cross the road? To get to the other side!";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());
    }
}
