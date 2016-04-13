package org.lappsgrid.mallet;

import cc.mallet.classify.Classifier;
import cc.mallet.fst.CRF;
import cc.mallet.fst.MultiSegmentationEvaluator;
import cc.mallet.fst.TransducerEvaluator;
import cc.mallet.fst.TransducerTrainer;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.InstanceList;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Pattern;


public class SequenceTagging implements ProcessingService
{
    public SequenceTagging() { }

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
        pipe = buildPipe();
        String[] labels = new String[]{"I-PER", "I-LOC", "I-ORG", "I-MISC"};
        try {
            CRF crf = loadModel(new File("sequence tagging data.model"));
            InstanceList instanceList = readDirectory(new File(input));
            TransducerEvaluator evaluator = new MultiSegmentationEvaluator(
                    new InstanceList[]{instanceList},
                    new String[]{"test"}, labels, labels) {
                @Override
                public boolean precondition(TransducerTrainer tt) {
                    // evaluate model every 5 training iterations
                    return tt.getIteration() % 5 == 0;
                }};
                crf.induceFeaturesFor(instanceList);

        }
        catch (IOException e) {
            System.out.println("Can't load model");
        }
        catch (ClassNotFoundException e) {
            System.out.println("Can't load model");
        }

        return null;
           }

    public CRF loadModel(File serializedFile)
            throws IOException, ClassNotFoundException {

        // The standard way to save classifiers and Mallet data
        //  for repeated use is through Java serialization.
        // Here we load a serialized classifier from a file.

        CRF crf;

        ObjectInputStream ois =
                new ObjectInputStream(new FileInputStream(serializedFile));
        crf = (CRF) ois.readObject();
        ois.close();

        return crf;
    }

    Pipe pipe;
    public Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));
        pipeList.add(new StringBuffer2String());
        pipeList.add(new SimpleTaggerSentence2TokenSequence());
        pipeList.add(new TokenSequenceLowercase());
        pipeList.add(new TokenSequenceRemoveStopwords(false,false));
        pipeList.add(new TokenSequence2FeatureVectorSequence());
        pipeList.add(new PrintInput());

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

}
