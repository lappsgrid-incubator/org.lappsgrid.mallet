package org.lappsgrid.mallet;

// JUnit modules for unit tests

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// more APIs for testing code
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;

import java.io.IOException;


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
        final String text =
                "Why did the chicken cross the road? To get to the other side!\n" +
                "Ordinarily, staring is creepy. But if you spread your attention across many individuals, then it's just people watching.\n" +
                        "We can teach kids there’s no i in team, but it’s way more important  to teach them that there’s no a in  definitely.";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());

        System.out.println(string);
    }
}
