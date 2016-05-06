package org.lappsgrid.mallet;

import cc.mallet.fst.*;
import cc.mallet.fst.tests.TestCRF;
import cc.mallet.optimize.Optimizable;
import cc.mallet.optimize.tests.TestOptimizable;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.ArrayIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.pipe.tsf.OffsetConjunctions;
import cc.mallet.pipe.tsf.TokenText;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.CommandOption;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
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
        train(input);
        return null;
    }

    CRF crf;
    Pipe pipe;
    TransducerEvaluator eval = null;
    InstanceList testData = null;
    ArrayList<String> trainingFiles = new ArrayList<String>();
    public String train(String input){
        try{
            String fileName = input + ".model";
            File file = new File(fileName);
            if (file.createNewFile()){
                pipe = new SimpleTagger.SimpleTaggerSentence2FeatureVectorSequence();
                pipe.getTargetAlphabet().lookupIndex("O");
            }
            else {
                System.out.println("Model file already exists");
                return null;
            }

            pipe.setTargetProcessing(true);
            listFilesForFolder(new File(input));
            for (String filePath : trainingFiles) {
                InstanceList trainingData = new InstanceList(pipe);
                Reader inputFile = new FileReader(new File(filePath));
                trainingData.addThruPipe(new LineGroupIterator(inputFile,
                        Pattern.compile("^\\s*$"), true));
                System.out.println("Training: " + filePath);
                System.out.println("Number of features in training data: " + pipe.getDataAlphabet().size());
                System.out.println("Number of predicates: " + pipe.getDataAlphabet().size());

                if (pipe.isTargetProcessing()) {
                    Alphabet targets = pipe.getTargetAlphabet();
                    StringBuffer buf = new StringBuffer("Labels:");
                    for (int i = 0; i < targets.size(); i++)
                        buf.append(" ").append(targets.lookupObject(i).toString());
                    System.out.println(buf.toString());
                }

                int[] orders = {1};
                double var = 10.0;
                crf = SimpleTagger.train(trainingData, testData, eval, orders,
                        "O", "\\s", ".*",
                        true, 500, var, crf);
            }
            ObjectOutputStream s =
                    new ObjectOutputStream(new FileOutputStream(file));
            s.writeObject(crf);
            s.close();


        }catch (IOException e) {
            System.out.println("Failed to create file");
            e.printStackTrace();
            return null;
        }/*catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException");
            return null;
        }*/

        //if (trainingFile != null) { try{trainingFile.close();}catch(IOException e){} }
        return null;
    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                String filePath = fileEntry.getPath();
                String extension = filePath.substring(
                        filePath.lastIndexOf(".") + 1, filePath.length());
                if (extension.equals("txt")){
                    trainingFiles.add(filePath);
                }
            }

        }
    }
}
