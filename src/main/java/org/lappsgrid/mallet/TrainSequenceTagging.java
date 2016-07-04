package org.lappsgrid.mallet;

import cc.mallet.fst.CRF;
import cc.mallet.fst.SimpleTagger;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.LineGroupIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import cc.mallet.util.ArrayUtils;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

public class TrainSequenceTagging implements ProcessingService {

    public TrainSequenceTagging() {
    }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setAllow(Discriminators.Uri.ANY);
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

    ArrayList<String> trainingFiles = new ArrayList<>();
    public String execute(String input) {
        // Step #1: Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Step #2: Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Discriminators.Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Get the directory specified by the parameters in the input
        String folder = data.getParameter("directory").toString();

        // Get the location to which the model will be written
        String path = data.getParameter("path").toString();
        String modelName = data.getParameter("modelName").toString();
        File file = new File(path + "/" + modelName);
        if (file.exists()){
            return new Data<>(Discriminators.Uri.ERROR, "File already exists").asJson();
        }

        // Populate an ArrayList with the paths to all the .txt files
        // in the specified directory used for training
        listFilesForFolder(new File(folder));

        File[] sortedTrainingFiles = new File[trainingFiles.size()];
        for (int i = 0; i < trainingFiles.size(); i++){
            sortedTrainingFiles[i] = new File(trainingFiles.get(i));
        }
        Arrays.sort(sortedTrainingFiles, new FileSizeComparator());
        for (File f : sortedTrainingFiles) {
            System.out.println(f);
        }

        // train first file
        try {
            SimpleTagger.main(
                    new String[]{"--train", "true",
                            "--model-file", path + "/" + modelName,
                            sortedTrainingFiles[0].getPath()});
        } catch (Exception e){
            e.printStackTrace();
            return new Data<>(Discriminators.Uri.ERROR, "Error while training data").asJson();
        }

        int numFiles = sortedTrainingFiles.length;

        // feed rest of the files to the trainer one at a time
        for (int i = 1; i < numFiles; i++) {
            try {
                SimpleTagger.main(
                        new String[]{"--train", "true",
                                "--continue-training", "true",
                                "--model-file", path + "/" + modelName,
                                sortedTrainingFiles[i].getPath()});
            } catch (Exception e){
                e.printStackTrace();
                return new Data<>(Discriminators.Uri.ERROR, "Error while training data").asJson();
            }
        }

        // Success
        return new Data<>(Discriminators.Uri.TEXT, "Success").asJson();
    }


    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                String filePath = fileEntry.getPath();
                String extension = filePath.substring(
                        filePath.lastIndexOf(".") + 1, filePath.length());
                if (extension.equals("txt")) {
                    trainingFiles.add(filePath);
                }
            }
        }
    }

    // Compares sizes of files in order to sort them by size in ascending order
    public class FileSizeComparator implements Comparator<File> {
        public int compare( File a, File b ) {
            long aSize = a.length();
            long bSize = b.length();
            if ( aSize == bSize ) {
                return 0;
            }
            else {
                return Long.compare(bSize, aSize);
            }
        }
    }
}
