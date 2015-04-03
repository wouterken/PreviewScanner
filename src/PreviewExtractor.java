
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.xml.bind.DatatypeConverter;


public class PreviewExtractor {

	public static BufferedImage extractEmbeddedPreview(File file, String fileExt){
		
		try{
			final FileImageInputStream input = new FileImageInputStream(file);
			
			if(fileExt.toLowerCase().equals(".psd")){
				return findEmbeddedThumbnailPSD(input);
			}
			else if(fileExt.toLowerCase().equals(".ai")){
				return findEmbeddedThumbnailAI(input);
			}
			else if(fileExt.toLowerCase().equals(".eps")){
				return findEmbeddedThumbnailAI(input);
			}
			else if(fileExt.toLowerCase().equals(".pdf")){
				return findEmbeddedThumbnailAI(input);
			}
			else if(fileExt.toLowerCase().equals(".tif")){
				return findEmbeddedThumbnailTiff(input);
			}
			else if(fileExt.toLowerCase().equals(".tiff")){
				return findEmbeddedThumbnailTiff(input);
			}
		}catch (Exception e) {
			//Couldn't find preview!.
		}
		return null;
	}
	
	private static BufferedImage findEmbeddedThumbnailTiff(FileImageInputStream input) throws IOException {
		final Byte [] tiffHeader = new Byte[]{0x38,0x42,0x49,0x4D};
		do{
			final int pos = scanTo(input, tiffHeader);
			if(pos == -1){
				break;
			}
			
			while(input.getStreamPosition() < input.length()){
				int blockType = input.readUnsignedShort();
				int blockNameLength = input.readUnsignedByte();
				
				if(blockNameLength == 0){
					input.seek(input.getStreamPosition() + 1);
				}else{
					input.seek(input.getStreamPosition() + blockNameLength);
				}
				
				long blockLength = input.readUnsignedInt();
				blockLength += (blockLength % 2 == 1)?1:0;
				
				if(blockType == 0x40C){
					input.seek(input.getStreamPosition() + 4);
					input.readInt();//Width
					input.readInt();//Height
					final byte[] imagebytes = new byte[(int) (blockLength - 28)];
					input.seek(input.getStreamPosition() + 16);
					input.read(imagebytes);
					BufferedImage img = ImageIO.read(new ByteArrayInputStream(imagebytes));
					return img;
				}else{
					input.seek(input.getStreamPosition() + blockLength + 4);
				}
			}
				
		}while(false);
		return null;
	}

	private static BufferedImage findEmbeddedThumbnailPSD(FileImageInputStream input) throws IOException {
		final int firstBlock = 34;
		
		input.seek(firstBlock);
		
		while(input.getStreamPosition() < input.length()){
			input.readInt(); //8BIM.
			int blockType = input.readUnsignedShort();
			int blockNameLength = input.readUnsignedByte();
			
			if(blockNameLength == 0){
				input.seek(input.getStreamPosition() + 1);
			}else{
				input.seek(input.getStreamPosition() + blockNameLength);
			}
			
			long blockLength = input.readUnsignedInt();
			blockLength += (blockLength % 2 == 1)?1:0;
			
			if(blockType == 0x40C){
				input.seek(input.getStreamPosition() + 4);
				input.readInt();//Width
				input.readInt();//Height
				final byte[] imagebytes = new byte[(int) (blockLength - 28)];
				input.seek(input.getStreamPosition() + 16);
				input.read(imagebytes);
				BufferedImage img = ImageIO.read(new ByteArrayInputStream(imagebytes));

				return img;
			}else{
				input.seek(input.getStreamPosition() + blockLength);
			}
		}
		
		return null;
	}

	private static BufferedImage findEmbeddedThumbnailAI(FileImageInputStream input) throws IOException {
		final Byte[] xmpHeader = new Byte[]{0x3C,0x3F,0x78,0x70,0x61,0x63,0x6B,0x65,0x74,0x20,0x62,0x65,0x67,0x69,0x6E,0x3D};
		final Byte[] endImage = new Byte[]{0x3C,0x2F,0x78,0x6D,0x70,0x47,0x49,0x6D,0x67,0x3A,0x69,0x6D,0x61,0x67,0x65,0x3E};
		final Byte[] startImage = new Byte[]{0x3C,0x78,0x6D,0x70,0x47,0x49,0x6D,0x67,0x3A,0x69,0x6D,0x61,0x67,0x65,0x3E};
		
		do{
			if(scanTo(input, xmpHeader) == -1){
				break;
			}
			if(scanTo(input, startImage)  == -1){
				break;
			}
			
			input.mark();
			int length = scanTo(input, endImage);
			
			if(length == -1){
				break;
			}
			
			length -= endImage.length;
			
			final byte[] imageBytes = new byte[length];
			input.reset();
			
			input.read(imageBytes);
			
			
			final byte[] decodedImageBytes = DatatypeConverter.parseBase64Binary(new String(imageBytes).replace("&#xA;", ""));
			

			BufferedImage img = ImageIO.read(new ByteArrayInputStream(decodedImageBytes));
			
			return img;
		}while(false);
		
		return null;
	}

	public static final int MAX_SCAN_AHEAD = 100000;

	/**
	 * This can be replaced with a buffered Boyer-moore string search if preview generation becomes too inefficient.
	 * @param input
	 * @param bytes
	 * @return
	 */
	public static int scanTo(FileImageInputStream input, Byte[] bytes){
		final ArrayList<Byte> buffer = new ArrayList<Byte>();
		int scanAhead = bytes.length;
		try {
			while(buffer.size() < bytes.length){
				buffer.add(input.readByte());
			}
			while(scanAhead < MAX_SCAN_AHEAD && ! Arrays.equals(buffer.toArray(new Byte[]{}),bytes)){

				buffer.add(input.readByte());
				buffer.remove(0);
				scanAhead += 1;
			}
			if(Arrays.equals(buffer.toArray(new Byte[]{}),bytes)){
				return scanAhead;
			}
			
		} catch (IOException e) {
			System.out.println(e);
		}
		return -1;
	}

}
