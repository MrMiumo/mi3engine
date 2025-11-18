package io.github.mrmiumo.mi3engine;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class VecTests {

    @Test
    public void testM90M90() {
        assertValid(new Vec(-90, -90, -90), new Vec(0, -90, 0));
        assertValid(new Vec(-90, -90, -25), new Vec(-90, -25, 90));
        assertValid(new Vec(-90, -90, 0),   new Vec(-90, 0, 90));
        assertValid(new Vec(-90, -90, 25),  new Vec(-90, 25, 90));
        assertValid(new Vec(-90, -90, 90),  new Vec(0, 90, 180));
    }

    @Test
    public void testM90M25() {
        assertValid(new Vec(-90, -25, -90), new Vec(0, -90, -65));
        assertValid(new Vec(-90, -25, -25), new Vec(-90, -25, 25));
        assertValid(new Vec(-90, -25, 0),   new Vec(-90, 0, 25));
        assertValid(new Vec(-90, -25, 25),  new Vec(-90, 25, 25));
        assertValid(new Vec(-90, -25, 90),  new Vec(0, 90, 115));
    }
    
    @Test
    public void testM90P0() {
        assertValid(new Vec(-90, 0, -90),   new Vec(0, -90, -90));
        assertValid(new Vec(-90, 0, -25),   new Vec(-90, -25, 0));
        assertValid(new Vec(-90, 0, 0),     new Vec(-90, 0, 0));
        assertValid(new Vec(-90, 0, 25),    new Vec(-90, 25, 0));
        assertValid(new Vec(-90, 0, 90),    new Vec(0, 90, 90));
    }

    @Test
    public void testM90P25() {
        assertValid(new Vec(-90, 25, -90),  new Vec(0, -90, -115));
        assertValid(new Vec(-90, 25, -25),  new Vec(-90, -25, -25));
        assertValid(new Vec(-90, 25, 0),    new Vec(-90, 0, -25));
        assertValid(new Vec(-90, 25, 25),   new Vec(-90, 25, -25));
        assertValid(new Vec(-90, 25, 90),   new Vec(0, 90, 65));
    }

    @Test
    public void testM90P90() {
        assertValid(new Vec(-90, 90, -90),  new Vec(0, -90, -180));
        assertValid(new Vec(-90, 90, -25),  new Vec(-90, -25, -90));
        assertValid(new Vec(-90, 90, 0),    new Vec(-90, 0, -90));
        assertValid(new Vec(-90, 90, 25),   new Vec(-90, 25, -90));
        assertValid(new Vec(-90, 90, 90),   new Vec(0, 90, 0));
    }



    @Test
    public void testM25M90() {
        assertValid(new Vec(-25, -90, -90), new Vec(90, -25, -90));
        assertValid(new Vec(-25, -90, -25), new Vec(0, -90, 0));
        assertValid(new Vec(-25, -90, 0),   new Vec(-90, -65, 90));
        assertValid(new Vec(-25, -90, 25),  new Vec(-90, -40, 90));
        assertValid(new Vec(-25, -90, 90),  new Vec(-90, 25, 90));
    }

    @Test
    public void testM25M25() {
        assertValid(new Vec(-25, -25, -90), new Vec(25, -25, -90));
        assertValid(new Vec(-25, -25, -25), new Vec(-15.0688, -31.7182, -15.0688));
        assertValid(new Vec(-25, -25, 0),   new Vec(-27.2264, -22.521, 11.1484));
        assertValid(new Vec(-25, -25, 25),  new Vec(-33.5594, -9.7024, 33.5594));
        assertValid(new Vec(-25, -25, 90),  new Vec(-25, 25, 90));
    }

    @Test
    public void testM25P0() {
        assertValid(new Vec(-25, 0, -90),   new Vec(0, -25, -90));
        assertValid(new Vec(-25, 0, -25),   new Vec(-22.9098, -10.2886, -22.9098));
        assertValid(new Vec(-25, 0, 0),     new Vec(-25, 0, 0));
        assertValid(new Vec(-25, 0, 25),    new Vec(-22.9098, 10.2886, 22.9098));
        assertValid(new Vec(-25, 0, 90),    new Vec(0, 25, 90));
    }

    @Test
    public void testM25P25() {
        assertValid(new Vec(-25, 25, -90),  new Vec(-25, -25, -90));
        assertValid(new Vec(-25, 25, -25),  new Vec(-33.5594, 9.7024, -33.5594));
        assertValid(new Vec(-25, 25, 0),    new Vec(-27.2264, 22.521, -11.1484));
        assertValid(new Vec(-25, 25, 25),   new Vec(-15.0688, 31.7182, 15.0688));
        assertValid(new Vec(-25, 25, 90),   new Vec(25, 25, 90));
    }

    @Test
    public void testM25P90() {
        assertValid(new Vec(-25, 90, -90),  new Vec(-90, -25, -90));
        assertValid(new Vec(-25, 90, -25),  new Vec(-90, 40, -90));
        assertValid(new Vec(-25, 90, 0),    new Vec(-90, 65, -90));
        assertValid(new Vec(-25, 90, 25),   new Vec(0, 90, 0));
        assertValid(new Vec(-25, 90, 90),   new Vec(90, 25, 90));
    }



    @Test
    public void testP0M90() {
        assertValid(new Vec(0, -90, -90),   new Vec(90, 0, -90));
        assertValid(new Vec(0, -90, -25),   new Vec(90, -65, -90));
        assertValid(new Vec(0, -90, 0),     new Vec(0, -90, 0));
        assertValid(new Vec(0, -90, 25),    new Vec(-90, -65, 90));
        assertValid(new Vec(0, -90, 90),    new Vec(-90, 0, 90));
    }

    @Test
    public void testP0M25() {
        assertValid(new Vec(0, -25, -90),   new Vec(25, 0, -90));
        assertValid(new Vec(0, -25, -25),   new Vec(11.1484, -22.521, -27.2264));
        assertValid(new Vec(0, -25, 0),     new Vec(0, -25, 0));
        assertValid(new Vec(0, -25, 25),    new Vec(-11.1484, -22.521, 27.2264));
        assertValid(new Vec(0, -25, 90),    new Vec(-25, 0, 90));
    }

    @Test
    public void testP0P0() {
        assertValid(new Vec(0, 0, -90),     new Vec(0, 0, -90));
        assertValid(new Vec(0, 0, -25),     new Vec(0, 0, -25));
        assertValid(new Vec(0, 0, 0),       new Vec(0, 0, 0));
        assertValid(new Vec(0, 0, 25),      new Vec(0, 0, 25));
        assertValid(new Vec(0, 0, 90),      new Vec(0, 0, 90));
    }

    @Test
    public void testP0P25() {
        assertValid(new Vec(0, 25, -90),    new Vec(-25, 0, -90));
        assertValid(new Vec(0, 25, -25),    new Vec(-11.1484, 22.521, -27.2264));
        assertValid(new Vec(0, 25, 0),      new Vec(0, 25, 0));
        assertValid(new Vec(0, 25, 25),     new Vec(11.1484, 22.521, 27.2264));
        assertValid(new Vec(0, 25, 90),     new Vec(25, 0, 90));
    }

    @Test
    public void testP0P90() {
        assertValid(new Vec(0, 90, -90),    new Vec(-90, 0, -90));
        assertValid(new Vec(0, 90, -25),    new Vec(-90, 65, -90));
        assertValid(new Vec(0, 90, 0),      new Vec(0, 90, 0));
        assertValid(new Vec(0, 90, 25),     new Vec(90, 65, 90));
        assertValid(new Vec(0, 90, 90),     new Vec(90, 0, 90));
    }



    @Test
    public void testP25M90() {
        assertValid(new Vec(25, -90, -90),  new Vec(90, 25, -90));
        assertValid(new Vec(25, -90, -25),  new Vec(90, -40, -90));
        assertValid(new Vec(25, -90, 0),    new Vec(90, -65, -90));
        assertValid(new Vec(25, -90, 25),   new Vec(0, -90, 0));
        assertValid(new Vec(25, -90, 90),   new Vec(-90, -25, 90));
    }

    @Test
    public void testP25M25() {
        assertValid(new Vec(25, -25, -90),  new Vec(25, 25, -90));
        assertValid(new Vec(25, -25, -25),  new Vec(33.5594, -9.7024, -33.5594));
        assertValid(new Vec(25, -25, 0),    new Vec(27.2264, -22.521, -11.1484));
        assertValid(new Vec(25, -25, 25),   new Vec(15.0688, -31.7182, 15.0688));
        assertValid(new Vec(25, -25, 90),   new Vec(-25, -25, 90));
    }

    @Test
    public void testP25P0() {
        assertValid(new Vec(25, 0, -90),    new Vec(0, 25, -90));
        assertValid(new Vec(25, 0, -25),    new Vec(22.9098, 10.2886, -22.9098));
        assertValid(new Vec(25, 0, 0),      new Vec(25, 0, 0));
        assertValid(new Vec(25, 0, 25),     new Vec(22.9098, -10.2886, 22.9098));
        assertValid(new Vec(25, 0, 90),     new Vec(0, -25, 90));
    }

    @Test
    public void testP25P25() {
        assertValid(new Vec(25, 25, -90),   new Vec(-25, 25, -90));
        assertValid(new Vec(25, 25, -25),   new Vec(15.0688, 31.7182, -15.0688));
        assertValid(new Vec(25, 25, 0),     new Vec(27.2264, 22.521, 11.1484));
        assertValid(new Vec(25, 25, 25),    new Vec(33.5594, 9.7024, 33.5594));
        assertValid(new Vec(25, 25, 90),    new Vec(25, -25, 90));
    }

    @Test
    public void testP25P90() {
        assertValid(new Vec(25, 90, -90),   new Vec(-90, 25, -90));
        assertValid(new Vec(25, 90, -25),   new Vec(0, 90, 0));
        assertValid(new Vec(25, 90, 0),     new Vec(90, 65, 90));
        assertValid(new Vec(25, 90, 25),    new Vec(90, 40, 90));
        assertValid(new Vec(25, 90, 90),    new Vec(90, -25, 90));
    }



    @Test
    public void testP90M90() {
        assertValid(new Vec(90, -90, -90),  new Vec(0, 90, -180));
        assertValid(new Vec(90, -90, -25),  new Vec(90, 25, -90));
        assertValid(new Vec(90, -90, 0),    new Vec(90, 0, -90));
        assertValid(new Vec(90, -90, 25),   new Vec(90, -25, -90));
        assertValid(new Vec(90, -90, 90),   new Vec(0, -90, 0));
    }

    @Test
    public void testP90M25() {
        assertValid(new Vec(90, -25, -90),  new Vec(0, 90, -115));
        assertValid(new Vec(90, -25, -25),  new Vec(90, 25, -25));
        assertValid(new Vec(90, -25, 0),    new Vec(90, 0, -25));
        assertValid(new Vec(90, -25, 25),   new Vec(90, -25, -25));
        assertValid(new Vec(90, -25, 90),   new Vec(0, -90, 65));
    }

    @Test
    public void testP90P0() {
        assertValid(new Vec(90, 0, -90),    new Vec(0, 90, -90));
        assertValid(new Vec(90, 0, -25),    new Vec(90, 25, 0));
        assertValid(new Vec(90, 0, 0),      new Vec(90, 0, 0));
        assertValid(new Vec(90, 0, 25),     new Vec(90, -25, 0));
        assertValid(new Vec(90, 0, 90),     new Vec(0, -90, 90));
    }

    @Test
    public void testP90P25() {
        assertValid(new Vec(90, 25, -90),   new Vec(0, 90, -65));
        assertValid(new Vec(90, 25, -25),   new Vec(90, 25, 25));
        assertValid(new Vec(90, 25, 0),     new Vec(90, 0, 25));
        assertValid(new Vec(90, 25, 25),    new Vec(90, -25, 25));
        assertValid(new Vec(90, 25, 90),    new Vec(0, -90, 115));
    }

    @Test
    public void testP90P90() {
        assertValid(new Vec(90, 90, -90),   new Vec(0, 90, 0));
        assertValid(new Vec(90, 90, -25),   new Vec(90, 25, 90));
        assertValid(new Vec(90, 90, 0),     new Vec(90, 0, 90));
        assertValid(new Vec(90, 90, 25),    new Vec(90, -25, 90));
        assertValid(new Vec(90, 90, 90),    new Vec(0, -90, 180));
    }

    private static void assertValid(Vec src, Vec expected) {
        var actual = src.localToGlobal();
        /* Clip to ]-180;180] and truncate to 4 digits precision */
        actual = new Vec(Math.round(actual.x() * 10000) / 10000.0, Math.round(actual.y() * 10000) / 10000.0, Math.round(actual.z() * 10000) / 10000.0);
        var correct = new Vec((actual.x() + 180) % 360 - 180, (actual.y() + 180) % 360 - 180, (actual.z() + 180) % 360 - 180);
        if (actual.equals(expected) || correct.equals(expected)) return;
        fail("Local: " + src + ", Expected Global:" + expected + ", Actual Global:" + actual);
    }
}
