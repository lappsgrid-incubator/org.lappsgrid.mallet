package org.lappsgrid.mallet;


import cc.mallet.classify.Classifier;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.types.Labeling;
import org.apache.axis.Version;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
        metadata.setVendor("http://www.lappsgrid.org");
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
        // Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Extract the text.
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

        // Create a new View
        View view = container.newView();

        // Get input text
        String text = container.getText();
        StringArrayIterator sai = new StringArrayIterator
                (new String[]{text});

        // get the document classification .classifier file as an InputStream
        Object classifier = data.getParameter("classifier");
        InputStream inputStream;
        if (classifier == null) {
            String defaultClassifier = "/masc_500k_texts.classifier";
            inputStream = this.getClass().getResourceAsStream(defaultClassifier);
            data.setParameter("model", this.getClass().getResource(defaultClassifier));
        } else {
            try {
                URL url = new URL(classifier.toString());
                inputStream = url.openStream();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                String message = "Path to file not valid";
                return new Data<>(Uri.ERROR, message).asJson();
            } catch (IOException e) {
                e.printStackTrace();
                String message = "Unable to open file";
                return new Data<>(Uri.ERROR, message).asJson();
            }
        }

        // load the classifier
        Classifier c;
        try {
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            c = (Classifier) ois.readObject();
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Unable to read classifier file";
            return new Data<>(Uri.ERROR, message).asJson();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            String message = "Invalid classifier file";
            return new Data<>(Uri.ERROR, message).asJson();
        }


        // classify and produce output
        Iterator instances =
                c.getInstancePipe().newIteratorFrom(sai);
        while (instances.hasNext()) {
            Labeling labeling = c.classify(instances.next()).getLabeling();

            for (int rank = 0; rank < labeling.numLocations(); rank++) {
                Annotation a = new Annotation();
                a.setId("documentType" + rank);
                a.addFeature("documentType" , labeling.getLabelAtRank(rank).toString());
                a.addFeature("probability", Double.toString(labeling.getValueAtRank(rank)));
                view.add(a);
            }
        }
        view.addContains("document types", this.getClass().getName(), "document-types:mallet");
        Map parameters = data.getParameters();
        data = new DataContainer(container);
        data.setDiscriminator(Discriminators.Uri.JSON);
        data.setParameters(parameters);

        return data.asPrettyJson();
    }
}
