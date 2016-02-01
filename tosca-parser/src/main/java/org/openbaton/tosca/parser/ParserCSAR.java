/*
 * Copyright (c) 2016 Fraunhofer FOKUS
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.openbaton.tosca.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.openbaton.catalogue.mano.common.LifecycleEvent;
import org.openbaton.catalogue.mano.descriptor.NetworkServiceDescriptor;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.catalogue.nfvo.VNFPackage;
import org.openbaton.nfvo.repositories.VNFDRepository;
import org.openbaton.nfvo.repositories.VnfPackageRepository;
import org.openbaton.openbaton.Image;
import org.openbaton.openbaton.Metadata;
import org.openbaton.tosca.catalogue.Definitions;
import org.openbaton.tosca.catalogue.exceptions.NotFoundException;
import org.openbaton.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by dbo on 25/01/16.
 */

@Service
public class ParserCSAR {

    @Autowired
    private ParserTosca parserTosca;
    @Autowired
    private VnfPackageRepository vnfPackageRepository;
    @Autowired
    private VNFDRepository vnfdRepository;

    private Definitions definition;
    private String author;
    private String version;
    private String pathUnzipFiles;
    private Set<String> vnfPackagesPaths;


    private Logger log = LoggerFactory.getLogger(this.getClass());

    public ParserCSAR() {
    }

    public ParserCSAR(String zipPath) throws Exception {
        storeScriptsFromCSAR(zipPath);
    }


    @Override
    public String toString() {
        return "ParserCSAR{" +
                "definition=" + definition +
                ", author='" + author + '\'' +
                ", version='" + version + '\'' +
                ", pathUnzipFiles='" + pathUnzipFiles + '\'' +
                '}';
    }

    public Definitions getDefinition() {
        return definition;
    }

    public void setDefinition(Definitions definition) {
        this.definition = definition;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void storeScriptsFromCSAR(String zipFile) throws Exception {

        log.debug("UnZipping the file: " +zipFile);
        VNFPackage vnfPackage = new VNFPackage();
        vnfPackage.setScripts(new HashSet<Script>());


        int BUFFER = 2048;
        File file = new File(zipFile);

        ZipFile zip = new ZipFile(file);
        String newPath = zipFile.substring(0, zipFile.length() - 5);
        System.out.println(newPath);
        this.pathUnzipFiles = newPath;

        new File(newPath).mkdir();
        Enumeration zipFileEntries = zip.entries();

        List<String> fileList = new ArrayList<>();
        // Process each entry
        while (zipFileEntries.hasMoreElements()) {
            // grab a zip file entry
            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

            String currentEntry = entry.getName();
            fileList.add(currentEntry.trim());
//
//            System.out.println(currentEntry);
//            System.out.println(entry.isDirectory());


            File destFile = new File(newPath + '/' + currentEntry, currentEntry);
            destFile = new File(newPath, destFile.getName());

            File destinationParent = destFile.getParentFile();
            String pathEntry = "" + destinationParent + '/' + currentEntry;


            if (entry.isDirectory()) {
                new File(pathEntry).mkdir();
            }

            if (!entry.isDirectory()) {
                destFile = new File(pathEntry);

                BufferedInputStream is = new BufferedInputStream(zip
                        .getInputStream(entry));
                int currentByte;
                // establish buffer for writing file
                byte data[] = new byte[BUFFER];

                // write the current file to disk
                FileOutputStream fos = new FileOutputStream(destFile.getAbsoluteFile());
                BufferedOutputStream dest = new BufferedOutputStream(fos,
                        BUFFER);


                if(destFile.isFile() && (!destFile.getName().endsWith(".txt") && !destFile.getName().endsWith(".yaml") && !destFile.getName().endsWith(".meta"))){
                    Script script = new Script();
                    script.setName(destFile.getName());
                    script.setPayload(IOUtils.toByteArray(is));
                    vnfPackage.getScripts().add(script);
                }
                // read and write until last byte is encountered
                while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, currentByte);
                }

                dest.flush();
                dest.close();
                fos.close();
                is.close();

            }

        }

        if (fileList.contains("TOSCA-Metadata/TOSCA.meta"))
            log.debug("Found: /TOSCA-Metadata/TOSCA.meta");
        else
            throw new NotFoundException("In the csar file is missing the TOSCA-Metadata/TOSCA.meta");
//        log.debug(String.valueOf(fileList));

