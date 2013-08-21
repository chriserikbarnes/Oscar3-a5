package uk.ac.cam.ch.wwmm.ptclib.cdk;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

/**Does geometric reflection of atom coordinates in 2D about a given line.
 * 
 * @author ptc24
 *
 */
final class Reflection {

	/** Gives co-ordinates of an atom, reflected along a line described by two
	 * other atoms.
	 * 
	 * @param atomCoords The atom whose co-ordinates are to be reflected
	 * @param start The first atom in the line
	 * @param end The other atom in the line
	 * @return The reflected co-ordinates
	 */
	public static Point2d reflect(Point2d atomCoords, Point2d start, Point2d end) {
		// New point, representing vector from start atom to end atom
		Vector2d lineVector = new Vector2d(end.x - start.x, end.y - start.y);
		
		// Rotate by 90 degrees to get vector orthogonal to line
		Vector2d aVector = new Vector2d(-lineVector.y, lineVector.x);
		
		Vector2d newAtomVector = new Vector2d(atomCoords);
		
		// Translate so line goes through origin
		newAtomVector.sub(start);
		
		// Magic formula
		Vector2d refVect = new Vector2d(aVector);
		refVect.scale(2*aVector.dot(newAtomVector)/aVector.dot(aVector));
		newAtomVector.sub(refVect);
		
		// Translate back
		newAtomVector.add(start);
		
		return new Point2d(newAtomVector);
	}

}
