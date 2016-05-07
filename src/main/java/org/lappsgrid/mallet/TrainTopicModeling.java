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

    ArrayList<String> trainingFiles = new ArrayList<String>();
    Pipe pipe;
    public String execute(String input) {
        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        pipeList.add(new Input2CharSequence("UTF-8"));
        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords());
        pipeList.add( new TokenSequence2FeatureSequence() );
        pipe = new SerialPipes(pipeList);

        InstanceList instances = readDirectory(new File(input));

/*        // creating a .mallet file that holds all of the input
        String fileName = input + ".mallet";
        File file = new File(fileName);
        try {
            if (!file.createNewFile()) {
                System.out.println(".mallet file already exists");
                return null;
            }
            ObjectOutputStream oos;
            oos = new ObjectOutputStream(new FileOutputStream(input + ".mallet"));
            oos.writeObject(instances);
            oos.close();
        } catch (IOException e) {
            System.out.println("Error writing .mallet file");
            return null;
        }*/

        int numberOfTopics = 10;
        double alpha = 5.0;
        double beta = 0.1;
        ParallelTopicModel topicModel = new ParallelTopicModel (numberOfTopics, alpha, beta);


        topicModel.addInstances(instances);

        topicModel.setTopicDisplay(50, 20);

        topicModel.setNumIterations(1000);
        topicModel.setOptimizeInterval(0);
        topicModel.setBurninPeriod(200);
        topicModel.setSymmetricAlpha(false);
        topicModel.setNumThreads(1);
        try {
            topicModel.estimate();
        } catch (IOException e){
            return null;
        }

        // write topic keys file
        try {
            topicModel.printTopWords(new File(input + "TopWords.txt"), 20, false);
        } catch (IOException e) {
            System.out.println("Error writing topic keys file");
        }

        // write topic decomposition file
/*
        try {
            PrintWriter out = new PrintWriter(new FileWriter((new File(input + "TopicDecomposition.txt"))));
            topicModel.printTopicDocuments(out, 100);
            out.close();
        } catch (IOException e) {
            System.out.println("Error writing document topic decomposition file");
        }
*/

        try {

            ObjectOutputStream oos =
                    new ObjectOutputStream (new FileOutputStream (input + ".inferencer"));
            oos.writeObject (topicModel.getInferencer());
            oos.close();

        } catch (Exception e) {
            System.out.println("Couldn't write topic model inferencer to filename " + input + ".inferencer");
        }
        return null;
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
