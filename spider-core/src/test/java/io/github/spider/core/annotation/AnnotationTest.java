package io.github.spider.core.annotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that all Spider annotations are properly defined and accessible.
 */
class AnnotationTest {

    @Test
    void testSpiderClientAnnotation() {
        assertNotNull(SpiderClient.class);
    }

    @Test
    void testSpiderGetAnnotation() {
        assertNotNull(SpiderGet.class);
    }

    @Test
    void testSpiderPostAnnotation() {
        assertNotNull(SpiderPost.class);
    }

    @Test
    void testPathAnnotation() {
        assertNotNull(Path.class);
    }

    @Test
    void testQueryAnnotation() {
        assertNotNull(Query.class);
    }

    @Test
    void testBodyAnnotation() {
        assertNotNull(Body.class);
    }

    @Test
    void testHeaderAnnotation() {
        assertNotNull(Header.class);
    }

    @Test
    void testTimeoutAnnotation() {
        assertNotNull(Timeout.class);
    }

    @Test
    void testRetryAnnotation() {
        assertNotNull(Retry.class);
    }
}
