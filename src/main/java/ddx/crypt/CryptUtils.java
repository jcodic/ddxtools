package ddx.crypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 *
 * @author druidman
 */
public class CryptUtils {

    public static byte[] compress(byte[] data, int level) throws IOException {
	
	Deflater deflater = new Deflater();
	deflater.setInput(data);
        deflater.setLevel(level);
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
	deflater.finish();
	byte[] buffer = new byte[1024];
	while (!deflater.finished()) {
	    int count = deflater.deflate(buffer);
	    outputStream.write(buffer, 0, count);
	}
	outputStream.close();
	byte[] output = outputStream.toByteArray();
	return output;
    }

    public static byte[] decompress(byte[] data) throws IOException, DataFormatException {
	
	Inflater inflater = new Inflater();
	inflater.setInput(data);
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
	byte[] buffer = new byte[1024];
	while (!inflater.finished()) {
	    int count = inflater.inflate(buffer);
	    outputStream.write(buffer, 0, count);
	}
	outputStream.close();
	byte[] output = outputStream.toByteArray();
	return output;
    }
}
