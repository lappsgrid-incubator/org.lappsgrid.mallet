package org.lappsgrid.mallet;



import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.topics.tui.TopicTrainer;
import cc.mallet.types.*;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class TopicModeling implements ProcessingService
{
    public TopicModeling() { }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setDescription("Mallet Topic Modeling");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Discriminators.Uri.APACHE2);

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormat(Discriminators.Uri.TEXT);           // Plain text (form)
        requires.addLanguage("en");             // Source language

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);          // LIF (form)
        produces.addAnnotation(Discriminators.Uri.TOKEN);      // Tokens (contents)
        requires.addLanguage("en");             // Target language

        // Embed I/O metadata JSON objects
        metadata.setRequires(requires);
        metadata.setProduces(produces);

        // Serialize the metadata into LEDS string and return
        Data<ServiceMetadata> data = new Data<>(Discriminators.Uri.META, metadata);
        return data.asPrettyJson();
    }

    public String getMetadata() {
        return generateMetadata();
    }

    public String execute(String input) {
        //String[] args = {};
        //TopicTrainer.main(args);
        try {
            TopicInferencer topicInferencer = TopicInferencer.read(new File("src/main/resources/masc_500k_texts.inferencer"));
            InstanceList instances = InstanceList.load(new File(input + ".mallet"));
            topicInferencer.writeInferredDistributions(instances, new File(input + "Results.txt"),
                    100, 10, 10, 0.0, -1);
            System.out.println("Results written");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
        return null;
    }
}
