package org.lappsgrid.mallet;

import cc.mallet.extract.StringTokenization;
import cc.mallet.fst.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.*;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

import java.io.*;
import java.util.ArrayList;
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
        pipe = buildPipe();
        try {
            CRF crf = loadModel(new File("dataCOPY.model"));
            InstanceList instances = readDirectory(new File(input));
            for (Instance instance : instances) {
                StringTokenization stringTokenization = (StringTokenization) instance.getData();

                Token[] tokens = new Token[stringTokenization.size()];
                String[] strings = new String[stringTokenization.size()];

                for (int i = 0 ; i < tokens.length ; i++) {
                    String string = stringTokenization.get(i).getText();
                    Token token = new Token(string);
                    token.setFeatureValue(string, 1);
                    tokens[i] = token;
                    strings[i] = string;
                }
                TokenSequence tokenSequence = new TokenSequence(tokens);
                Alphabet alphabet = new Alphabet(strings);
                FeatureVectorSequence data = new FeatureVectorSequence(alphabet, tokenSequence);

                Sequence output = crf.transduce(data);
                for (int index = 0; index < data.size(); index++) {
                    String word = data.get(index).toString();
                    System.out.print(word.substring(0,word.length()-1) + "/");
                    System.out.print(output.get(index) + " ");
                }
            }

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

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers,
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
                Pattern.compile("[\\p{L}\\p{N}_]+|[\\p{P}]+");
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));
        pipeList.add(new TokenSequenceLowercase());

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
