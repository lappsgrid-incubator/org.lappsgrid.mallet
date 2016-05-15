package org.lappsgrid.mallet;

import cc.mallet.fst.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.*;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
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
import java.util.Map;
import java.util.regex.Pattern;

import static cc.mallet.fst.SimpleTagger.apply;


public class SequenceTagging implements ProcessingService {
    public SequenceTagging() {
    }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setDescription("Mallet Sequence Tagging");
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
        InstanceList instanceList;

        // Step #1: Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Step #2: Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Discriminators.Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Step #3: Extract the text.
        Container container;
        if (discriminator.equals(Discriminators.Uri.TEXT)) {
            container = new Container();
            container.setText(data.getPayload().toString());
        } else if (discriminator.equals(Discriminators.Uri.LAPPS)) {
            container = new Container((Map) data.getPayload());
        } else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }

        // Step #4: Create a new View
        View view = container.newView();

        // Get input text and write a temporary file
        String text = container.getText();


        try {
            Pipe p;
            InputStream inputStream = this.getClass().getResourceAsStream("/masc_500k_texts.model");
//            ObjectInputStream s =
//                    new ObjectInputStream(new FileInputStream("src/main/resources/masc_500k_texts.model"));
            ObjectInputStream s = new ObjectInputStream(inputStream);

            CRF crf = (CRF) s.readObject();
            s.close();
            p = crf.getInputPipe();
            p.setTargetProcessing(false);
            instanceList = new InstanceList(p);

            instanceList.addThruPipe(new LineGroupIterator(new StringReader(text),
                    Pattern.compile("^\\s*$"), true));
            System.out.println("Number of predicates: " + p.getDataAlphabet().size());
            for (int i = 0; i < instanceList.size(); i++) {
                Sequence inputs = (Sequence) instanceList.get(i).getData();
                Sequence[] outputs = apply(crf, inputs, 1);
                int k = outputs.length;
                boolean error = false;
                for (int a = 0; a < k; a++) {
                    if (outputs[a].size() != inputs.size()) {
                        error = true;
                    }
                }
                if (!error) {
                    int start = 0;
                    for (int j = 0; j < inputs.size(); j++) {

                        // getting the POS for each word
                        StringBuilder buf = new StringBuilder();
                        for (int a = 0; a < k; a++) {
                            buf.append(outputs[a].get(j).toString()).append(" ");
                        }

                        String word = inputs.get(j).toString();
                        word = word.substring(0, word.length() - 1);
                        start = text.indexOf(word, start);
                        int end = start + word.length();
                        Annotation a = view.newAnnotation("tok" + j, Discriminators.Uri.TOKEN, start, end);
                        a.addFeature(Features.Token.POS, buf.toString());

                        System.out.println(word + " " + buf.toString());
                    }
                    System.out.println();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Can't read file");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("ClassNotFoundException");
        }

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Discriminators.Uri.POS, this.getClass().getName(), "part of speech");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asPrettyJson();

    }
}
