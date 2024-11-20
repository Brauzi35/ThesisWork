package antiFrag.Position;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
public class FindPositionAndNeighbour {

    // I nodi forniti (esclusi dal calcolo del punto casuale)
    // I nodi forniti (esclusi dal calcolo del punto casuale)
    private static int seed = 1;
    private static List<Point2D> nodes = Arrays.asList(
            new Point2D.Double(378, 193),  // 2
            new Point2D.Double(365, 343),  // 3
            new Point2D.Double(508, 296),  // 4
            new Point2D.Double(603, 440),  // 5
            new Point2D.Double(628, 309),  // 6
            new Point2D.Double(324, 273),  // 7
            new Point2D.Double(392, 478),  // 8
            new Point2D.Double(540, 479),  // 9
            new Point2D.Double(694, 356),  // 10
            new Point2D.Double(234, 232),  // 11
            new Point2D.Double(221, 332),  // 12
            new Point2D.Double(142, 170),  // 13
            new Point2D.Double(139, 293),  // 14
            new Point2D.Double(128, 344)   // 15
    );

    // Genera un punto casuale all'interno del poligono
    public static Point2D generateRandomPointInPolygon(Path2D.Double polygon) {
        Random rand = new Random(seed);
        double minX = polygon.getBounds2D().getMinX();
        double maxX = polygon.getBounds2D().getMaxX();
        double minY = polygon.getBounds2D().getMinY();
        double maxY = polygon.getBounds2D().getMaxY();

        Point2D point;
        do {
            double x = minX + (maxX - minX) * rand.nextDouble();
            double y = minY + (maxY - minY) * rand.nextDouble();
            point = new Point2D.Double(x, y);
        } while (!polygon.contains(point));  // Assicura che il punto sia all'interno del poligono
        seed++;
        return point;
    }

    // Trova il nodo più vicino al punto dato
    public static int findClosestNode(Point2D point) {
        double minDistance = Double.MAX_VALUE;
        int closestNodeId = -1;

        for (int i = 0; i < nodes.size(); i++) {
            Point2D node = nodes.get(i);
            double distance = point.distance(node);  // Distanza euclidea
            if (distance < minDistance) {
                minDistance = distance;
                closestNodeId = i + 2;  // ID del nodo (2 a 15)
            }
        }

        return closestNodeId;
    }

    // Genera un punto casuale vicino ai nodi definiti
    public static Point2D getPosition() {
        Random rand = new Random(seed);

        // Seleziona un nodo casuale dalla lista
        Point2D baseNode = nodes.get(rand.nextInt(nodes.size()));

        // Genera un offset casuale vicino al nodo
        double offsetX = (rand.nextDouble() - 0.5) * 100; // Range di deviazione di ±50 unità
        double offsetY = (rand.nextDouble() - 0.5) * 100;

        // Calcola la nuova posizione attorno al nodo selezionato
        double newX = baseNode.getX() + offsetX;
        double newY = baseNode.getY() + offsetY;

        // Incrementa il seed per garantire casualità tra chiamate successive
        seed++;

        return new Point2D.Double(newX, newY);
    }






}
