package com.jobapp.ml;

import opennlp.tools.doccat.*;
import opennlp.tools.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ModelTrainer {

    public static void main(String[] args) {
        try {
            // --- CORRECTION : CHEMIN DU FICHIER ---
            // On cherche le fichier à la racine du projet (au même niveau que pom.xml)
            File trainingFile = new File("training_data.txt");

            System.out.println("📂 Recherche du fichier : " + trainingFile.getAbsolutePath());

            if (!trainingFile.exists()) {
                System.err.println("❌ ERREUR : Le fichier 'training_data.txt' est introuvable !");
                System.err.println("👉 Assurez-vous qu'il est bien à la racine du projet (JobHunterr).");
                return;
            }

            // 1. Lecture des données
            InputStreamFactory dataIn = new MarkableFileInputStreamFactory(trainingFile);
            ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, StandardCharsets.UTF_8);
            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

            // 2. Configuration de l'entraînement
            DoccatFactory factory = new DoccatFactory();
            TrainingParameters params = TrainingParameters.defaultParams();
            params.put(TrainingParameters.ITERATIONS_PARAM, 100);
            params.put(TrainingParameters.CUTOFF_PARAM, 0);

            // 3. Lancer l'apprentissage
            System.out.println("⏳ Entraînement du modèle en cours...");
            DoccatModel model = DocumentCategorizerME.train("fr", sampleStream, params, factory);

            // 4. Sauvegarde du modèle généré (.bin)
            File modelFile = new File("recruitment_model.bin");
            try (BufferedOutputStream modelOut = new BufferedOutputStream(new FileOutputStream(modelFile))) {
                model.serialize(modelOut);
            }

            System.out.println("✅ SUCCÈS ! Modèle généré : " + modelFile.getAbsolutePath());
            System.out.println("👉 Vous pouvez maintenant lancer MainGui !");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}