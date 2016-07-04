package org.lappsgrid.mallet;


import cc.mallet.classify.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.InstanceList;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class TrainClassifier implements ProcessingService {
    public TrainClassifier() {
    }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setAllow(Discriminators.Uri.ANY);
        metadata.setDescription("Mallet Document Classifier Trainer");
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

        // Get the directory specified in the parameters
        URI uri;
        try {
            uri = new URI(data.getParameter("directory").toString());
        } catch (URISyntaxException e){
            e.printStackTrace();
            String message = "Path to file is invalid";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }
        File f = new File(uri);

        // get the desired name and location of the .classifier file
        String filePath, classifierName;
        try {
            filePath = data.getParameter("path").toString();
        } catch (NullPointerException e){
            e.printStackTrace();
            String message = "No file path specified";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }
        try {
            classifierName = data.getParameter("classifierName").toString();
        } catch (NullPointerException e){
            e.printStackTrace();
            String message = "No file name specified";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }

        // get desired training model type
        String modelType;
        try {
            modelType = data.getParameter("trainer").toString();
        } catch (NullPointerException e){
            modelType = "NaiveBayes";
        }

        // train a classifier file using the specified directory
        pipe = buildPipe();
        InstanceList instances = readDirectory(f);
        Classifier classifier;
        try {
            classifier = trainClassifier(instances, modelType);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            String message = modelType + " is not a valid classifier trainer.";
            return new Data<>(Discriminators.Uri.ERROR, message).asJson();
        }

        // try to save the classifier
        try {
            saveClassifier(classifier, new File(filePath + "/" + classifierName));
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("Classifier file cannot be written.");
        }

        // success
        return new Data<>(Discriminators.Uri.TEXT, "Success").asJson();
    }

    public Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers,
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
                Pattern.compile("[\\p{L}\\p{N}_]+");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        // Rather than storing tokens as strings, convert
        //  them to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field:
        //  convert a class label string to a Label object,
        //  which has an index in a Label alphabet.
        pipeList.add(new Target2Label());

        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
        pipeList.add(new FeatureSequence2FeatureVector());

        return new SerialPipes(pipeList);
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

    public Classifier trainClassifier
            (InstanceList trainingInstances, String modelType) throws IllegalArgumentException {

        // Here we use a maximum entropy (ie polytomous logistic regression)
        //  classifier. Mallet includes a wide variety of classification
        //  algorithms, see the JavaDoc API for details.

        ClassifierTrainer trainer;
        switch (modelType) {
            case "NaiveBayes":
                trainer = new NaiveBayesTrainer();
                break;
            case "MaxEnt":
                trainer = new MaxEntTrainer();
                break;
            case "BalancedWinnow":
                trainer = new BalancedWinnowTrainer();
                break;
            case "C45":
                trainer = new C45Trainer();
                break;
            case "DecisionTree":
                trainer = new DecisionTreeTrainer();
                break;
            case "MaxEntGERange":
                trainer = new MaxEntGERangeTrainer();
                break;
            case "MaxEntGE":
                trainer = new MaxEntGETrainer();
                break;
            case "MaxEntL1":
                trainer = new MaxEntL1Trainer();
                break;
            case "MaxEntPR":
                trainer = new MaxEntPRTrainer();
                break;
            case "MCMaxEnt":
                trainer = new MCMaxEntTrainer();
                break;
            case "NaiveBayesEMT":
                trainer = new NaiveBayesEMTrainer();
                break;
            case "RankMaxEnt":
                trainer = new RankMaxEntTrainer();
                break;
            case "Winnow":
                trainer = new WinnowTrainer();
                break;
            default:
                throw new IllegalArgumentException();
        }
        return trainer.train(trainingInstances);
    }

    public void saveClassifier(Classifier classifier, File serializedFile)
            throws IOException {

        // The standard method for saving classifiers in
        //  Mallet is through Java serialization. Here we
        //  write the classifier object to the specified file.

        ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(serializedFile));
        oos.writeObject (classifier);
        oos.close();
    }
}
