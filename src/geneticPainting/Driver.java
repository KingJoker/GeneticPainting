package geneticPainting;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static geneticPainting.Main.*;


public class Driver extends GridPane {

    static final int ITERATIONS = Integer.MAX_VALUE;
    static final int rollingAverageSize = 50;
    static final int iterationsPerStat = 100;
    CanvasPainting best;
    Label bestFitness;
    Label elapsedTime;
    Image target;

    List<CanvasPainting> paintings;

    public Driver(){
        paintings = Collections.synchronizedList(new ArrayList<>());
        //generateRandomPopulation();
        int numGenes = NUM_GENES;
        bestFitness = new Label("% match =\n 0");
        elapsedTime = new Label("time elapsed:\n0");
    }

    public Driver(String imagePath){
        this();
        target = new Image(imagePath);
        generateRandomPopulation();
        setGridLinesVisible(true);
        ImageView imageView = new ImageView(target);
        //imageView.setScaleX(.5);
        //imageView.setScaleY(.5);
        //imageView.setScaleX(.5);
        //imageView.setScaleY(.5);

        add(imageView,0,0);
        add(bestFitness,2,0);
        add(elapsedTime,3,0);
        updateBest();


        for (int j = 0; j < APP_WIDTH/(WIDTH + 5); j++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100/(APP_WIDTH/(WIDTH +5)));
            cc.setHgrow(Priority.ALWAYS);
            getColumnConstraints().add(cc);
        }

