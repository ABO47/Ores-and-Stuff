package com.abo47.oresandstuff.tools;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SvgIconRasterizer {
    private SvgIconRasterizer() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: SvgIconRasterizer <size> <outputDir> <svg...>");
        }

        int size = Integer.parseInt(args[0]);
        Path outputDir = Path.of(args[1]);
        Files.createDirectories(outputDir);

        for (int i = 2; i < args.length; i++) {
            Path svgPath = Path.of(args[i]);
            Path pngPath = outputDir.resolve(toPngName(svgPath));
            rasterize(svgPath, pngPath, size);
        }
    }

    private static void rasterize(Path svgPath, Path pngPath, int size) throws Exception {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
        transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);

        TranscoderInput input = new TranscoderInput(svgPath.toUri().toString());
        try (OutputStream output = Files.newOutputStream(pngPath)) {
            transcoder.transcode(input, new TranscoderOutput(output));
        }
    }

    private static String toPngName(Path svgPath) {
        String fileName = svgPath.getFileName().toString();
        int extensionStart = fileName.toLowerCase().lastIndexOf(".svg");
        if (extensionStart < 0) {
            return fileName + ".png";
        }
        return fileName.substring(0, extensionStart) + ".png";
    }
}
