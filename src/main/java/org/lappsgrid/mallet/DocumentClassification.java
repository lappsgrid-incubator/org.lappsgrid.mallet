package org.lappsgrid.mallet;


import cc.mallet.classify.Classifier;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.types.Labeling;
import org.apache.axis.Version;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class DocumentClassification implements ProcessingService {
    public DocumentClassification() {
    }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setAllow(Uri.ANY);
        metadata.setDescription("Mallet Document Classifier");
        metadata.setVersion(Version.getVersion());
        metadata.setVendor("http://www.anc.org");
        metadata.setLicense(Uri.APACHE2);

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormat(Uri.TEXT);           // Plain text (form)
        requires.addLanguage("en");             // Source language

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);          // LIF (form)
        produces.addLanguage("en");             // Target language

        // Embed I/O metadata JSON objects
        metadata.setRequires(requires);
        metadata.setProduces(produces);

        // Serialize the metadata into LEDS string and return
        Data<ServiceMetadata> data = new Data<>(Uri.META, metadata);
        return data.asPrettyJson();
    }

    public String getMetadata() {
        return generateMetadata();
    }

    public String execute(String input) {
        // Step #1: Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Step #2: Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Step #3: Extract the text.
        Container container;
        if (discriminator.equals(Uri.TEXT)) {
            container = new Container();
            container.setText(data.getPayload().toString());
        } else if (discriminator.equals(Uri.LAPPS)) {
            container = new Container((Map) data.getPayload());
        } else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<>(Uri.ERROR, message).asJson();
        }

        // Step #4: Create a new View
        View view = container.newView();

        // Get input text
        String text = container.getText();
        StringArrayIterator sai = new StringArrayIterator
                (new String[]{text});

        // get the classifier file
        String classifierName = "masc_500k_texts";
        InputStream inputStream =
                this.getClass().getResourceAsStream
                        ("/" + classifierName + ".classifier");

        // add the name of the classifier to the metadata
        data.setParameter("classifier", classifierName + ".classifier");

        // load the classifier
        Classifier classifier;
        try {
            ObjectInputStream s = new ObjectInputStream(inputStream);
            classifier = (Classifier) s.readObject();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
            String message = String.format
                    ("Unable to read classifier file: %s.classifier", classifierName);
            return new Data<>(Uri.ERROR, message).asJson();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            String message = String.format
                    ("Invalid classifier file: %s.classifier", classifierName);
            return new Data<>(Uri.ERROR, message).asJson();
        }

        // map to store the probability of the input being each document type
        Map<String, Double> typeProbability = new HashMap<>();

        Iterator instances =
                classifier.getInstancePipe().newIteratorFrom(sai);
        while (instances.hasNext()) {
            Labeling labeling = classifier.classify(instances.next()).getLabeling();

            // print the labels with their weights in descending order (ie best first)
            for (int rank = 0; rank < labeling.numLocations(); rank++) {
                typeProbability.put(labeling.getLabelAtRank(rank).toString(), labeling.getValueAtRank(rank));
            }
        }
        data = new Data<Map>(Uri.JSON, typeProbability);


        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Uri.TOKEN, this.getClass().getName(), "labels");

        // Step #7: Create a DataContainer with the result.
//        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asPrettyJson();
    }
}