        FileInputStream fstream = new FileInputStream(newPath + "/TOSCA-Metadata/TOSCA.meta");
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;
        //Read File Line By Line
        while ((strLine = br.readLine()) != null) {
            // Print the content on the console

            String fileDefinition;
            String entryDefinition = "Entry-Definitions:";
            String author = "Created-By:";
            String version = "CSAR-Version:";
            if (strLine.contains(entryDefinition)) {
                fileDefinition = strLine.substring(entryDefinition.length(), strLine.length()).trim();


                if (!fileList.contains(fileDefinition))
                    throw new FileNotFoundException("Error not found the file:" + fileDefinition);


                this.definition = fromFileToDefinitions(newPath + '/' + fileDefinition);
//                ParserTosca parserTosca = new ParserTosca();
                NetworkServiceDescriptor nsd = parserTosca.getNetworkServiceDescriptor(this.definition);
//                craeteVNFPackages(nsd);
                vnfPackage.setName(nsd.getName());
                vnfPackageRepository.save(vnfPackage);

                for(VirtualNetworkFunctionDescriptor vnfd : nsd.getVnfd()){
                    vnfdRepository.save(vnfd);
                }

            }
            if (strLine.contains(author))
                this.author = strLine.substring(author.length(), strLine.length()).trim();
            if (strLine.contains(version))
                this.version = strLine.substring(version.length(), strLine.length()).trim();

        }
        in.close();


    }


    /**
     * Creates VNFPackages from the nsd ready for be stored into Openbaton
     * @param nsd
     * @return
     * @throws IOException
     */
    public Set<String> craeteVNFPackages(NetworkServiceDescriptor nsd) throws IOException {
        this.vnfPackagesPaths = new HashSet<>();
        for (VirtualNetworkFunctionDescriptor vnfd : nsd.getVnfd()) {
            Metadata metadata = new Metadata();
            metadata.setName(vnfd.getType());
            Image image = metadata.getImage();
            image.setUpload("false");
            Set<VirtualDeploymentUnit> vdus = vnfd.getVdu();
            for (VirtualDeploymentUnit vdu : vdus) {
                for (String imageString : vdu.getVm_image())
                    if (!image.getNames().contains(imageString))
                        image.getNames().add(imageString);
            }
            metadata.setImage(image);
//            image.setNames(((VirtualDeploymentUnit) vnfd.getVdu()).getVm_image());
//            FileOutputStream f = new FileOutputStream(this.pathUnzipFiles + '/' + vnfd.getType() + ".tar");
//            ZipOutputStream tar = new ZipOutputStream(new BufferedOutputStream(f));
//            tar.putNextEntry(new ZipEntry("scripts/"));


            Constructor constructor = new Constructor(Metadata.class);
            TypeDescription typeDescription = new TypeDescription(Metadata.class);
            constructor.addTypeDescription(typeDescription);
            Writer jsonWriter = new FileWriter(this.pathUnzipFiles + "/vndf.json");
            Gson gson = new GsonBuilder().create();
            gson.toJson(vnfd, jsonWriter);
            jsonWriter.close();
            Utils.addFileToFolder(this.pathUnzipFiles + "/vndf.json", vnfd.getName() + "/");
            Yaml yaml = new Yaml(constructor);
            Writer metadataFile = new FileWriter(this.pathUnzipFiles + "/Metadata.yaml");
            yaml.dump(metadata, metadataFile);
            metadataFile.close();


            Utils.addFileToFolder(this.pathUnzipFiles + "/Metadata.yaml", vnfd.getName() + "/");


            for (LifecycleEvent lifecycleEvents : vnfd.getLifecycle_event()) {
                for (String event : lifecycleEvents.getLifecycle_events()) {
                    Utils.addFileToFolder(this.pathUnzipFiles + "/" + event, vnfd.getName() + "/scripts/");
                    System.out.println(this.pathUnzipFiles + "/" + event);

                }
            }

            File directory = new File(vnfd.getName());
            File tar = new File(vnfd.getName() + ".tar");
            Utils.createTar(directory, tar);
//            log.debug(String.valueOf(this.vnfPackagesPaths));
            this.vnfPackagesPaths.add(tar.getAbsolutePath());

        }
        return this.vnfPackagesPaths;

    }


    public Definitions fromFileToDefinitions(String fileName) throws FileNotFoundException {
        InputStream tosca = new FileInputStream(new File(fileName));
        Constructor constructor = new Constructor(Definitions.class);
        TypeDescription projectDesc = new TypeDescription(Definitions.class);

//        projectDesc.putMapPropertyType("properties", String.class, PropertiesVnf.class);

        constructor.addTypeDescription(projectDesc);

        Yaml yaml = new Yaml(constructor);
        this.definition = yaml.loadAs(tosca, Definitions.class);
        return definition;
    }
}