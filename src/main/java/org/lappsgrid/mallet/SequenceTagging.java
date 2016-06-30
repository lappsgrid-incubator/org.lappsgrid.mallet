package org.lappsgrid.mallet;

import cc.mallet.fst.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.*;
import org.apache.axis.Version;
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
import java.util.*;
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
        metadata.setAllow(Discriminators.Uri.ANY);
        metadata.setDescription("Mallet Sequence Tagging");
        metadata.setVersion(Version.getVersion());
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
        produces.addLanguage("en");             // Target language

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

    // ArrayLists for holding information about tokens
    ArrayList<String> words = new ArrayList<>();
    ArrayList<Integer> starts = new ArrayList<>();
    ArrayList<Integer> ends = new ArrayList<>();

    public String execute(String input) {
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

        // Get the text from the container
        String text = container.getText();

        // tokenize the text. tokens go into the String[] words
        // the starts and ends are stored in an int[]
        tokenize(text);

        // turns text into the format Mallet requires
        StringBuilder textFormatted = new StringBuilder();
        for (String word : words) {
            textFormatted.append(word);
            textFormatted.append('\n');
        }

        // get the sequence tagging model file
        String modelName = "masc_500k_texts";
        InputStream inputStream =
                this.getClass().getResourceAsStream
                        ("/" + modelName + ".model");

        // add the name of the model file to the metadata
        data.setParameter("model", modelName + ".model");

        CRF crf;
        Pipe p;
        try {
            // get trained sequence tagging model
            ObjectInputStream s = new ObjectInputStream(inputStream);
            crf = (CRF) s.readObject();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
            String message = String.format
                    ("Unable to read model file: %s.model", modelName);
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            String message = String.format
                    ("Invalid model file: %s.model", modelName);
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }


        // process input
        p = crf.getInputPipe();
        p.setTargetProcessing(false);
        InstanceList instanceList = new InstanceList(p);
        instanceList.addThruPipe
                (new LineGroupIterator(new StringReader(textFormatted.toString()),
                        Pattern.compile("^\\s*$"), true));

        // apply model
        for (Instance instance : instanceList){
            Sequence inputs = (Sequence) instance.getData();
            Sequence[] outputs = apply(crf, inputs, 1);
            boolean error = false;
            for (Sequence output : outputs){
                if (output.size() != inputs.size()){
                    error = true;
                }
            }
            if (!error) {
                for (int j = 0; j < inputs.size(); j++) {

                    // predicting the POS for each word
                    StringBuilder buf = new StringBuilder();
                    for (Sequence output : outputs){
                        buf.append(output.get(j).toString());
                    }

                    // adding annotations
                    Annotation a = view.newAnnotation("tok" + j, Discriminators.Uri.TOKEN,
                            starts.get(j), ends.get(j));
                    a.addFeature(Features.Token.WORD, words.get(j));
                    a.addFeature(Features.Token.POS, buf.toString());
                }
            }
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

    public void tokenize(String text) {
        // initialization of variables needed for tokenization
        int charCount = 0, start = 0, end;
        StringBuilder token = new StringBuilder();
        char[] punctuation = ":;.!?\'\"(){}[]".toCharArray();

        // iterate through the text to tokenize
        for (char ch : text.toCharArray()) {
            if (Character.isLetter(ch)) { // process letters
                token.append(ch);
            } else if (Character.isSpaceChar(ch)) { // process space chars
                if (token.length() > 0) {
                    end = charCount - 1;
                    addToken(token, start, end);
                    token = new StringBuilder();
                }
                start = charCount + 1;
            } else if (ch == '\'' && token.length() > 0) { // process contractions
                end = charCount - 1;
                addToken(token, start, end);
                token = new StringBuilder();
                token.append('\'');
                start = charCount;
            } else if (Arrays.binarySearch(punctuation, ch) != -1) { // process punctuation
                if (token.length() > 0) {
                    end = charCount - 1;
                    addToken(token, start, end);
                }
                start = charCount;
                addToken(ch, start);
                token = new StringBuilder();
                start = charCount + 1;
            }
            charCount++;
        }
        if (token.length() > 0) { // process last token
            end = charCount - 1;
            addToken(token, start, end);
        }
    }

    public void addToken(StringBuilder token, int start, int end) {
        words.add(token.toString());
        starts.add(start);
        ends.add(end);
    }

    public void addToken(char ch, int start) {
        words.add(Character.toString(ch));
        starts.add(start);
        ends.add(start);
    }
}
