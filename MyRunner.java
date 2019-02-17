package com.ipiecoles.java.java230;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    
    private boolean checkedNumField = false;
    private String checkedMat = null;
    private LocalDate dateEmbauche = null;
    private Double checkedSalaire = null;
    private int checkedGrade = 0;
    private Double checkedChiffreAff = null;
    private int checkedPerf = 0;
    private String checkedManager = null;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings){
        String fileName = "employes.csv";
        readFile(fileName);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être lu
     * @throws IOException 
     */
    public List<Employe> readFile(String fileName) {
        Stream<String> stream = null;
        try {
        	stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        	logger.info("lecture du fichier "+ fileName);
        }
        catch(IOException e) {
        	logger.error("le fichier" + fileName + "n'existe pas!");
        	return null;
        }
        
        List<String> lignes = stream.collect(Collectors.toList());
        for(int i=0; i <lignes.size(); i++) {
        	logger.info("lecture ligne "+ (i+1));
        	try {
        		processLine(lignes.get(i));
        	} catch(BatchException e){
        		logger.error("Ligne" + (i+1) + " : " + e.getMessage());
        	}
        }
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
    	switch(ligne.substring(0, 1)) {
    	case "T":
    		processTechnicien(ligne);
    		break;
    	case "M":
    		processManager(ligne);
    		break;
    	case "C":
    		processCommercial(ligne);
    		break;
    	default:
    		throw new BatchException("Type d'employé inconnu:" + ligne.substring(0,1));
    	}
    }

    /**
     * Méthode qui contrôle le format du champs Date et renvoie la valeur si le format attendu est respecté 
     * @param date valeur de champs date à contrôler
     * @throws BatchException
     */
    private LocalDate controlDate(String date) throws BatchException {    	
    	try {
            LocalDate d = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(date);
            return d;
    	}catch (Exception e) {
    		throw new BatchException("La date " + date + " n'est pas au bon format");
    	}
    }
    
    /**
     * Méthode de contrôle sur les matricules d'employé renvoie la valeur du matricule uniquement si le format de matricule est respecté
     * @param value valeur de champs matricule à contrôler
     * @param regex expression régulière utilisée pour contrôler le format de matricule
     * @return
     * @throws BatchException
     */
    private String controlStringRegex(String value, String regex) throws BatchException{
    	if (!value.matches(regex)){
    		throw new BatchException("la chaîne "+ value +" ne respecte pas l'expression régulière " + regex);
    	}else{
            return value;
        }
    }
    
    /**Méthode de contrôle sur le nombre de champs pour manager ou commercial.
     * 
     * @param value tableau a controler
     * @param toCompare valeur attendue pour le nombre de champs
     * @param target type de ligne a controler. Manger ou commercial
     * @return
     * @throws BatchException
     */
    private boolean controlNumberField(String[] value, int toCompare, String target) throws BatchException{
    	if(value.length != toCompare) {
    		throw new BatchException("La ligne "+ target +" ne contient pas "+ toCompare + " éléments mais " + value.length);
    	}else{
            return true;
        }
    }
    
    /** Méthode de contrôle pour vérifier le respect du format pour les valeurs décimales
     * 
     * @param value valeur a tester
     * @param target champs à contrôler. Dans ce contexte, le controleur est valable pour le salaire et le chiffre d'affaire
     * @return
     * @throws BatchException
     */
    private Double controlDouble(String value, String target) throws BatchException{
        try{
            Double toCompare = Double.parseDouble(value); 
            return toCompare;
        }
        catch (Exception e) {
            throw new BatchException(value + " n'est pas un nombre valide pour un " + target);
        }
    }
    
    /** Méthode similaire à la précédente pour les valeurs entières
     * 
     * @param value
     * @param target
     * @return
     * @throws BatchException
     */
    private int controlInt(String value, String target) throws BatchException{
        try{
            int toCompare = Integer.parseInt(value); 
            return toCompare;
        }
        catch (Exception e) {
            throw new BatchException(value + " n'est pas un nombre valide pour " + target);
        }
    }
    
    
    /**Méthode de contrôle pour la vérification de la valeur grade qui doit être de type entier et comprise entre 1 et 5
     * 
     * @param value
     * @return
     * @throws BatchException
     */
    private int controlGrade(String value) throws BatchException{
    	
    	int toCompare;
    	
    	try{
        	toCompare = Integer.parseInt(value);	
        }
        catch (Exception e){
        	throw new BatchException(value + " n'est pas un nombre valide pour un grade");
        }
        
        if(toCompare < 1 || toCompare > 5) {
    		throw new BatchException("Le grade doit être compris entre 1 et 5 : "+ toCompare);
    	}else {
    		return toCompare;
    	}	
    }
    
    /**Méthode qui contrôle la présence en base de donnée du matricule d'un manager pour un technicien donné
     * 
     * @param matManager
     * @return
     * @throws BatchException
     */
    private String controlManager(String matManager) throws BatchException{
    	
    	try {
    		Manager existManager = managerRepository.findByMatricule(matManager);
    		return existManager.getMatricule();
    	}
    	catch (Exception e) {
    		throw new BatchException("Le manager de matricule " + matManager + " n'a pas été trouvé dans le fichier ou en base de données");
    	}
    }
    
    
    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException{
        //TODO
    	String[] champs = ligneCommercial.split(",");
	
		this.checkedNumField = controlNumberField(champs, NB_CHAMPS_COMMERCIAL, "commercial");
    	this.checkedMat =  controlStringRegex(champs[0], REGEX_MATRICULE);
        this.dateEmbauche = controlDate(champs[3]);
        this.checkedSalaire = controlDouble(champs[4], "salaire");
        this.checkedChiffreAff = controlDouble(champs[5], "chiffre d'affaire");
        this.checkedPerf = controlInt(champs[6], "Performance");
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        //TODO
    	String[] champs = ligneManager.split(",");
    		
    	this.checkedNumField = controlNumberField(champs, NB_CHAMPS_MANAGER, "manager");
    	this.checkedMat =  controlStringRegex(champs[0], REGEX_MATRICULE_MANAGER);
        this.dateEmbauche = controlDate(champs[3]);
        this.checkedSalaire = controlDouble(champs[4], "salaire");
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        //TODO
    	String[] champs = ligneTechnicien.split(",");
        
        this.checkedNumField = controlNumberField(champs, NB_CHAMPS_TECHNICIEN, "technicien");
    	this.checkedMat =  controlStringRegex(champs[0], REGEX_MATRICULE);
        this.dateEmbauche = controlDate(champs[3]);
        this.checkedSalaire = controlDouble(champs[4],"salaire");
        this.checkedGrade = controlGrade(champs[5]);
        this.checkedManager = controlManager(champs[6]);
    }
}
