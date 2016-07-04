package org.lappsgrid.mallet;



import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.*;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class TrainTopicModeling implements ProcessingService
{
    public TrainTopicModeling() { }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setAllow(Discriminators.Uri.ANY);
        metadata.setDescription("Mallet Topic Modeling Trainer");
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

    Pipe pipe;
    public String execute(String input) {
        // Step #1: Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Step #2: Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Discriminators.Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Create a series of pipes to process the training files
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new Input2CharSequence("UTF-8"));
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords());
        pipeList.add( new TokenSequence2FeatureSequence());
        pipe = new SerialPipes(pipeList);

        // put the directory of files used for training through the pipes
        String directory = data.getParameter("directory").toString();
        InstanceList instances = readDirectory(new File(directory));

        // create a topic to be trained
        int numberOfTopics = (Integer) data.getParameter("numTopics");
        ParallelTopicModel topicModel = new ParallelTopicModel(numberOfTopics);
        topicModel.addInstances(instances);

        // train the model
        try {
            topicModel.estimate();
        } catch (IOException e){
            e.printStackTrace();
            return new Data<>(Discriminators.Uri.ERROR,
                    "Unable to train the model").asJson();
        }

        // write topic keys file
        String path = data.getParameter("path").toString();
        String keysName = data.getParameter("keysName").toString();
        int wordsPerTopic = (Integer) data.getParameter("wordsPerTopic");
        try {
            topicModel.printTopWords(new File(path + "/" + keysName), wordsPerTopic, false);
        } catch (IOException e) {
            e.printStackTrace();
            return new Data<>(Discriminators.Uri.ERROR,
                    "Unable to write the topic keys to " + path + "/" + keysName).asJson();
        }

        // write the .inferencer file
        String inferencerName = data.getParameter("inferencerName").toString();
        try {
            ObjectOutputStream oos =
                    new ObjectOutputStream (new FileOutputStream (path + "/" + inferencerName));
            oos.writeObject (topicModel.getInferencer());
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return new Data<>(Discriminators.Uri.ERROR,
                    "Unable to write the inferencer to " + path + "/" + inferencerName).asJson();
        }

        // Success
        return new Data<>(Discriminators.Uri.TEXT, "Success").asJson();
    }

    public InstanceList readDirectory(File directory) {
        return readDirectories(new File[] {directory});
    }

    public InstanceList readDirectories(File[] directories) {

        // Construct a file iterator, starting with the
        //  specified directories, and recursing through subdirectories.
        // The second argument specifies a FileFilter to use to select
        //  files within a directory.
        // The third argument is a Pattern that is applied to the
        //   filename to produce a class label. In this case, I've
        //   asked it to use the last directory name in the path.
        FileIterator iterator =
                new FileIterator(directories,
                        new TxtFilter(),
                        FileIterator.LAST_DIRECTORY);

        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        InstanceList instances = new InstanceList(pipe);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }

    class TxtFilter implements FileFilter {

        /** Test whether the string representation of the file
         *   ends with the correct extension. Note that {@ref FileIterator}
         *   will only call this filter if the file is not a directory,
         *   so we do not need to test that it is a file.
         */
        public boolean accept(File file) {
            return file.toString().endsWith(".txt");
        }
    }
}
