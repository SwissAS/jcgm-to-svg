package com.jpprade.jcgmtosvg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JcgmToSvgTest {

    @Test
    void testConvertWithInvalidInputStream() {
        InputStream invalidStream = new ByteArrayInputStream(new byte[]{});
        OutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(JcgmToSvgException.class, () -> JcgmToSvg.convert(invalidStream, outputStream));
    }

    @Test
    void testConvertFile(@TempDir Path tempDir) throws IOException {
        File inputFile = new File(tempDir.toFile(), "test.cgm");
        File outputDir = tempDir.toFile();
        
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            fos.write(new byte[]{0x00, 0x01, 0x02});
        }
        
        assertThrows(JcgmToSvgException.class, () -> JcgmToSvg.convert(inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));
    }
}
