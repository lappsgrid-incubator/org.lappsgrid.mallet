package org.lappsgrid.mallet;



import cc.mallet.classify.Classifier;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Labeling;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.io.*;
import java.util.Iterator;
import java.util.Map;


public class DocumentClassification implements ProcessingService
{
    public DocumentClassification() { }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setDescription("Mallet Document Classifier");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Uri.APACHE2);

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormat(Uri.TEXT);           // Plain text (form)
        requires.addLanguage("en");             // Source language

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);          // LIF (form)
        produces.addAnnotation(Uri.TOKEN);      // Tokens (contents)
        requires.addLanguage("en");             // Target language

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
        Container container = null;
        if (discriminator.equals(Uri.TEXT)) {
            container = new Container();
            container.setText(data.getPayload().toString());
        }
        else if (discriminator.equals(Uri.LAPPS)) {
            container = new Container((Map) data.getPayload());
        }
        else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<String>(Uri.ERROR, message).asJson();
        }

        // Step #4: Create a new View
        View view = container.newView();

        // Get input text and write a temporary file
        String text = container.getText();
        File temp = new File("temp");
        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream(new FileOutputStream(temp));
            oos.writeObject(text);
            oos.close();
        } catch (IOException e) {
            return new Data<String>("Error writing temporary file").asJson();
        }

        // convert strings of file names in File objects
        File classifierFile = new File("src/main/resources/masc_500k_texts.classifier");
        Classifier classifier;
        try
        {
            // load the classifier and guess the labeling of the input file
            ObjectInputStream ois =
                    new ObjectInputStream(new FileInputStream(classifierFile));
            classifier = (Classifier) ois.readObject();
            ois.close();

            CsvIterator reader =
                    new CsvIterator(new FileReader(temp),
                            "(\\w+)\\s+(\\w+)\\s+(.*)",
                            3, 2, 1);

            Iterator instances =
                    classifier.getInstancePipe().newIteratorFrom(reader);

            while (instances.hasNext()) {
                Labeling labeling = classifier.classify(instances.next()).getLabeling();

                // print the labels with their weights in descending order (ie best first)
                for (int rank = 0; rank < labeling.numLocations(); rank++) {
                    Annotation a = view.newAnnotation(
                            labeling.getLabelAtRank(rank).toString(),
                            Uri.TEXT);
                    a.addFeature(Features.Token.TYPE, Double.toString(labeling.getValueAtRank(rank)));

                    System.out.println(labeling.getLabelAtRank(rank) + ":" + labeling.getValueAtRank(rank));
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("File not found.");
            System.out.println(temp.getAbsolutePath());
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("ClassNotFoundException");
        }

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Uri.TOKEN, this.getClass().getName(), "labels");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asJson();
    }
}
