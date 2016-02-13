package reproducer;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.exif.EXIFReader;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter;


/**
 * Write the file color.tiff several times to the a dina4 page to test if the color/brigthness
 * changes which is the case. Both tiff images use the icc profile fogra 39. The issue is that the
 * color gets darker after writing it to the dina4 page. Furthermore this class contains two
 * sub-issues.
 * 
 * @author Mathias
 *
 */
public class TestTiffWriteRepetition {

  public static void main(String[] args) throws Exception {
    ImageIO.scanForPlugins();

    // Sub-issue 1: The tmpFile "_tmp1.tiff" is persisted on disk although setUserCache = false
    ImageIO.setUseCache(false);

    TestTiffWriteRepetition t = new TestTiffWriteRepetition();
    t.run();
    System.out.println("Finished!");
  }

  private void run() throws Exception {
    File page = loadFile("dina4.tiff");
    File colorTiff = loadFile("color.tiff");
    // Sub-issue 2: value of i > 1 produces an ArrayIndexOutOfBoundsException
    for (int i = 0; i < 1; i++) {
      // at java.awt.image.DataBufferByte.getElem(DataBufferByte.java:256)
      // at
      // com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter.writeImageData(TIFFImageWriter.java:639)
      // at com.twelvemonkeys.imageio.plugins.tiff.TIFFImageWriter.write(TIFFImageWriter.java:380)
      // at reproducer.TestTiffWriteRepetition.testWriteTifftoTiff(TestTiffWriteRepetition.java:95)
      // at reproducer.TestTiffWriteRepetition.run(TestTiffWriteRepetition.java:51)
      // at reproducer.TestTiffWriteRepetition.main(TestTiffWriteRepetition.java:40)
      File tmpFile = new File("_tmp" + i + ".tiff");
      testWriteTifftoTiff(page, colorTiff, tmpFile);
      page = tmpFile;
    }


    // Optional: Copy the file to some destination
    // File output = new File("issue-02-output.tiff");
    // Files.copy(page.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private void testWriteTifftoTiff(File inFile1, File inFile2, File outFile) throws Exception {
    BufferedImage bufferImage = ImageIO.read(inFile1);
    Graphics2D g = bufferImage.createGraphics();
    g.setComposite(AlphaComposite.Src);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
        RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

    // Add tiff to tiff
    BufferedImage itemImg = ImageIO.read(inFile2);
    int xLeft = 100;
    int xTop = 50;

    g.drawImage(itemImg, xLeft, xTop, null);
    g.dispose();

    // Write tiff
    String format = "tiff";
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
    TIFFImageWriter writer = (TIFFImageWriter) writers.next();
    try {
      ImageOutputStream output = ImageIO.createImageOutputStream(outFile);

      try {
        writer.setOutput(output);
        ImageWriteParam param = writer.getDefaultWriteParam();
        ImageInputStream input = ImageIO.createImageInputStream(inFile1);
        Directory ifd = new EXIFReader().read(input);
        TIFFImageMetadata metadata = new TIFFImageMetadata(ifd);
        IIOImage iioimage = new IIOImage(bufferImage, null, metadata);
        writer.write(metadata, iioimage, param);
      } finally {
        output.close();
      }
    } finally {
      writer.dispose();
    }
  }

  private File loadFile(String name) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(name).toURI());
  }
}
