package org.lappsgrid.mallet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.Data;

import java.io.IOException;

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
    }

    @Test
    public void testExecute() {
        // example text for testing
        final String text =
                "Research scientists are the primary audience for the journal, but summaries and accompanying articles are intended to make many of the most important papers understandable to scientists in other fields and the educated public. Towards the front of each issue are editorials, news and feature articles on issues of general interest to scientists, including current affairs, science funding, business, scientific ethics and research breakthroughs. There are also sections on books and arts. The remainder of the journal consists mostly of research papers (articles or letters), which are often dense and highly technical. Because of strict limits on the length of papers, often the printed text is actually a summary of the work in question with many details relegated to accompanying supplementary material on the journal's website.";

        // wrap plain text into `Data`
        Data input = new Data<>(Discriminators.Uri.TEXT, text);

        // call `execute()` with jsonized input,
        String string = this.service.execute(input.asJson());

        System.out.println(string);
    }
}