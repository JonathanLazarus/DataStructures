package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class MinHeapImplTest {
    private static int productNumberCount = 0;
    private MinHeapImpl<Car> heap;
    private Car[] cars;
    private Car ford;
    private Car lincoln;
    private Car toyota;
    private Car jeep;
    private Car honda;


    public MinHeapImplTest() {
        this.heap = new MinHeapImpl<>();
        ford = new Car("Mustang GT", 1967);
        lincoln = new Car("name", 1995);
        toyota = new Car("Camry", 2004);
        jeep = new Car("Wrangler", 2014);
        honda = new Car("Civic", 2020);
        this.cars = new Car[]{ford, lincoln, toyota, jeep, honda};
    }

    @Before
    public void setup() {
        this.heap = new MinHeapImpl<>();
        this.ford.setYear(1967);
        this.lincoln.setYear(1995);
        this.toyota.setYear(2004);
        this.jeep.setYear(2014);
        this.honda.setYear(2020);
        for (Car c : this.cars) {
            this.heap.insert(c);
        }
    }

    @Test
    public void deleteindex4() {
        //honda is in array 5
        this.jeep.setYear(Integer.MIN_VALUE);
        //delete it now that it is the min
        this.heap.reHeapify(jeep);
        assertTrue(this.heap.removeMin().equals(jeep));
        //manually check that the indexes of the other cars were updated acccordingly (bec of the reheapify)
        HashMap<Car, Integer> hm = this.heap.getMap();
        for (Car c : hm.keySet()) {
            System.out.printf("%-12s%-10d%s%d%n", c.model, c.year, "index:", hm.get(c));
        }
    }

    @Test
    public void reHeapify() {
        assertEquals(2, this.heap.getArrayIndex(lincoln));
        lincoln.setYear(1965);
        assertEquals(2, this.heap.getArrayIndex(lincoln));
        this.heap.reHeapify(lincoln);
        assertEquals(1, this.heap.getArrayIndex(lincoln));
        assertEquals(2, this.heap.getArrayIndex(ford));
        assertEquals(5, this.heap.getHeapCount());



        //reheapify for delete
        assertEquals(lincoln, this.heap.removeMin());
        assertEquals(1, this.heap.getArrayIndex(ford));
        assertEquals(4, this.heap.getHeapCount());

    }

    @Test (expected = NoSuchElementException.class)
    public void getArrayIndex() {
        this.heap.getArrayIndex(new Car("GMC Yukon", 2006));
    }

    @Test
    public void doubleArraySize() {
        Car solara = new Car("Solara", 2004);
        this.heap.insert(new Car("yukon", 2003));
        this.heap.insert(solara);
        assertEquals(7, this.heap.getHeapCount());
        assertEquals(7, this.heap.getArrayIndex(solara));
        assertEquals(10, this.heap.getArraySize());
        assertEquals(7, this.heap.getMap().get(solara).intValue());

        HashMap<Car, Integer> hm = this.heap.getMap();
        for (Car c : hm.keySet()) {
            System.out.printf("%-12s%-10d%s%d%n", c.model, c.year, "index:", hm.get(c));
        }
        solara.setYear(1950);
        System.out.println("\nnew\n");
        this.heap.reHeapify(solara);
        for (Car c : hm.keySet()) {
            System.out.printf("%-12s%-10d%s%d%n", c.model, c.year, "index:", hm.get(c));
        }
        assertTrue(hm.containsValue(7));
        assertEquals(1, this.heap.getArrayIndex(solara));
    }

    protected static class Car implements Comparable<Car> {
        protected String model;
        protected int year;
        protected int productNumber;

        protected Car (String model, int year) {
            this.model = model;
            this.year = year;
            this.productNumber = productNumberCount++;
        }

        protected void setYear(int year) {
            this.year = year;
        }

        @Override
        public int compareTo(Car car) {
            return Integer.compare(this.year, car.year);
        }

        @Override
        public boolean equals(Object obj) {
            Car c = (Car) obj;
            if (c == this) return true;
            if (c.year != this.year) {
                return false;
            }
            if (this.productNumber != c.productNumber) return false;
            return this.model.equals(c.model);
        }


        @Override
        public int hashCode() {
            return this.productNumber;
        }
    }
}