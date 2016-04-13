package org.lappsgrid.mallet;

import cc.mallet.fst.*;
import cc.mallet.optimize.Optimizable;
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

public class TrainSequenceTagging implements ProcessingService {

    public TrainSequenceTagging() {
    }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setDescription("Mallet Sequence Tagging Trainer");
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
        InstanceList instances = readDirectory(new File(input));
        try {
            trainSequenceTagging(instances, input);
        } catch (IOException e) {
            System.out.println("Classifier file cannot be written.");
        }
        return null;
    }

    Pipe pipe;
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

        pipeList.add(new StringBuffer2String());
        pipeList.add(new SimpleTaggerSentence2TokenSequence());
        pipeList.add(new TokenSequenceLowercase());
        pipeList.add(new TokenSequenceRemoveStopwords(false,false));
        pipeList.add(new TokenSequence2FeatureVectorSequence());
        pipeList.add(new PrintInput());

        return new SerialPipes(pipeList);
    }

    public InstanceList readDirectory(File directory) {
        return readDirectories(new File[]{directory});
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

        /**
         * Test whether the string representation of the file
         * ends with the correct extension. Note that {@ref FileIterator}
         * will only call this filter if the file is not a directory,
         * so we do not need to test that it is a file.
         */
        public boolean accept(File file) {
            return file.toString().endsWith(".txt");
        }
    }

    public void trainSequenceTagging(InstanceList trainingData, String input)
            throws IOException {
        // setup:
        //    CRF (model) and the state machine
        //    CRFOptimizableBy* objects (terms in the objective function)
        //    CRF trainer
        //    evaluator and writer

        // model
        CRF crf = new CRF(trainingData.getDataAlphabet(),
                trainingData.getTargetAlphabet());
        // construct the finite state machine
        crf.addFullyConnectedStatesForLabels();
        // initialize model's weights
        crf.setWeightsDimensionAsIn(trainingData, false);

        //  CRFOptimizableBy* objects (terms in the objective function)
        // objective 1: label likelihood objective
        CRFOptimizableByLabelLikelihood optLabel =
                new CRFOptimizableByLabelLikelihood(crf, trainingData);

        // CRF trainer
        Optimizable.ByGradientValue[] opts =
                new Optimizable.ByGradientValue[]{optLabel};
        // by default, use L-BFGS as the optimizer
        CRFTrainerByValueGradients crfTrainer =
                new CRFTrainerByValueGradients(crf, opts);

        // *Note*: labels can also be obtained from the target alphabet
        String[] labels = new String[]{"I-PER", "I-LOC", "I-ORG", "I-MISC"};

        CRFWriter crfWriter = new CRFWriter(input + ".model") {
            @Override
            public boolean precondition(TransducerTrainer tt) {
                // save the trained model after training finishes
                return tt.getIteration() % Integer.MAX_VALUE == 0;
            }
        };
        crfTrainer.addEvaluator(crfWriter);

        // all setup done, train until convergence
        crfTrainer.setMaxResets(0);
        crfTrainer.train(trainingData, Integer.MAX_VALUE);


        // save the trained model (if CRFWriter is not used)
        FileOutputStream fos = new FileOutputStream(input + ".model");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(crf);
    }
}