        for (int j = 0; j < APP_HEIGHT/(HEIGHT+ 5) ; j++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100/(APP_HEIGHT/(HEIGHT +5)));
            rc.setVgrow(Priority.ALWAYS);
            getRowConstraints().add(rc);
        }
        Button start = new Button();
        start.setOnMouseClicked(x -> CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            double lastFitness=0;
            long lastTime = System.currentTimeMillis();
            
            List<Double> pastChanges = new ArrayList<>();
            List<Double> pastTimes = new ArrayList<>();
            for (int i = 0; i < ITERATIONS ; i++) {
                generateNextGeneration();
                updateBest();
                Platform.runLater(() -> {
                        bestFitness.setText("% match = \n"+
                                ((1-paintings.get(0).fitness(target)/Integer.MAX_VALUE))*100);
                        elapsedTime.setText("time elapsed:\n");
                });

                if(i % iterationsPerStat == 0){
                    Platform.runLater(this::updatePaintings);
                    long timeDiff = System.currentTimeMillis() - lastTime;
                    double currentFitness = paintings.get(0).fitness(target);
                    double changePerSecond = (((1-currentFitness/Integer.MAX_VALUE)*100) - ((1-lastFitness/Integer.MAX_VALUE)*100))/timeDiff*1000;
                    if(i > 0){
                        pastChanges.add(changePerSecond);
                        pastTimes.add((double)(timeDiff/iterationsPerStat));
                    }
                    if(pastChanges.size() > rollingAverageSize)
                        pastChanges.remove(0);
                    if(pastTimes.size() > rollingAverageSize){
                        pastTimes.remove(0);
                    }
                    double pastChangeAverage = pastChanges.parallelStream().mapToDouble(z -> z).sum() / pastChanges.size();
                    double pastTimeAverage = pastTimes.parallelStream().mapToDouble(z -> z).sum() / pastTimes.size();
                    System.out.println("");
                    System.out.println("Iteration: "+ i);
                    System.out.println("% fitness of best = " + ((1-currentFitness/Integer.MAX_VALUE)*100) + " changePerSecond: " + changePerSecond + " changeAverage: "+pastChangeAverage);
                    System.out.println("time per iteration: " + (timeDiff/iterationsPerStat)+ " average time per iteration: " + pastTimeAverage);

                    lastTime = System.currentTimeMillis();
                    lastFitness = currentFitness;
                }
            }
            System.out.println("time" + ((System.currentTimeMillis() - startTime) / ITERATIONS));

            //System.out.println(paintings.get(0).fitness(target));
            return true;

        }));
        add(start,1,0);
        Platform.runLater(()->updatePaintings());
    }

    public void updateBest(){
        Platform.runLater(() -> {
            getChildren().remove(best);
            best = new CanvasPainting(paintings.get(0).getGenes());
            add(best,4,0);
            setHgrow(best, Priority.NEVER);
            setVgrow(best, Priority.NEVER);
        });
    }

    public void updatePaintings(){
        getChildren().removeIf(x -> x instanceof CanvasPainting && x != best);
        int index = 0;
            for (int i = 1; i < APP_HEIGHT / (HEIGHT + 5) && index < paintings.size(); i++) {
                for (int j = 0; j < APP_WIDTH / (WIDTH + 5) && index < paintings.size(); j++) {

                    add(paintings.get(index), j, i);
                    setHgrow(paintings.get(index), Priority.NEVER);
                    setVgrow(paintings.get(index), Priority.NEVER);
                    index++;

                }
            }

        //System.out.println("best fitness = "+paintings.get(0).fitness(target));
    }


    private void generateNextGeneration(){
            List<CanvasPainting> nextGeneration = Collections.synchronizedList(new ArrayList<>());
        /*Task<Boolean> preSort = new Task<Boolean>(){
            @Override
            protected Boolean call() throws Exception {
                paintings.sort((x, y) -> (int)(x.fitness(target) - y.fitness(target)));
                return true;
            }
        };
        Platform.runLater(preSort);
        try {
            preSort.get();
        }
        catch(Exception e){
            System.out.println("Exception in generateNextGeneration pre-sort");
            e.printStackTrace();
        }*/
        /*Task<Void> preUpdate = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updatePaintings();
                return null;
            }
        };
        Platform.runLater(preUpdate);
        try {
            preUpdate.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
            IntStream.range(0, paintings.size()).parallel().forEach(z -> paintings.get(z).fitness(target));
            paintings.sort((x, y) -> (int) (x.fitness(target) - y.fitness(target)));

            for (int i = 0; i < (POPULATION_SIZE * SELECTION_CUTOFF); i++) {
                nextGeneration.add(paintings.get(i));
            }
        /*for(int i = 0; i < nextGeneration.size(); i++){
            nextGeneration.get(i).mutate();
        }*/
        /*IntStream.range(0,(int)(POPULATION_SIZE*SELECTION_CUTOFF)).parallel().
                forEach(i->IntStream.range(i+1,(int)(POPULATION_SIZE*SELECTION_CUTOFF)).parallel().
                        forEach(j -> {
                            Painting temp =  paintings.get(i).breed(paintings.get(j));
                            *//*Painting mutate = paintings.get(i).breed(paintings.get(j));
                            mutate.mutate();
                            nextGeneration.add(mutate);*//*
                            nextGeneration.add(temp);
                        } ));*/

        /*for (int i = (int) (POPULATION_SIZE * SELECTION_CUTOFF); i < paintings.size(); i++) {
            paintings.get(i).mutate();
        }*/


        /*while(nextGeneration.size() < paintings.size()){
        int numRandBreed = (int) Math.ceil(1/(.5-SELECTION_CUTOFF));
            for(int j = 0; j < (POPULATION_SIZE*SELECTION_CUTOFF) && nextGeneration.size() < paintings.size(); j++){
                //for(int i = 0; i < numRandBreed; i++)
                    nextGeneration.add(paintings.get(j).breed(paintings.get(ThreadLocalRandom.current().nextInt(paintings.size()))));
            }
        }*/
        IntStream.range(0,paintings.size() - nextGeneration.size()).parallel().
                forEach(j -> nextGeneration.
                        add(nextGeneration.get(j % ((int)(POPULATION_SIZE*SELECTION_CUTOFF)))
                        .breed(nextGeneration.get(ThreadLocalRandom.current().
                                nextInt((int)(POPULATION_SIZE*SELECTION_CUTOFF))))));

            /*for (int j = 0; j < (POPULATION_SIZE * SELECTION_CUTOFF); j++) {
                nextGeneration.add(paintings.get(j).breed(paintings.get(ThreadLocalRandom.current().nextInt(paintings.size()))));
            } */
            /*for (int i = (int) (POPULATION_SIZE * SELECTION_CUTOFF)+1; nextGeneration.size() < paintings.size(); i++) {
                nextGeneration.add(paintings.get(i));
            }*/
            /*for (int i = (int) (POPULATION_SIZE * SELECTION_CUTOFF); i < nextGeneration.size(); i++) {
                nextGeneration.get(i).mutate();
            }*/
       /* IntStream.range(nextGeneration.size(),POPULATION_SIZE).parallel().
                forEach(i ->{
                    nextGeneration.add(new Painting());
                });*/
        /*for (int i = 0; i < Math.sqrt(paintings.size()); i++) {
            for (int j = 0; j  < Math.sqrt(paintings.size()); j++) {
                Painting temp =  paintings.get(i).breed(paintings.get(j));
                Painting mutate = paintings.get(i).breed(paintings.get(j));
                mutate.mutate();
                nextGeneration.add(temp);
                nextGeneration.add(mutate);
            }
        }*/

        /*for(int i = 0; i < paintings.size(); i++){
            System.out.println(String.format("before index: %d: fitness: %f",i,paintings.get(i).fitness(target)));
        }*/

         IntStream.range((int)(POPULATION_SIZE * SELECTION_CUTOFF), nextGeneration.size()).parallel().
                forEach(i -> nextGeneration.get(i).mutate());



            IntStream.range(0, nextGeneration.size()).parallel().forEach(z -> nextGeneration.get(z).fitness(target));
            nextGeneration.sort((x, y) -> (int) (x.fitness(target) - y.fitness(target)));
        paintings = nextGeneration;
        //Platform.runLater(this::updatePaintings);
        /*Task<Void> postUpdate = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updatePaintings();
                return null;
            }
        };
        Platform.runLater(postUpdate);
        try {
            postUpdate.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
       /* Task<Boolean> postSort = new Task<Boolean>(){
            @Override
            protected Boolean call() throws Exception {
                paintings.sort((x, y) -> (int)x.fitness(target) - (int)y.fitness(target));
                return true;
            }
        };
        Platform.runLater(postSort);
        try {
            postSort.get();
        }
        catch(Exception e){
            System.out.println("Exception in generateNextGeneration post-sort");
            e.printStackTrace();
        }*/

        /*Platform.runLater(()->{
            //updatePaintings();
            try{
                //Thread.sleep(10);
            }
            catch(Exception e ){
                e.printStackTrace();
            }
            paintings.sort((x, y) -> (int)x.fitness(target) - (int)y.fitness(target));
        });*/
        /*for(int i = 0; i < paintings.size(); i++){
            final int j = i;
            Platform.runLater(()->System.out.println(String.format("index: %d: fitness: %f",j,paintings.get(j).fitness(target))));
        }*/

    }

    private void generateRandomPopulation(){
        for (int i = 0; i < POPULATION_SIZE; i++) {
            paintings.add(new CanvasPainting());
        }
        
    }

    //public
}


