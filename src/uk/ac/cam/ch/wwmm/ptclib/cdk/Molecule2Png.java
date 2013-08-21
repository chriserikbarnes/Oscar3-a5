/*
 * Molecule2Png - should be trivially convertible to JPEG etc.
 * NOT gif, due to strange legal pedantry over software patents...
 * 
 * Peter Corbett, 9/12/2005
 */

package uk.ac.cam.ch.wwmm.ptclib.cdk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.openscience.cdk.geometry.GeometryToolsInternalCoordinates;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.renderer.Renderer2D;
import org.openscience.cdk.renderer.Renderer2DModel;

/** Produces png images for CDK molecules. Configure this by setting the
 * public variables.
 * 
 * @author ptc24
 *
 */
public class Molecule2Png {
	
	public Color backgroundColour = Color.WHITE;
	public String fontName = "MonoSpaced";
	public int fontStyle = Font.BOLD;
	public int fontSize = 16;
	public boolean colourAtoms = true;
	/* Gif apparently doesn't work, for strange legal reasons. */
	private String format = "png";
	
	public boolean fixedWidthAndHeight = false;
	/* If fixedWidthAndHeight... */
	public int width = 500;
	public int height = 500;
	public double occupationFactor = 0.8; /* 1.0 = no border */
	/* else ... */
	public double scaleFactor = 20.0;
	public int borderWidth = 20; /* Pixels. This is *after* a sensible margin for lettering */
	/* ...endif */
	
	/**Set up a new Molecule2Png with default options.
	 * 
	 */
	public Molecule2Png() {
		
	}
	
	/**Draw a molecule, writing the results to disk.
	 * 
	 * @param mol The molecule.
	 * @param filename The output file.
	 * @throws Exception
	 */
	public void renderMolecule(IMolecule mol, String filename) throws Exception {
		renderMolecule(mol, new FileOutputStream(new File(filename)));
	}

	/**Draw a molecule, generating co-ordinates from scratch.
	 * 
	 * @param mol The molecule.
	 * @param out An output stream.
	 * @throws Exception
	 */
	public void renderMolecule(IMolecule mol, OutputStream out) throws Exception {
		renderMolecule(mol, out, true);
	}

	/**Draw a molecule.
	 * 
	 * @param mol The molecule.
	 * @param out An output stream.
	 * @param genCoords Whether or not to generate co-ordinates (if false,
	 * co-ordinates must already have been generated.
	 * @throws Exception
	 */
	public void renderMolecule(IMolecule mol, OutputStream out, boolean genCoords) throws Exception {
		Renderer2DModel r2dm = new Renderer2DModel();
		Renderer2D r2d = new Renderer2D(r2dm);
		try {
			if(mol != null) {
				if(genCoords) {
					//System.out.println("Making co-ordinates");
					mol = MultiFragmentStructureDiagramGenerator.getMoleculeWith2DCoords(mol);
				}
				GeometryToolsInternalCoordinates.translateAllPositive(mol);
				if(fixedWidthAndHeight) {
					r2dm.setBackgroundDimension(new Dimension(width, height));
					GeometryToolsInternalCoordinates.scaleMolecule(mol, r2dm.getBackgroundDimension(), occupationFactor);        	
				} else {
					double [] cvals = GeometryToolsInternalCoordinates.getMinMax(mol);
					width = (int) Math.round(((cvals[2] - cvals[0]) * scaleFactor) + (fontSize*3)/2 + 3 + borderWidth * 2);
					height = (int) Math.round(((cvals[3] - cvals[1]) * scaleFactor) + fontSize/2 + 1 + borderWidth * 2);
					GeometryToolsInternalCoordinates.scaleMolecule(mol, scaleFactor);
					r2dm.setBackgroundDimension(new Dimension(width, height));
				}
				GeometryToolsInternalCoordinates.center(mol, r2dm.getBackgroundDimension());
				
				if(mol == null) throw new Exception();
			}
		} catch (Exception e) {
			mol = null;
		}
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics g = img.getGraphics();
		g.setColor(backgroundColour);		
		g.fillRect(0, 0, width, height);
		r2dm.setBackColor(backgroundColour);
		r2dm.setFont(new Font(fontName, fontStyle, fontSize));
		r2dm.setColorAtomsByType(colourAtoms);
		if(mol != null) r2d.paintMolecule(mol, img.createGraphics(), true, true);
		ImageIO.write(img, format, out);		
	}
}
