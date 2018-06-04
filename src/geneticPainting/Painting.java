package geneticPainting;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import static geneticPainting.Main.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class Painting extends Pane{


    List<Polygon> polygons;
    List<PolygonDNA> genes;
    double fitness;
    AtomicBoolean fitnessCache;
    Image fitnessImage;


    public Painting() {
        super();
        Label label = new Label("hello");
        fitness = 0.0;
        fitnessCache = new AtomicBoolean(false);
        fitnessImage = null;
        //getChildren().add(label);
        polygons = new ArrayList<>();
        genes = new ArrayList<>();
        for (int i = 0; i < NUM_GENES; i++) {
            PolygonDNA dna = new PolygonDNA();
            genes.add(dna);
            polygons.add(dna.getPolygon());
        }
        //getChildren().addAll(polygons);
        Button button = new Button();
        button.setOnMouseClicked(x -> fitness(new Image("file:C:\\Users\\David\\OneDrive\\Workspace\\GeneticPainting\\src\\geneticPainting\\squares100.png")));

        Button mutate = new Button();
        mutate.setOnMouseClicked(x -> genes.stream().forEach(dna -> dna.mutate()));
        //getChildren().add(button);
       // getChildren().add(mutate);
        setMaxSize(WIDTH,HEIGHT);
        setPrefSize(WIDTH,HEIGHT);
        setMinSize(WIDTH,HEIGHT);
        refreshPolygons();
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    public List<PolygonDNA> getGenes() {
        return genes;
    }

    public Painting(List<PolygonDNA> genes, List<Polygon> polygons){
        this();
        this.genes = genes;
        this.polygons = polygons;
        refreshPolygons();
    }

    public void refreshPolygons(){
        if(Thread.currentThread().getName().equals("JavaFX Application Thread")) {
            getChildren().clear();
            getChildren().addAll(polygons);
            return;
        }
        
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {

                getChildren().clear();
                getChildren().addAll(polygons);

                return null;
            }
        };
        Platform.runLater(task);
        try {
            task.get(1,TimeUnit.MINUTES);
        } catch (Exception e) {

            System.out.println("Exception in refreshPolygons");
            System.out.println("refreshPolygons thread: "+ Thread.currentThread().getName());
            e.printStackTrace();
        }
    }

    public void mutate(){
        if(ThreadLocalRandom.current().nextDouble() > MUTATION_CHANCE){
            return;
        }

        List<Polygon> newPolygons = Collections.synchronizedList(new ArrayList<>());

        List<Set<Integer>> genesToFlip = Collections.synchronizedList(new ArrayList<>());
        for(int i = 0; i < NUM_GENES; i++){
            genesToFlip.add(Collections.synchronizedSet(new HashSet<>()));
        }
        for(int i = 0; i < MUTATION_AMOUNT; i++){
            int polygonIndex = ThreadLocalRandom.current().nextInt(NUM_GENES);
            int bitToFlip = ThreadLocalRandom.current().nextInt(PolygonDNA.dnasize);
            genesToFlip.get(polygonIndex).add(bitToFlip);
        }
        IntStream.range(0,NUM_GENES).parallel().forEach(polygonIndex -> {
            Set<Integer> bitsToFlip = genesToFlip.get(polygonIndex);
            if(bitsToFlip.size() > 0){
                genes.get(polygonIndex).flipBits(bitsToFlip);
            }
            newPolygons.add(genes.get(polygonIndex).getPolygon());
        });
        /*IntStream.range(0,genes.size()).parallel().forEach(z ->{
            PolygonDNA gene = genes.get(z);
            gene.mutate();
            newPolygons.add(gene.getPolygon());
        });*/
        polygons = newPolygons;
        fitnessCache.set(false);
        refreshPolygons();
    }


    public double fitness(Image image){
        if(fitnessCache.get() && fitnessImage == image){
            return fitness;
        }
        WritableImage writableImage = new WritableImage(WIDTH,HEIGHT);
        Task<WritableImage> getSnapshot = new Task<WritableImage>() {
            @Override
            protected WritableImage call() throws Exception {
            //long startTime = System.currentTimeMillis();
            refreshPolygons();
            //System.out.println("fitness after refresh time: " + (System.currentTimeMillis() - startTime));
            //startTime = System.currentTimeMillis();
            WritableImage snapshot = snapshot(null, writableImage);//params,null);
            //System.out.println("fitness after snapshot time: " + (System.currentTimeMillis() - startTime));
            return snapshot;
            }
        };
        Platform.runLater(getSnapshot);
        WritableImage polygonSnapshot = null;
        try {
            polygonSnapshot = getSnapshot.get(1, TimeUnit.MINUTES);
        }
        catch (Exception e){

            System.out.println("Error getting snapshot. thread: " + Thread.currentThread().getName());
            e.printStackTrace();
        }
        PixelReader polygon = polygonSnapshot.getPixelReader();
        PixelReader actual = image.getPixelReader();
        CompletableFuture<Double> completableFuture = CompletableFuture.supplyAsync(() ->
                IntStream.range(0, WIDTH).mapToDouble(z -> z).parallel().flatMap(x ->
                IntStream.range(0, HEIGHT).mapToDouble(z->z).parallel()
                        .map(y -> {
                            //System.out.println(Thread.currentThread().getName());
                            Color thisColor = polygon.getColor((int)x,(int)y);
                            Color otherColor = actual.getColor((int)x,(int)y);
                            double redDiff = Math.abs(thisColor.getRed() - otherColor.getRed())*255;
                            double blueDiff = Math.abs(thisColor.getBlue() - otherColor.getBlue())*255;
                            double greenDiff = Math.abs(thisColor.getGreen() - otherColor.getGreen())*255;
                            //double opacityDiff = Math.abs(thisColor.getOpacity() - otherColor.getOpacity())*255;
                            //System.out.println("this opacity: "+thisColor.getOpacity() + " other opacity: " + otherColor.getOpacity());
                            return ((redDiff + blueDiff + greenDiff));// + opacityDiff));
                                    //return Math.abs((int) ((long) polygon.getArgb((int)x, (int)y) - (long) actual.getArgb((int)x, (int)y)));
                            }
                        )).sum() / (WIDTH * HEIGHT * 3 * 255) * Integer.MAX_VALUE);
        fitness = completableFuture.join();
        fitnessCache.set(true);
        fitnessImage = image;
        return fitness;

    }

    public static Polygon getRandomPolygon(int x, int y){
        Polygon poly = new Polygon(getRandomPolygonPoints(x,y));

        poly.setFill(
                Paint.valueOf(
                        String.format("rgba(%d,%d,%d,%f)",
                            ThreadLocalRandom.current().nextInt(255),ThreadLocalRandom.current().nextInt(255),
                            ThreadLocalRandom.current().nextInt(255),ThreadLocalRandom.current().nextDouble())));

        return poly;
    }

    public static double[] getRandomPolygonPoints(int x, int y){
        double[] points = new double[6];
        for (int i = 0; i < points.length; i++) {
            if(i % 2 == 0){
                points[i] = ThreadLocalRandom.current().nextInt(x*100)/100.0;
            }
            else{
                points[i] = ThreadLocalRandom.current().nextInt(y*100)/100.0;
            }
        }
        return points;
    }

    public static Painting generateRandom(){
        List<PolygonDNA> genes = new ArrayList<>();
        List<Polygon> polygons = new ArrayList<>();
        for (int i = 0; i < NUM_GENES; i++) {
            PolygonDNA gene = new PolygonDNA();
            Polygon poly = gene.getPolygon();
            genes.add(gene);
            polygons.add(poly);
        }
        return new Painting(genes,polygons);
    }

    public Painting breed(Painting otherPainting){
        List<PolygonDNA> tempGenes = new ArrayList<>();
        List<Polygon> tempPolygons = new ArrayList<>();
        List<PolygonDNA> otherGenes = otherPainting.getGenes();
        for (int i = 0; i < genes.size(); i++) {
            if(ThreadLocalRandom.current().nextBoolean()){
                tempGenes.add(genes.get(i).clone());
            }
            else{
                tempGenes.add(otherGenes.get(i).clone());
            }
        }
        for(PolygonDNA gene: tempGenes){
            tempPolygons.add(gene.getPolygon());
        }
        return new Painting(tempGenes,tempPolygons);
    }

}
