package org.lappsgrid.mallet;

import cc.mallet.extract.StringTokenization;
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
import cc.mallet.types.*;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

import static cc.mallet.fst.SimpleTagger.apply;


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
        Reader testFile;
        InstanceList testData;
        try {
            testFile = new FileReader(new File(input + ".txt"));
            Pipe p;
            ObjectInputStream s =
                    new ObjectInputStream(new FileInputStream("dataCOPY.model"));
            CRF crf = (CRF) s.readObject();
            s.close();
            p = crf.getInputPipe();
            TransducerEvaluator eval = null;
            p.setTargetProcessing(false);
            testData = new InstanceList(p);
            testData.addThruPipe(new LineGroupIterator(testFile,
                    Pattern.compile("^\\s*$"), true));
            System.out.println("Number of predicates: "+p.getDataAlphabet().size());
            for (int i = 0; i < testData.size(); i++) {
                Sequence inputs = (Sequence)testData.get(i).getData();
                Sequence[] outputs = apply(crf, inputs, 1);
                int k = outputs.length;
                boolean error = false;
                for (int a = 0; a < k; a++) {
                    if (outputs[a].size() != inputs.size()) {
                        error = true;
                    }
                }
                if (! error) {
                    for (int j = 0; j < inputs.size(); j++) {
                        StringBuffer buf = new StringBuffer();
                        for (int a = 0; a < k; a++) {
                            buf.append(outputs[a].get(j).toString()).append(" ");
                        }
                        String word = inputs.get(j).toString();
                        System.out.println(word.substring(0,word.length()-1) + " "  + buf.toString());
                    }
                    System.out.println();
                }
            }
        }
        catch (IOException e) {
            System.out.println("Can't read file");
        }
        catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException");
        }
        return null;
    }
}
