/**
 * Licensed under the EUPL-1.2-or-later.
 * Copyright (c) 2023, gridDigIt Kft. All rights reserved.
 * @author Chavdar Ivanov
 */

package core;

import application.MainController;
import customWriter.CustomRDFFormat;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.vocabulary.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class InstanceDataFactory {

    public static Map<Property,Map> shaclConstraints;


    //Loads one or many models
    public static Map<String,Model> modelLoad(List<File> files, String xmlBase, Lang rdfSourceFormat) throws FileNotFoundException {

        Map<String,Model> unionModelMap=new HashMap<>();
        Model modelUnion = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        Model modelUnionWithoutHeader = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        Map prefixMap = modelUnion.getNsPrefixMap();
        Map prefixMapWithoutHeader = modelUnionWithoutHeader.getNsPrefixMap();
        for (Object file : files) {
            if (rdfSourceFormat==null){
                String extension = FilenameUtils.getExtension(file.toString());
                if (extension.equals("rdf") || extension.equals("xml")){
                    rdfSourceFormat=Lang.RDFXML;
                }else if(extension.equals("ttl")){
                    rdfSourceFormat=Lang.TURTLE;
                }else if(extension.equals("jsonld")){
                    rdfSourceFormat=Lang.JSONLD;
                }
            }
            Model model = org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
            InputStream inputStream = new FileInputStream(file.toString());
            RDFDataMgr.read(model, inputStream, xmlBase, rdfSourceFormat);
            prefixMap.putAll(model.getNsPrefixMap());
            prefixMapWithoutHeader.putAll(model.getNsPrefixMap());

            //get profile short name for CGMES v2.4, keyword for CGMES v3
            String keyword=getProfileKeyword(model);
            if (FilenameUtils.getName(file.toString()).equals("FileHeader.rdf")){
                keyword="FH";
            }
            if (!keyword.equals("")) {
                unionModelMap.put(keyword, model);
            }else{
                unionModelMap.put(FilenameUtils.getName(file.toString()), model);
            }

            if (!keyword.equals("FH")) {
                modelUnionWithoutHeader.add(model);
            }
            modelUnion.add(model);
        }
        modelUnion.setNsPrefixes(prefixMap);
        modelUnionWithoutHeader.setNsPrefixes(prefixMap);


        unionModelMap.put("unionModel",modelUnion);
        unionModelMap.put("modelUnionWithoutHeader",modelUnionWithoutHeader);
        return unionModelMap;

    }

    //get the keyword for the profile
    static String getProfileKeyword(Model model) {

        String keyword="";


        if (model.listObjectsOfProperty(DCAT.keyword).hasNext()){
            keyword=model.listObjectsOfProperty(DCAT.keyword).next().toString();
        }

        if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#EquipmentVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))){
            keyword=model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#EquipmentVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#EquipmentBoundaryVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#EquipmentBoundaryVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
            keyword="EQBD";
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#TopologyBoundaryVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#TopologyBoundaryVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
            //TODO maybe fix RDFS. Here a quick override
            keyword="TPBD";
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#TopologyVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#TopologyVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#SteadyStateHypothesisVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#SteadyStateHypothesisVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#StateVariablesVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#StateVariablesVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#EDynamicsVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#EDynamicsVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#GeographicalLocationVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#GeographicalLocationVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#DiagramLayoutVersion.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#DiagramLayoutVersion.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.contains(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#Ontology.shortName"),
                ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed"))) {
            keyword = model.getRequiredProperty(ResourceFactory.createResource("http://entsoe.eu/CIM/SchemaExtension/3/1#Ontology.shortName"),
                    ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().toString();
        }else if (model.listObjectsOfProperty(ResourceFactory.createProperty("http://iec.ch/TC57/61970-552/ModelDescription/1#Model.profile")).hasNext()){
            List<RDFNode> profileString=model.listObjectsOfProperty(ResourceFactory.createProperty("http://iec.ch/TC57/61970-552/ModelDescription/1#Model.profile")).toList();
            for (RDFNode node: profileString){
                String nodeString=node.toString();
                if (nodeString.equals("http://entsoe.eu/CIM/EquipmentCore/3/1") || nodeString.equals("http://entsoe.eu/CIM/EquipmentOperation/3/1") || nodeString.equals("http://entsoe.eu/CIM/EquipmentShortCircuit/3/1")){
                    keyword="EQ";
                }else if (nodeString.equals("http://entsoe.eu/CIM/SteadyStateHypothesis/1/1")){
                    keyword="SSH";
                }else if (nodeString.equals("http://entsoe.eu/CIM/Topology/4/1")) {
                    keyword = "TP";
                }else if (nodeString.equals("http://entsoe.eu/CIM/StateVariables/4/1")) {
                    keyword = "SV";
                }else if (nodeString.equals("http://entsoe.eu/CIM/EquipmentBoundary/3/1") || nodeString.equals("http://entsoe.eu/CIM/EquipmentBoundaryOperation/3/1")) {
                    keyword = "EQBD";
                }else if (nodeString.equals("http://entsoe.eu/CIM/TopologyBoundary/3/1")) {
                    keyword = "TPBD";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/CoreEquipment-EU/3.0")) {
                    keyword = "EQ";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/Operation-EU/3.0")) {
                    keyword = "OP";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/ShortCircuit-EU/3.0")) {
                    keyword = "SC";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/SteadyStateHypothesis-EU/3.0")){
                    keyword="SSH";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/Topology-EU/3.0")) {
                    keyword = "TP";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/StateVariables-EU/3.0")) {
                    keyword = "SV";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/EquipmentBoundary-EU/3.0")) {
                    keyword = "EQBD";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/DiagramLayout-EU/3.0")) {
                    keyword = "DL";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/GeographicalLocation-EU/3.0")) {
                    keyword = "GL";
                }else if (nodeString.equals("http://iec.ch/TC57/ns/CIM/Dynamics-EU/1.0")) {
                    keyword = "DY";
                }
            }
        }

        if (keyword.equals("")){
            List<Resource> listRes=model.listSubjectsWithProperty(RDF.type).toList();
            for (Resource res : listRes){
                if (res.getLocalName().contains("Ontology.keyword")){
                    keyword = model.getRequiredProperty(res,ResourceFactory.createProperty("http://iec.ch/TC57/1999/rdf-schema-extensions-19990926#isFixed")).getObject().asLiteral().getString();
                }
            }
        }

        return keyword;
    }

    //save instance data
    public static void saveInstanceData(Model instanceDataModel, Map<String,Object> saveProperties) throws IOException {
        //register custom format
        CustomRDFFormat.RegisterCustomFormatWriters();

        String filename=saveProperties.get("filename").toString().toUpperCase();
        filename=filename.replace(".XML", ".xml"); //TODO make this more intelligent and bring it to GUI
        String showXmlDeclaration=saveProperties.get("showXmlDeclaration").toString();
        String showDoctypeDeclaration=saveProperties.get("showDoctypeDeclaration").toString();
        String tab=saveProperties.get("tab").toString();
        String relativeURIs=saveProperties.get("relativeURIs").toString();
        String showXmlEncoding=saveProperties.get("showXmlEncoding").toString();
        String xmlBase=saveProperties.get("xmlBase").toString();
        RDFFormat rdfFormat=(RDFFormat) saveProperties.get("rdfFormat");
        boolean useAboutRules = (boolean) saveProperties.get("useAboutRules");   //switch to trigger file chooser and adding the property
        boolean useEnumRules = (boolean) saveProperties.get("useEnumRules");   //switch to trigger special treatment when Enum is referenced
        boolean useFileDialog=(boolean) saveProperties.get("useFileDialog");
        String instanceData = saveProperties.get("instanceData").toString();
        String fileFolder=saveProperties.get("fileFolder").toString();
        boolean dozip=(boolean) saveProperties.get("dozip");
        String showXmlBaseDeclaration = saveProperties.get("showXmlBaseDeclaration").toString();

        //Set<Resource> rdfAboutList = null;
        //Set<Resource> rdfEnumList = null;
        Set<Resource> rdfAboutList = (Set<Resource>) saveProperties.get("rdfAboutList");
        Set<Resource> rdfEnumList = (Set<Resource>) saveProperties.get("rdfEnumList");
        boolean putHeaderOnTop = (boolean) saveProperties.get("putHeaderOnTop");
        String headerClassResource=saveProperties.get("headerClassResource").toString();
        String extensionName=saveProperties.get("extensionName").toString();
        String fileExtension=saveProperties.get("fileExtension").toString();
        String fileDialogTitle=saveProperties.get("fileDialogTitle").toString();

        //save file
        OutputStream outXML=null;
        ZipOutputStream outzip=null;

        if(useFileDialog) {
            outXML = fileSaveDialog(fileDialogTitle, filename, extensionName, fileExtension);
        }else{
            outXML = new FileOutputStream(fileFolder+"\\"+filename);

        }
        if (outXML!=null) {
            try {
                if (rdfFormat == CustomRDFFormat.RDFXML_CUSTOM_PLAIN_PRETTY || rdfFormat == CustomRDFFormat.RDFXML_CUSTOM_PLAIN) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("showXmlDeclaration", showXmlDeclaration);
                    properties.put("showDoctypeDeclaration", showDoctypeDeclaration);
                    properties.put("showXmlEncoding", showXmlEncoding); // works only with the custom format
                    //properties.put("blockRules", "daml:collection,parseTypeLiteralPropertyElt,"
                    //        +"parseTypeResourcePropertyElt,parseTypeCollectionPropertyElt"
                    //        +"sectionReification,sectionListExpand,idAttr,propertyAttr"); //???? not sure
                    if (putHeaderOnTop) {
                        properties.put("prettyTypes", new Resource[]{ResourceFactory.createResource(headerClassResource)});
                    }
                    properties.put("xmlbase", xmlBase);
                    properties.put("tab", tab);
                    properties.put("relativeURIs", relativeURIs);
                    properties.put("instanceData", instanceData);
                    properties.put("showXmlBaseDeclaration", showXmlBaseDeclaration);

                    if (useAboutRules) {
                        properties.put("aboutRules", rdfAboutList);
                    }

                    if (useEnumRules) {
                        properties.put("enumRules", rdfEnumList);
                    }


                    // Put a properties object into the Context.
                    Context cxt = new Context();
                    cxt.set(SysRIOT.sysRdfWriterProperties, properties);


                    org.apache.jena.riot.RDFWriter.create()
                            .base(xmlBase)
                            .format(rdfFormat)
                            .context(cxt)
                            .source(instanceDataModel)
                            .output(outXML);

                } else {
                    instanceDataModel.write(outXML, rdfFormat.getLang().getLabel().toUpperCase(), xmlBase);
                }
            } finally {
                outXML.flush();
                outXML.close();


            }
            if (dozip) {
                String sourceFile = fileFolder + "\\" + filename;
                FileOutputStream fos = new FileOutputStream(fileFolder + "\\" + filename.replace(".xml", ".zip"));
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                File fileToZip = new File(sourceFile);
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                //zipEntry.setMethod(ZipEntry.STORED); // no compression, deflated - with
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                zipOut.close();
                fis.close();
                fos.close();
                //delete the xml
                File xmlTodelete = new File(fileFolder + "\\" + filename);
                xmlTodelete.delete();
            }
        }


    }

    //File save dialog
    private static OutputStream fileSaveDialog(String title, String filename, String extensionName, String extension) throws FileNotFoundException {
        File saveFile;
        FileChooser filechooserS = new FileChooser();
        filechooserS.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(extensionName, extension));
        filechooserS.setInitialFileName(filename);
        filechooserS.setInitialDirectory(new File(MainController.prefs.get("LastWorkingFolder","")));
        filechooserS.setTitle(title);
        saveFile = filechooserS.showSaveDialog(null);
        OutputStream out=null;
        if (saveFile!=null) {
            MainController.prefs.put("LastWorkingFolder", saveFile.getParent());
            out = new FileOutputStream(saveFile);
        }
        return out;
    }

    public static InputStream unzip(File selectedFile) {
        InputStream inputStream = null;
        try{
            ZipFile zipFile = new ZipFile(selectedFile);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();



            while(entries.hasMoreElements()){
                ZipEntry entry = entries.nextElement();
                if(entry.isDirectory()){
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Selected zip file contains folder. This is a violation of data exchange standard.");
                    alert.setHeaderText(null);
                    alert.setTitle("Error - violation of a zip file packaging requirement.");
                    alert.showAndWait();
                } else {
                    String destPath = selectedFile.getParent() + File.separator+ entry.getName();

                    if(! isValidDestPath(selectedFile.getParent(), destPath)){
                        throw new IOException("Final file output path is invalid: " + destPath);
                    }

                    try{
                        inputStream = zipFile.getInputStream(entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch(IOException e){
            throw new RuntimeException("Error unzipping file " + selectedFile, e);
        }
        return inputStream;
    }

    private static boolean isValidDestPath(String targetDir, String destPathStr) {
        // validate the destination path of a ZipFile entry,
        // and return true or false telling if it's valid or not.

        Path destPath           = Paths.get(destPathStr);
        Path destPathNormalized = destPath.normalize(); //remove ../../ etc.

        return destPathNormalized.toString().startsWith(targetDir + File.separator);
    }

}

