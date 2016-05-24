package com.dglab.cia.levelcreator;

import kn.uni.voronoitreemap.j2d.Point2D;

import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) 2007 Alexander Hristov.
 * http://www.ahristov.com
 *
 * Feel free to use this code as you wish, as long as you keep this copyright
 * notice. The only limitation on use is that this code cannot be republished
 * on other web sites.
 *
 * As usual, this code comes with no warranties of any kind.
 *
 *
 */
public class QuickHull {
    public static List<Point2D> quickHull(List<Point2D> points) {
        ArrayList<Point2D> convexHull = new ArrayList<>();
        if (points.size() < 3) return points;
        // find extremals
        int minPoint2D = -1, maxPoint2D = -1;
        double minX = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).x < minX) {
                minX = points.get(i).x;
                minPoint2D = i;
            }
            if (points.get(i).x > maxX) {
                maxX = points.get(i).x;
                maxPoint2D = i;
            }
        }
        Point2D A = points.get(minPoint2D);
        Point2D B = points.get(maxPoint2D);
        convexHull.add(A);
        convexHull.add(B);
        points.remove(A);
        points.remove(B);

        ArrayList<Point2D> leftSet = new ArrayList<Point2D>();
        ArrayList<Point2D> rightSet = new ArrayList<Point2D>();

        for (int i = 0; i < points.size(); i++) {
            Point2D p = points.get(i);
            if (pointLocation(A, B, p) == -1)
                leftSet.add(p);
            else
                rightSet.add(p);
        }
        hullSet(A, B, rightSet, convexHull);
        hullSet(B, A, leftSet, convexHull);

        return convexHull;
    }

    /*
     * Computes the square of the distance of point C to the segment defined by points AB
     */
    public static double distance(Point2D A, Point2D B, Point2D C) {
        double ABx = B.x - A.x;
        double ABy = B.y - A.y;
        double num = ABx * (A.y - C.y) - ABy * (A.x - C.x);
        if (num < 0) num = -num;
        return num;
    }

    public static void hullSet(Point2D A, Point2D B, ArrayList<Point2D> set, ArrayList<Point2D> hull) {
        int insertPosition = hull.indexOf(B);
        if (set.size() == 0) return;
        if (set.size() == 1) {
            Point2D p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Integer.MIN_VALUE;
        int furthestPoint2D = -1;
        for (int i = 0; i < set.size(); i++) {
            Point2D p = set.get(i);
            double distance = distance(A, B, p);
            if (distance > dist) {
                dist = distance;
                furthestPoint2D = i;
            }
        }
        Point2D P = set.get(furthestPoint2D);
        set.remove(furthestPoint2D);
        hull.add(insertPosition, P);

        // Determine who's to the left of AP
        ArrayList<Point2D> leftSetAP = new ArrayList<Point2D>();
        for (int i = 0; i < set.size(); i++) {
            Point2D M = set.get(i);
            if (pointLocation(A, P, M) == 1) {
                leftSetAP.add(M);
            }
        }

        // Determine who's to the left of PB
        ArrayList<Point2D> leftSetPB = new ArrayList<Point2D>();
        for (int i = 0; i < set.size(); i++) {
            Point2D M = set.get(i);
            if (pointLocation(P, B, M) == 1) {
                leftSetPB.add(M);
            }
        }
        hullSet(A, P, leftSetAP, hull);
        hullSet(P, B, leftSetPB, hull);

    }

    public static double pointLocation(Point2D A, Point2D B, Point2D P) {
        double cp1 = (B.x - A.x) * (P.y - A.y) - (B.y - A.y) * (P.x - A.x);
        return (cp1 > 0) ? 1 : -1;
    }

    public static ArrayList<Point2D> ConvexHull_JarvisMarch(List<Point2D> points) {

        int crnt, next = -1, root = -1;
        Vector a = new Vector();
        Vector b = new Vector();
        //boolean[] vpoints = new boolean[points.size()];

        //arraylist of type 'Line' holding alllines of Convex hull
        ArrayList<Point2D> ConvexPoint2Ds = new ArrayList<Point2D>();

        if (points.size() <= 0)
            return ConvexPoint2Ds;

        //choose point with lowest X-value.
        crnt = 0;
        for (int i = 0; i < points.size(); i++)
            if (points.get(i).x < points.get(crnt).x || (points.get(i).x == points.get(crnt).x && points.get(i).y < points.get(crnt).y))
                crnt = i;

        //add first point in convex hull.
        ConvexPoint2Ds.add(points.get(crnt));
        root = crnt;

        while (true) {
            next = -1;
            for (int i = 0; i < points.size(); i++) {
                if (i == crnt)
                    continue;

                if (next == -1) {
                    next = i;
                    a.Set(points.get(next), points.get(crnt));
                    continue;
                }


                b.Set(points.get(i), points.get(crnt));

                if (Vector.ToRight(a, b)) {
                    next = i;
                    a.Set(points.get(next), points.get(crnt));
                }
            }

            //break condition when no next point to go to.
            if (next == -1 || next == root)
                break;

            //vpoints[next] = true;
            ConvexPoint2Ds.add(points.get(next));
            crnt = next;
        }


        return ConvexPoint2Ds;
    }

    private static class Vector {

        /* data members */
        public double X,Y,Z;

        public Vector()
        {}
        public Vector(double x,double y,double z)
        {
            this.X = x;
            this.Y = y;
            this.Z = z;
        }

        //Vector norm
        public double norm()
        {
            return Math.sqrt(X*X + Y*Y +Z*Z);
        }

        public void Set(Point2D p1,Point2D p2)
        {
            X = p1.x - p2.x;
            Y = p1.y - p2.y;
            Z = 0;
        }

  /* Static Functions */

        //Add two Vectors
        static public Vector Add(Vector a,Vector b)
        {
            return new Vector(a.X + b.X , a.Y + b.Y, a.Z + b.Z);
        }

        //Dot Product two Vectors
        static public double DotProduct(Vector a,Vector b)
        {
            return ( a.X*b.X + a.Y+b.Y + a.Z*b.Z);
        }

        // A ⋅ B = |A||B|Cos(θ)
        static public double AngleBetween(Vector a,Vector b)
        {
            return Math.acos( DotProduct(a,b)/( a.norm() * b.norm() ));
        }

        //Cross Product two Vectors
        static public Vector CrossProduct(Vector a , Vector b)
        {
            return new Vector( (a.Y * b.Z) - (b.Y * a.Z)
                    ,(a.Z * b.X) - (b.Z * a.X)
                    ,(a.X * b.Y) - (b.X * a.Y));
        }

        //In 2-D geometry means that if A is less than 180 degrees clockwise from B, the Theta value is positive.
        static public Boolean ToRight(Vector a , Vector b)
        {
            return ( CrossProduct(a,b).Z > 0 );
        }

        //In 2-D geometry to get angle of vector
        public double Angle()
        {
            double angle = Math.toDegrees(Math.atan2(Y,X));

            if(angle<0)
                angle+=360;

            return angle;


        }

    }
}
