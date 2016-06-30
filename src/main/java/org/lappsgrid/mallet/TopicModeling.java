package org.lappsgrid.mallet;



import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.InstanceList;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class TopicModeling implements ProcessingService
{
    public TopicModeling() { }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setAllow(Discriminators.Uri.ANY);
        metadata.setDescription("Mallet Topic Modeling");
        metadata.setVersion(Version.getVersion());
        metadata.setVendor("http://http://www.lappsgrid.org/");
        metadata.setLicense(Discriminators.Uri.APACHE2);

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormat(Discriminators.Uri.TEXT);           // Plain text (form)
        requires.addLanguage("en");             // Source language

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);          // LIF (form)
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

    Data data;
    public String execute(String input) {
        // Step #1: Parse the input.
        data = Serializer.parse(input, Data.class);

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

        // process input into the an InstanceList
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new Input2CharSequence());
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence());
        pipeList.add(new TokenSequenceRemoveStopwords());
        pipeList.add(new TokenSequence2FeatureSequence());
        Pipe p = new SerialPipes(pipeList);
        InstanceList instances = new InstanceList(p);
        instances.addThruPipe(new StringArrayIterator
                (new String[]{text}));

        // get the topic modeling inferencer file as an InputStream
        Object inferencer = data.getParameter("inferencer");
        InputStream inputStream;
        if (inferencer == null) {
            String defaultInferencer = "/masc_500k_texts.inferencer";
            inputStream = this.getClass().getResourceAsStream(defaultInferencer);
            data.setParameter("inferencer", this.getClass().getResource(defaultInferencer));
        } else {
            try {
                URL url = new URL(inferencer.toString());
                inputStream = url.openStream();
            } catch (Exception e){ // TODO: handle exceptions more specifically
                e.printStackTrace();
                return null;
            }
        }

        // turn the inferencer InputStream into a TopicInferencer object
        TopicInferencer ti;
        try {
            ObjectInputStream ois = new ObjectInputStream(inputStream);
            ti = (TopicInferencer) ois.readObject();
            ois.close();
        } catch (IOException e) {
            e.printStackTrace();
            String message = "Unable to read inferencer file";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            String message = "Invalid inferencer file";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }

        // get the topic modeling topic keys file
        BufferedReader br;
        Object keys = data.getParameter("keys");
        String defaultKeys = "/masc_500k_texts_topic_keys.txt";
        if (keys == null || keys == defaultKeys) {
            inputStream = this.getClass().getResourceAsStream(defaultKeys);
            data.setParameter("keys", this.getClass().getResource(defaultKeys));
        } else{
            try {
                URL url = new URL(keys.toString());
                inputStream = url.openStream();
            } catch (Exception e){ // TODO: handle exceptions more specifically
                e.printStackTrace();
                return null;
            }
        }
        br = new BufferedReader(new InputStreamReader(inputStream));

        // read the topic keys .txt file and store topics in an ArrayList
        ArrayList<String> topicKeys = new ArrayList<>();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                int tabs = 0;
                int lgth = line.length();
                for (int i = 0; i < lgth; i++){
                    // looking for 2 tabs
                    if (Character.isSpaceChar(line.charAt(i))){
                        tabs++;
                    }
                    // topic keys start on 3rd column (after 2 tabs)
                    if (tabs == 2){
                        if (lgth > i) {
                            topicKeys.add(line.substring(i+1));
                        }
                        break;
                    }
                }
            }
            br.close();
        } catch (IOException e){
            e.printStackTrace();
            String message = "Unable to read topic keys file";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }

        // get sampling parameters
        int numIterations, thinning, burnIn;
        try {
            numIterations = getIntParameter("numIterations", 100);
            thinning = getIntParameter("thinning", 10);
            burnIn = getIntParameter("burnIn", 10);
        } catch (ClassCastException e){
            e.printStackTrace();
            return new Data<>(Discriminators.Uri.ERROR, e.getMessage()).asJson();
        }

        // get the proportion of each topic in our text
        double[] sampledDistribution =
                ti.getSampledDistribution(instances.get(0), numIterations, thinning, burnIn);

        // return an error if the inferencer contains a different number of
        // topics than the topic keys file
        int numberOfTopics = sampledDistribution.length;
        int numberOfKeys = topicKeys.size();
        if (numberOfTopics != numberOfKeys){
            String message = String.format(
                    "Number of topics and number of topic keys are different. " +
            "Number of topics:%d Number of keys: %d", numberOfTopics, numberOfKeys);
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }

        // put each topic with its corresponding proportion in a Map
        Map<String, Double> topicProportions = new HashMap<>();
        for (int i = 0; i < numberOfTopics; i++){
            topicProportions.put(topicKeys.get(i), sampledDistribution[i]);
        }
        data.setPayload(topicProportions);
        data.setDiscriminator(Discriminators.Uri.JSON);

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Discriminators.Uri.TOKEN, this.getClass().getName(), "topics");

        // Step #7: Create a DataContainer with the result.
        // data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asPrettyJson();
    }

    // gets an integer parameter from the data being passed through
    public int getIntParameter(String parameterName, int defaultInt) throws ClassCastException{
        Object temp;

        temp = data.getParameter(parameterName);
        if (temp == null){
            data.setParameter(parameterName, defaultInt);
            return defaultInt;
        }
        if (temp instanceof Integer) {
            return (int) temp;
        } else {
            ClassCastException e =
                    new ClassCastException("Unable to use parameter: " + parameterName);
            throw e;
        }
    }
}
