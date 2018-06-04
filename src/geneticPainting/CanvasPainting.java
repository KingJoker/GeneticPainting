package geneticPainting;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static geneticPainting.Main.*;

public class CanvasPainting extends Canvas {

    List<PolygonDNA> genes;

    double fitness;
    AtomicBoolean fitnessCache;
    Image fitnessImage;

    public CanvasPainting(){
        super(WIDTH,HEIGHT);
        fitness = 0;
        init();
    }

    public CanvasPainting(List<PolygonDNA> genes){
        super(WIDTH,HEIGHT);
        this.genes = Collections.synchronizedList(new ArrayList<>(genes));

        fitnessCache=new AtomicBoolean(false);
        refreshPolygons();
    }

    public CanvasPainting(double width, double height){
        super(width,height);
        init();
    }

    public void init(){

        fitnessCache=new AtomicBoolean(false);
        genes = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < NUM_GENES; i++) {
            PolygonDNA dna = new PolygonDNA();
            genes.add(dna);
        }
        refreshPolygons();
    }

    public List<PolygonDNA> getGenes() {
        return genes;
    }


    public synchronized void refreshPolygons(){
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0,0,WIDTH,HEIGHT);
        for(int i = 0; i < NUM_GENES; i++){
            Polygon poly = genes.get(i).getPolygon();
            Double[] points = poly.getPoints().toArray(new Double[0]);

            double[] x = new double[3];
            double[] y = new double[3];
            for(int j = 0; j < 3; j++){
                x[j] = points[j];
            }
            for(int j = 0; j < 3; j++){
                y[j] = points[j+3];
            }
            //gc.setFill(poly.getFill());
            //gc.fillPolygon(x,y,3);

            gc.setFill(poly.getFill());
            gc.beginPath();
            gc.moveTo(points[0],points[1]);
            for(int j = 2; j < points.length; j+=2){
                gc.lineTo(points[j],points[j+1]);
            }
            gc.closePath();
            gc.fill();
        }
    }
    public  double fitness(Image image){
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
                if(snapshot == null){
                    System.out.println("Null Snapshot");
                }
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

    public CanvasPainting breed(CanvasPainting otherPainting){
        List<PolygonDNA> tempGenes = new ArrayList<>();
        List<PolygonDNA> otherGenes = otherPainting.getGenes();
        for (int i = 0; i < genes.size(); i++) {
            if(ThreadLocalRandom.current().nextBoolean()){
                tempGenes.add(genes.get(i).clone());
            }
            else{
                tempGenes.add(otherGenes.get(i).clone());
            }
        }
        return new CanvasPainting(tempGenes);
    }
    public void mutate(){
        if(ThreadLocalRandom.current().nextDouble() > MUTATION_CHANCE){
            return;
        }
        List<Set<Integer>> genesToFlip = Collections.synchronizedList(new ArrayList<>());
        for(int i = 0; i < NUM_GENES; i++){
            genesToFlip.add(Collections.synchronizedSet(new HashSet<>()));
        }
        int numberGenesToMutate = ThreadLocalRandom.current().nextInt((int)MUTATION_AMOUNT);
        for(int i = 0; i < numberGenesToMutate; i++){
            int polygonIndex = ThreadLocalRandom.current().nextInt(NUM_GENES);
            int bitToFlip = ThreadLocalRandom.current().nextInt(PolygonDNA.dnasize);
            genesToFlip.get(polygonIndex).add(bitToFlip);
        }
        IntStream.range(0,NUM_GENES).forEach(polygonIndex -> {
            Set<Integer> bitsToFlip = genesToFlip.get(polygonIndex);
            if(bitsToFlip.size() > 0){
                genes.get(polygonIndex).flipBits(bitsToFlip);
            }
        });
        fitnessCache.set(false);
        refreshPolygons();
    }
}
